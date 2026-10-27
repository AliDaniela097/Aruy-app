package ec.edu.puce.aruy.data.remote.util

import retrofit2.HttpException

/** Mensajes en español para los errores del servidor al publicar servicios y subir fotos. */
object MensajesPrestador {

    /** `POST /servicios/{id}/fotos` (ver las respuestas en routes/servicios.js del backend). */
    fun alSubirFoto(error: Exception): String {
        if (error is HttpException) {
            when (error.code()) {
                400, 415 -> return "La foto no es un JPG, PNG o WEBP válido."
                403 -> return "Ese servicio no es tuyo."
                404 -> return "No encontramos el servicio. Puede que ya se haya eliminado."
                409 -> return "Este servicio ya tiene 10 fotos, el máximo permitido."
                413 -> return "La foto es demasiado grande para enviarla."
                422 -> return "No se pudo reducir la foto a 500 KB. Prueba con otra más pequeña."
                429 -> return "Subiste muchas fotos en poco tiempo. Espera unos minutos."
                502 -> return "No se pudo guardar la foto en el almacenamiento. Inténtalo de nuevo."
                503 -> return "El servidor no puede guardar fotos ahora mismo. Inténtalo más tarde."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }

    /** `POST`, `PATCH` y `DELETE` de `/servicios`. */
    fun alGuardarServicio(error: Exception): String {
        if (error is HttpException) {
            when (error.code()) {
                400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Revisa los datos del servicio."
                403 -> return "Ese servicio no es tuyo."
                404 -> return "No encontramos el servicio. Puede que ya se haya eliminado."
                409 -> return "Primero crea tu perfil de prestador."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }
}
