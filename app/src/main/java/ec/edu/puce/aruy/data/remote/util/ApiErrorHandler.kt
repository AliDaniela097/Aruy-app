package ec.edu.puce.aruy.data.remote.util

import com.google.gson.JsonParser
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

object ApiErrorHandler {

    const val MENSAJE_SIN_CONEXION = "No se pudo conectar con el servidor. Verifica tu conexión a internet."
    const val MENSAJE_LENTO = "El servidor tardó demasiado en responder. Inténtalo nuevamente."

    /** true si el error es de red (sin internet, servidor apagado o muy lento), no de datos. */
    fun esDeRed(mensaje: String): Boolean = mensaje == MENSAJE_SIN_CONEXION || mensaje == MENSAJE_LENTO

    fun obtenerMensaje(error: Exception): String {
        return when (error) {

            is SocketTimeoutException -> {
                MENSAJE_LENTO
            }

            is IOException -> {
                MENSAJE_SIN_CONEXION
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
