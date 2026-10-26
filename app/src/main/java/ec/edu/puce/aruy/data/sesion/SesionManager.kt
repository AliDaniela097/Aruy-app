package ec.edu.puce.aruy.data.sesion

import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.Sesion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Única fuente de verdad de la sesión. La interfaz observa [estado]; el [AuthInterceptor]
 * lee [tokenActual] en cada petición y avisa con [expirar] si el servidor responde 401.
 */
class SesionManager(
    private val almacen: AlmacenSesion,
    private val alcance: CoroutineScope,
    private val ahoraMs: () -> Long = System::currentTimeMillis
) {

    private val _estado = MutableStateFlow<EstadoSesion>(EstadoSesion.Cargando)
    val estado: StateFlow<EstadoSesion> = _estado.asStateFlow()

    /** Aviso para mostrar una sola vez en el login ("Tu sesión venció..."). */
    private val _aviso = MutableStateFlow<String?>(null)
    val aviso: StateFlow<String?> = _aviso.asStateFlow()

    @Volatile
    private var token: String? = null

    fun tokenActual(): String? = token

    /** Lee la sesión guardada. Si el token ya venció (dura 8 horas) se descarta. */
    suspend fun cargar() {
        val guardada = runCatching { almacen.leer() }.getOrNull()
        if (guardada == null) {
            _estado.value = EstadoSesion.SinSesion
        } else if (Jwt.estaVencido(guardada.token, ahoraMs())) {
            runCatching { almacen.borrar() }
            _aviso.value = MENSAJE_VENCIDA
            _estado.value = EstadoSesion.SinSesion
        } else {
            token = guardada.token
            _estado.value = EstadoSesion.Autenticada(guardada)
        }
    }

    suspend fun iniciar(sesion: Sesion) {
        almacen.guardar(sesion)
        token = sesion.token
        _aviso.value = null
        _estado.value = EstadoSesion.Autenticada(sesion)
    }

    suspend fun cerrar() {
        token = null
        runCatching { almacen.borrar() }
        _estado.value = EstadoSesion.SinSesion
    }

    fun entrarComoInvitado() {
        token = null
        _estado.value = EstadoSesion.Invitado
    }

    /** El servidor rechazó el token (401): se cierra la sesión y se avisa en el login. */
    fun expirar() {
        if (token == null) return
        token = null
        _aviso.value = MENSAJE_VENCIDA
        _estado.value = EstadoSesion.SinSesion
        alcance.launch { runCatching { almacen.borrar() } }
    }

    fun avisoMostrado() {
        _aviso.value = null
    }

    companion object {
        const val MENSAJE_VENCIDA = "Tu sesión venció. Inicia sesión de nuevo."
    }
}
