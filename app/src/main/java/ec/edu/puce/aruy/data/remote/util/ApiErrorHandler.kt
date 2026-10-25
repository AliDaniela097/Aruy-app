package ec.edu.puce.aruy.data.remote.util

import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

object ApiErrorHandler {

    fun obtenerMensaje(error: Exception): String {
        return when (error) {

            is SocketTimeoutException -> {
                "El servidor tardó demasiado en responder. Inténtalo nuevamente."
            }

            is IOException -> {
                "No se pudo conectar con el servidor. Verifica tu conexión a internet."
            }

            is HttpException -> {
                when (error.code()) {
                    400 -> "La solicitud contiene datos incorrectos."
                    401 -> "Debes iniciar sesión nuevamente."
                    403 -> "No tienes permisos para realizar esta acción."
                    404 -> "No se encontró el recurso solicitado."
                    500 -> "Ocurrió un error interno en el servidor."
                    else -> "Error del servidor: ${error.code()}"
                }
            }

            else -> {
                "Ocurrió un error inesperado. Inténtalo nuevamente."
            }
        }
    }
}