package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.remote.util.ApiErrorHandler
import ec.edu.puce.aruy.data.repository.CatalogoRepository
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.model.PrestadorPublico
import ec.edu.puce.aruy.domain.model.ResumenOpiniones
import ec.edu.puce.aruy.domain.model.Servicio
import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Mensaje claro para los errores de calificar (`POST /calificaciones`). */
internal fun mensajeDeCalificar(error: Exception): String {
    if (error is HttpException) {
        when (error.code()) {
            409 -> return "Ya calificaste a este prestador."
            403 -> return "Solo las personas que contratan servicios pueden calificar."
            404 -> return "Este prestador ya no está disponible."
            400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Revisa la calificación e inténtalo de nuevo."
        }
    }
    return ApiErrorHandler.obtenerMensaje(error)
}

data class DetalleUi(
    val cargando: Boolean = true,
    val error: String? = null,
    val item: ServicioConPrestador? = null,
    val fotos: List<FotoPortafolio> = emptyList(),
    val opiniones: ResumenOpiniones? = null,
    val enviandoOpinion: Boolean = false
)

class DetalleViewModel(
    private val servicioId: String,
    inicial: ServicioConPrestador?,
    private val repositorio: CatalogoRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(DetalleUi(item = inicial, cargando = inicial == null))
    val ui: StateFlow<DetalleUi> = _ui.asStateFlow()

    /** Avisos de una sola vez ("Opinión publicada. ¡Gracias!") para el snackbar. */
    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = it.item == null, error = null) }
            try {
                val servicio: Servicio = _ui.value.item?.servicio ?: repositorio.servicio(servicioId)
                val prestador: PrestadorPublico? = _ui.value.item?.prestador ?: repositorio.prestador(servicio.prestadorId)
                _ui.update { it.copy(item = ServicioConPrestador(servicio, prestador), cargando = false) }

                coroutineScope {
                    val fotos = async { runCatching { repositorio.fotos(servicioId, forzar = true) }.getOrDefault(emptyList()) }
                    val opiniones = async { runCatching { repositorio.opiniones(servicio.prestadorId) }.getOrNull() }
                    _ui.update { it.copy(fotos = fotos.await(), opiniones = opiniones.await()) }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, error = ApiErrorHandler.obtenerMensaje(e)) }
            }
        }
    }

    fun calificar(puntuacion: Int, comentario: String?, alExito: () -> Unit) {
        val prestadorId = _ui.value.item?.servicio?.prestadorId ?: return
        if (_ui.value.enviandoOpinion) return
        _ui.update { it.copy(enviandoOpinion = true) }
        viewModelScope.launch {
            try {
                repositorio.calificar(prestadorId, puntuacion, comentario)
                val resumen = runCatching { repositorio.opiniones(prestadorId) }.getOrNull()
                _ui.update { it.copy(enviandoOpinion = false, opiniones = resumen ?: it.opiniones) }
                _mensajes.tryEmit("Opinión publicada. ¡Gracias!")
                alExito()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(enviandoOpinion = false) }
                _mensajes.tryEmit(mensajeDeCalificar(e))
            }
        }
    }
}

data class PrestadorUi(
    val cargando: Boolean = true,
    val error: String? = null,
    val prestador: PrestadorPublico? = null,
    val servicios: List<Servicio> = emptyList(),
    val fotos: List<FotoPortafolio> = emptyList(),
    val opiniones: ResumenOpiniones? = null
)

/** Perfil público de un prestador: sus servicios, fotos de trabajos y opiniones. */
class PrestadorViewModel(
    private val perfilId: String,
    inicial: PrestadorPublico?,
    private val repositorio: CatalogoRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(PrestadorUi(prestador = inicial))
    val ui: StateFlow<PrestadorUi> = _ui.asStateFlow()

    init {
        cargar()
    }

    fun cargar() {
        viewModelScope.launch {
            _ui.update { it.copy(cargando = true, error = null) }
            try {
                coroutineScope {
                    val prestador = async { _ui.value.prestador ?: repositorio.prestador(perfilId) }
                    val servicios = async { repositorio.serviciosDe(perfilId) }
                    val opiniones = async { runCatching { repositorio.opiniones(perfilId) }.getOrNull() }
                    val lista = servicios.await()
                    val fotos = lista.take(MAX_SERVICIOS_CON_FOTOS)
                        .map { s -> async { runCatching { repositorio.fotos(s.id) }.getOrDefault(emptyList()) } }
                        .flatMap { it.await() }
                    _ui.update {
                        it.copy(
                            cargando = false,
                            prestador = prestador.await(),
                            servicios = lista,
                            fotos = fotos,
                            opiniones = opiniones.await()
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, error = ApiErrorHandler.obtenerMensaje(e)) }
            }
        }
    }

    private companion object {
        const val MAX_SERVICIOS_CON_FOTOS = 10
    }
}
