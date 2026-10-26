package ec.edu.puce.aruy.data.remote.util

import com.google.gson.JsonParser
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
                    409 -> "Esa acción ya no es posible: los datos cambiaron o ya existe un registro igual."
                    429 -> "Demasiados intentos en poco tiempo. Espera unos minutos e inténtalo de nuevo."
                    500 -> "Ocurrió un error interno en el servidor."
                    else -> "Error del servidor: ${error.code()}"
                }
            }

            else -> {
                "Ocurrió un error inesperado. Inténtalo nuevamente."
            }
        }
    }

    /** El backend responde los errores como `{"error": "texto"}`; devuelve ese texto si viene. */
    fun mensajeDelServidor(error: HttpException): String? = runCatching {
        val cuerpo = error.response()?.errorBody()?.string() ?: return null
        JsonParser.parseString(cuerpo).asJsonObject.get("error")?.asString
    }.getOrNull()
}
