package ec.edu.puce.aruy.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.remote.util.ApiErrorHandler
import ec.edu.puce.aruy.data.remote.util.MensajesPrestador
import ec.edu.puce.aruy.data.repository.PrestadorRepository
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.catalogo.SugerenciaCategoria
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.model.Servicio
import ec.edu.puce.aruy.util.ConversorWebp
import ec.edu.puce.aruy.util.ErrorFoto
import ec.edu.puce.aruy.util.EtiquetadorFotos
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ServicioPropio(val servicio: Servicio, val fotos: List<FotoPortafolio>)

data class MisServiciosUi(
    val cargando: Boolean = false,
    val cargado: Boolean = false,
    val error: String? = null,
    val servicios: List<ServicioPropio> = emptyList(),
    /** `true` = "Recibiendo solicitudes" (perfiles_prestadores.disponible). */
    val recibeSolicitudes: Boolean = true,
    val subiendoFoto: Boolean = false
) {
    val totalFotos: Int get() = servicios.sumOf { it.fotos.size }
}

/**
 * Panel, Mis servicios y Portafolio del prestador comparten estos datos. Se reinicia cuando cambia
 * la cuenta, para que un prestador nunca vea los servicios de otro.
 */
class MisServiciosViewModel(
    private val repositorio: PrestadorRepository,
    private val sesion: SesionManager,
    private val conversor: ConversorWebp,
    private val etiquetador: EtiquetadorFotos
) : ViewModel() {

    private val _ui = MutableStateFlow(MisServiciosUi())
    val ui: StateFlow<MisServiciosUi> = _ui.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    init {
        viewModelScope.launch {
            sesion.estado.map { (it as? EstadoSesion.Autenticada)?.sesion?.usuario?.id }
                .distinctUntilChanged()
                .collect { _ui.value = MisServiciosUi() }
        }
    }

    private fun perfilId(): String? =
        (sesion.estado.value as? EstadoSesion.Autenticada)?.sesion?.perfilPrestadorId

    fun cargarSiHaceFalta() {
        if (!_ui.value.cargado && !_ui.value.cargando) cargar()
    }

    fun cargar() {
        val perfil = perfilId()
        if (perfil == null) {
            _ui.update { it.copy(cargado = true, error = "Todavía no tienes un perfil de prestador.") }
            return
        }
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                coroutineScope {
                    val disponible = async { runCatching { repositorio.disponibilidad() }.getOrNull() }
                    val servicios = repositorio.misServicios(perfil)
                    val conFotos = servicios
                        .map { s -> async { ServicioPropio(s, runCatching { repositorio.fotosDe(s.id) }.getOrDefault(emptyList())) } }
                        .map { it.await() }
                    _ui.update {
                        it.copy(
                            cargando = false,
                            cargado = true,
                            servicios = conFotos,
                            recibeSolicitudes = disponible.await() ?: it.recibeSolicitudes
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, cargado = true, error = ApiErrorHandler.obtenerMensaje(e)) }
            }
        }
    }

    fun cambiarRecepcion(recibe: Boolean) {
        val antes = _ui.value.recibeSolicitudes
        _ui.update { it.copy(recibeSolicitudes = recibe) }
        viewModelScope.launch {
            try {
                _ui.update { it.copy(recibeSolicitudes = repositorio.cambiarDisponibilidad(recibe)) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(recibeSolicitudes = antes) }
                _mensajes.tryEmit(ApiErrorHandler.obtenerMensaje(e))
            }
        }
    }

    fun pausarOReanudar(servicio: Servicio) {
        viewModelScope.launch {
            try {
                val nuevo = repositorio.cambiarDisponibilidadDelServicio(servicio.id, !servicio.disponible)
                reemplazar(nuevo)
                _mensajes.tryEmit(if (nuevo.disponible) "El servicio vuelve a recibir solicitudes." else "El servicio quedó pausado.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensajes.tryEmit(MensajesPrestador.alGuardarServicio(e))
            }
        }
    }

    fun eliminarServicio(id: String) {
        viewModelScope.launch {
            try {
                repositorio.eliminar(id)
                _ui.update { estado -> estado.copy(servicios = estado.servicios.filterNot { it.servicio.id == id }) }
                _mensajes.tryEmit("Servicio eliminado.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensajes.tryEmit(MensajesPrestador.alGuardarServicio(e))
            }
        }
    }

    fun eliminarFoto(servicioId: String, fotoId: String) {
        viewModelScope.launch {
            try {
                repositorio.eliminarFoto(fotoId)
                _ui.update { estado ->
                    estado.copy(servicios = estado.servicios.map {
                        if (it.servicio.id == servicioId) it.copy(fotos = it.fotos.filterNot { f -> f.id == fotoId }) else it
                    })
                }
                _mensajes.tryEmit("Quitaste la foto.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensajes.tryEmit(MensajesPrestador.alGuardarServicio(e))
            }
        }
    }

    /** Convierte la foto a WebP, la analiza con ML Kit y la sube al servicio elegido (desde Portafolio). */
    fun subirFoto(servicioId: String, uri: Uri, desdeCamara: Boolean) {
        val actual = _ui.value.servicios.firstOrNull { it.servicio.id == servicioId }
        if (actual != null && actual.fotos.size >= PrestadorRepository.MAX_FOTOS_POR_SERVICIO) {
            _mensajes.tryEmit("Este servicio ya tiene ${PrestadorRepository.MAX_FOTOS_POR_SERVICIO} fotos, el máximo permitido.")
            return
        }
        _ui.update { it.copy(subiendoFoto = true) }
        viewModelScope.launch {
            try {
                val imagen = conversor.convertir(uri)
                val etiquetas = etiquetador.etiquetar(imagen.paraEtiquetar)
                val foto = repositorio.subirFoto(servicioId, imagen.webp, etiquetas.map { it.texto })
                _ui.update { estado ->
                    estado.copy(
                        subiendoFoto = false,
                        servicios = estado.servicios.map {
                            if (it.servicio.id == servicioId) it.copy(fotos = it.fotos + foto) else it
                        }
                    )
                }
                val categoria = SugerenciaCategoria.sugerir(etiquetas)
                _mensajes.tryEmit(
                    if (categoria != null) "Foto agregada a tu portafolio (${categoria.etiqueta})." else "Foto agregada a tu portafolio."
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorFoto) {
                _ui.update { it.copy(subiendoFoto = false) }
                _mensajes.tryEmit(e.message.orEmpty())
            } catch (e: Exception) {
                _ui.update { it.copy(subiendoFoto = false) }
                _mensajes.tryEmit(MensajesPrestador.alSubirFoto(e))
            } finally {
                if (desdeCamara) conversor.borrarTemporal(uri)
            }
        }
    }

    /** Llamado por el editor al crear, editar o borrar un servicio, para que las listas se actualicen. */
    fun invalidar() {
        _ui.update { it.copy(cargado = false) }
    }

    private fun reemplazar(servicio: Servicio) {
        _ui.update { estado ->
            estado.copy(servicios = estado.servicios.map {
                if (it.servicio.id == servicio.id) it.copy(servicio = servicio) else it
            })
        }
    }
}
