package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.remote.util.ApiErrorHandler
import ec.edu.puce.aruy.data.repository.AuthRepository
import ec.edu.puce.aruy.data.repository.DatosRegistro
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.model.Sesion
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException

/** Estado de un formulario que habla con el servidor (login o registro). */
data class EstadoFormularioAuth(
    val cargando: Boolean = false,
    /** Mensaje general, se anuncia con `role="alert"` en el prototipo. */
    val error: String? = null,
    /** Error que pertenece al campo del correo (por ejemplo, "Ya existe una cuenta..."). */
    val errorCorreo: String? = null
)

class AuthViewModel(
    private val repositorio: AuthRepository,
    private val sesionManager: SesionManager
) : ViewModel() {

    val estadoSesion = sesionManager.estado
    val aviso: StateFlow<String?> = sesionManager.aviso

    private val _formulario = MutableStateFlow(EstadoFormularioAuth())
    val formulario: StateFlow<EstadoFormularioAuth> = _formulario.asStateFlow()

    fun iniciarSesion(correo: String, password: String, alExito: (Sesion) -> Unit) {
        ejecutar(::mensajeDeLogin) {
            val sesion = repositorio.iniciarSesion(correo, password)
            alExito(sesion)
        }
    }

    fun registrar(datos: DatosRegistro, alExito: (Sesion) -> Unit) {
        ejecutar(::mensajeDeRegistro) {
            val sesion = repositorio.registrar(datos)
            alExito(sesion)
        }
    }

    fun limpiarError() {
        _formulario.value = EstadoFormularioAuth()
    }

    fun avisoMostrado() = sesionManager.avisoMostrado()

    fun entrarComoInvitado() = sesionManager.entrarComoInvitado()

    fun cerrarSesion() {
        viewModelScope.launch { repositorio.cerrarSesion() }
    }

    private fun ejecutar(
        mensaje: (Exception) -> EstadoFormularioAuth,
        bloque: suspend () -> Unit
    ) {
        if (_formulario.value.cargando) return
        _formulario.value = EstadoFormularioAuth(cargando = true)
        viewModelScope.launch {
            try {
                bloque()
                _formulario.value = EstadoFormularioAuth()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _formulario.value = mensaje(e)
            }
        }
    }

    companion object {

        fun mensajeDeLogin(error: Exception): EstadoFormularioAuth {
            if (error is HttpException) {
                when (error.code()) {
                    400 -> return EstadoFormularioAuth(error = "Escribe tu correo y tu contraseña.")
                    401 -> return EstadoFormularioAuth(error = "El correo o la contraseña no son correctos.")
                }
            }
            return EstadoFormularioAuth(error = ApiErrorHandler.obtenerMensaje(error))
        }

        fun mensajeDeRegistro(error: Exception): EstadoFormularioAuth {
            if (error is HttpException) {
                when (error.code()) {
                    409 -> return EstadoFormularioAuth(
                        errorCorreo = "Ya existe una cuenta con este correo o celular. Prueba iniciar sesión."
                    )
                    400 -> return EstadoFormularioAuth(
                        error = ApiErrorHandler.mensajeDelServidor(error)
                            ?: "Revisa los datos: hay algún campo incorrecto."
                    )
                }
            }
            return EstadoFormularioAuth(error = ApiErrorHandler.obtenerMensaje(error))
        }

        fun fabrica(repositorio: AuthRepository, sesionManager: SesionManager): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AuthViewModel(repositorio, sesionManager) as T
            }
    }
}
