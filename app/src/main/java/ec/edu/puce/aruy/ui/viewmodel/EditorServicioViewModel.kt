package ec.edu.puce.aruy.ui.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.local.BorradorServicio
import ec.edu.puce.aruy.data.local.BorradorStore
import ec.edu.puce.aruy.data.remote.util.MensajesPrestador
import ec.edu.puce.aruy.data.repository.DatosServicio
import ec.edu.puce.aruy.data.repository.PrestadorRepository
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.catalogo.EtiquetaFoto
import ec.edu.puce.aruy.domain.catalogo.SugerenciaCategoria
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.validation.ValidacionesServicio
import ec.edu.puce.aruy.util.ConversorWebp
import ec.edu.puce.aruy.util.ErrorFoto
import ec.edu.puce.aruy.util.EtiquetadorFotos
import ec.edu.puce.aruy.util.ImagenPreparada
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface EstadoSubida {
    data object Pendiente : EstadoSubida
    data object Subiendo : EstadoSubida
    data class Fallo(val mensaje: String) : EstadoSubida
}

/** Foto elegida que todavía no está en el servidor: ya viene convertida a WebP. */
data class FotoPendiente(
    val id: Long,
    val imagen: ImagenPreparada,
    val etiquetas: List<EtiquetaFoto>,
    val estado: EstadoSubida = EstadoSubida.Pendiente
)

data class EditorUi(
    val cargando: Boolean = false,
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: CategoriaServicio? = null,
    val precio: String = "",
    val disponible: Boolean = true,
    val errorTitulo: String? = null,
    val errorDescripcion: String? = null,
    val errorCategoria: String? = null,
    val errorPrecio: String? = null,
    val fotosGuardadas: List<FotoPortafolio> = emptyList(),
    val pendientes: List<FotoPendiente> = emptyList(),
    val etiquetasUltimaFoto: List<EtiquetaFoto> = emptyList(),
    val categoriaSugerida: CategoriaServicio? = null,
    val analizandoFoto: Boolean = false,
    val guardando: Boolean = false,
    val progreso: String? = null,
    /** Id del servicio ya creado en el servidor; así un reintento no lo duplica. */
    val servicioId: String? = null,
    val finalizado: Boolean = false
) {
    val esEdicion: Boolean get() = servicioId != null
    val totalFotos: Int get() = fotosGuardadas.size + pendientes.size
    val puedeAgregarFoto: Boolean
        get() = totalFotos < PrestadorRepository.MAX_FOTOS_POR_SERVICIO && !analizandoFoto && !guardando
}

/**
 * Formulario de un servicio, nuevo o existente. Las fotos se convierten a WebP al elegirlas y se suben
 * después de guardar el servicio, una por una (el backend no recibe fotos en `POST /servicios`).
 */
class EditorServicioViewModel(
    servicioId: String?,
    private val repositorio: PrestadorRepository,
    private val sesion: SesionManager,
    private val conversor: ConversorWebp,
    private val etiquetador: EtiquetadorFotos,
    private val borradores: BorradorStore
) : ViewModel() {

    private val _ui = MutableStateFlow(EditorUi(cargando = servicioId != null, servicioId = servicioId))
    val ui: StateFlow<EditorUi> = _ui.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    private var siguienteIdFoto = 0L
    private var guardadoDelBorrador: Job? = null

    private val usuarioId: String?
        get() = (sesion.estado.value as? EstadoSesion.Autenticada)?.sesion?.usuario?.id

    init {
        if (servicioId != null) cargarExistente(servicioId) else recuperarBorrador()
    }

    private fun cargarExistente(id: String) {
        viewModelScope.launch {
            try {
                val servicio = repositorio.servicio(id)
                val fotos = runCatching { repositorio.fotosDe(id) }.getOrDefault(emptyList())
                _ui.update {
                    it.copy(
                        cargando = false,
                        titulo = servicio.titulo,
                        descripcion = servicio.descripcion,
                        categoria = servicio.categoria,
                        precio = servicio.precioReferencial?.let(::textoDePrecio).orEmpty(),
                        disponible = servicio.disponible,
                        fotosGuardadas = fotos
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false) }
                _mensajes.tryEmit(MensajesPrestador.alGuardarServicio(e))
            }
        }
    }

    private fun recuperarBorrador() {
        val id = usuarioId ?: return
        viewModelScope.launch {
            val borrador = runCatching { borradores.leer(id) }.getOrNull() ?: return@launch
            _ui.update {
                it.copy(
                    titulo = borrador.titulo,
                    descripcion = borrador.descripcion,
                    categoria = borrador.categoria?.let { c -> runCatching { CategoriaServicio.valueOf(c) }.getOrNull() },
                    precio = borrador.precio,
                    disponible = borrador.disponible
                )
            }
            _mensajes.tryEmit("Recuperamos tu borrador.")
        }
    }

    fun cambiarTitulo(valor: String) = cambiar { it.copy(titulo = valor.take(ValidacionesServicio.TITULO_MAX), errorTitulo = null) }
    fun cambiarDescripcion(valor: String) = cambiar { it.copy(descripcion = valor.take(ValidacionesServicio.DESCRIPCION_MAX), errorDescripcion = null) }
    fun cambiarCategoria(valor: CategoriaServicio) = cambiar { it.copy(categoria = valor, errorCategoria = null) }
    fun cambiarPrecio(valor: String) = cambiar { it.copy(precio = valor.take(7), errorPrecio = null) }
    fun cambiarDisponible(valor: Boolean) = cambiar { it.copy(disponible = valor) }

    fun usarCategoriaSugerida() {
        _ui.value.categoriaSugerida?.let(::cambiarCategoria)
    }

    private fun cambiar(transformar: (EditorUi) -> EditorUi) {
        _ui.update(transformar)
        guardarBorrador()
    }

    /** Solo los servicios nuevos tienen borrador; se guarda 0,5 s después del último cambio. */
    private fun guardarBorrador() {
        val id = usuarioId ?: return
        if (_ui.value.esEdicion) return
        guardadoDelBorrador?.cancel()
        guardadoDelBorrador = viewModelScope.launch {
            delay(500)
            val u = _ui.value
            runCatching { borradores.guardar(id, BorradorServicio(u.titulo, u.descripcion, u.categoria?.name, u.precio, u.disponible)) }
        }
    }

    /** Convierte la foto a WebP, la analiza con ML Kit y la deja lista para subirla al guardar. */
    fun agregarFoto(uri: Uri, desdeCamara: Boolean) {
        if (!_ui.value.puedeAgregarFoto) {
            if (desdeCamara) conversor.borrarTemporal(uri)
            _mensajes.tryEmit("Un servicio admite como máximo ${PrestadorRepository.MAX_FOTOS_POR_SERVICIO} fotos.")
            return
        }
        _ui.update { it.copy(analizandoFoto = true) }
        viewModelScope.launch {
            try {
                val imagen = conversor.convertir(uri)
                val etiquetas = etiquetador.etiquetar(imagen.paraEtiquetar)
                val sugerida = SugerenciaCategoria.sugerir(etiquetas)
                val pendiente = FotoPendiente(siguienteIdFoto++, imagen, etiquetas)
                _ui.update {
                    it.copy(
                        analizandoFoto = false,
                        pendientes = it.pendientes + pendiente,
                        etiquetasUltimaFoto = etiquetas,
                        categoriaSugerida = sugerida ?: it.categoriaSugerida
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: ErrorFoto) {
                _ui.update { it.copy(analizandoFoto = false) }
                _mensajes.tryEmit(e.message.orEmpty())
            } catch (e: Exception) {
                _ui.update { it.copy(analizandoFoto = false) }
                _mensajes.tryEmit("No se pudo procesar la foto. Prueba con otra.")
            } finally {
                if (desdeCamara) conversor.borrarTemporal(uri)
            }
        }
    }

    fun quitarPendiente(id: Long) {
        _ui.update { it.copy(pendientes = it.pendientes.filterNot { p -> p.id == id }) }
    }

    fun quitarFotoGuardada(foto: FotoPortafolio) {
        viewModelScope.launch {
            try {
                repositorio.eliminarFoto(foto.id)
                _ui.update { it.copy(fotosGuardadas = it.fotosGuardadas.filterNot { f -> f.id == foto.id }) }
                _mensajes.tryEmit("Quitaste la foto.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _mensajes.tryEmit(MensajesPrestador.alGuardarServicio(e))
            }
        }
    }

    /** Valida, crea o edita el servicio y sube las fotos pendientes. */
    fun guardar() {
        val u = _ui.value
        if (u.guardando) return
        val precio = ValidacionesServicio.precio(u.precio)
        val errores = _ui.value.copy(
            errorTitulo = ValidacionesServicio.titulo(u.titulo),
            errorDescripcion = ValidacionesServicio.descripcion(u.descripcion),
            errorCategoria = ValidacionesServicio.categoria(u.categoria),
            errorPrecio = precio.error
        )
        _ui.value = errores
        if (listOf(errores.errorTitulo, errores.errorDescripcion, errores.errorCategoria, errores.errorPrecio).any { it != null }) return

        val datos = DatosServicio(u.titulo, u.descripcion, u.categoria!!, precio.valor, u.disponible)
        _ui.update { it.copy(guardando = true, progreso = if (it.esEdicion) "Guardando…" else "Publicando…") }
        viewModelScope.launch {
            try {
                val id = u.servicioId?.also { repositorio.editar(it, datos) }
                    ?: repositorio.crear(datos).id.also { nuevo -> _ui.update { it.copy(servicioId = nuevo) } }
                usuarioId?.let { runCatching { borradores.borrar(it) } }
                subirPendientes(id)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(guardando = false, progreso = null) }
                _mensajes.tryEmit(MensajesPrestador.alGuardarServicio(e))
            }
        }
    }

    private suspend fun subirPendientes(servicioId: String) {
        val total = _ui.value.pendientes.size
        var fallidas = 0
        _ui.value.pendientes.forEachIndexed { indice, pendiente ->
            _ui.update {
                it.copy(
                    progreso = "Subiendo foto ${indice + 1} de $total…",
                    pendientes = it.pendientes.map { p -> if (p.id == pendiente.id) p.copy(estado = EstadoSubida.Subiendo) else p }
                )
            }
            try {
                val foto = repositorio.subirFoto(servicioId, pendiente.imagen.webp, pendiente.etiquetas.map { it.texto })
                _ui.update {
                    it.copy(
                        pendientes = it.pendientes.filterNot { p -> p.id == pendiente.id },
                        fotosGuardadas = it.fotosGuardadas + foto
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                fallidas++
                val mensaje = MensajesPrestador.alSubirFoto(e)
                _ui.update {
                    it.copy(pendientes = it.pendientes.map { p -> if (p.id == pendiente.id) p.copy(estado = EstadoSubida.Fallo(mensaje)) else p })
                }
            }
        }
        if (fallidas == 0) {
            _ui.update { it.copy(guardando = false, progreso = null, finalizado = true) }
            _mensajes.tryEmit("Servicio guardado.")
        } else {
            _ui.update { it.copy(guardando = false, progreso = null) }
            _mensajes.tryEmit(
                "El servicio se guardó, pero ${if (fallidas == 1) "1 foto no se subió" else "$fallidas fotos no se subieron"}. " +
                    "Toca \"Guardar cambios\" para reintentar o quítalas."
            )
        }
    }

    /** Al terminar marca [EditorUi.finalizado], que hace que la pantalla se cierre. */
    fun eliminarServicio() {
        val id = _ui.value.servicioId ?: return
        _ui.update { it.copy(guardando = true, progreso = "Eliminando…") }
        viewModelScope.launch {
            try {
                repositorio.eliminar(id)
                _ui.update { it.copy(guardando = false, progreso = null, finalizado = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(guardando = false, progreso = null) }
                _mensajes.tryEmit(MensajesPrestador.alGuardarServicio(e))
            }
        }
    }

    private fun textoDePrecio(valor: Double): String =
        if (valor % 1.0 == 0.0) valor.toLong().toString() else valor.toString().replace('.', ',')
}
