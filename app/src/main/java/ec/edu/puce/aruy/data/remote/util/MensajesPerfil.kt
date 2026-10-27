package ec.edu.puce.aruy.data.remote.util

import retrofit2.HttpException

/** Mensajes en español para los errores de editar el perfil, darse de baja y Aruy Pro. */
object MensajesPerfil {

    /** `PATCH /usuarios/yo` y `PATCH /perfiles/yo`. */
    fun alGuardarPerfil(error: Exception): String {
        if (error is HttpException) {
            when (error.code()) {
                400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Revisa los datos: hay algún campo incorrecto."
                404 -> return "Todavía no tienes perfil de prestador."
                409 -> return "Ese celular ya está registrado en otra cuenta."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }

    /** `POST /suscripciones` y `PATCH /suscripciones/{id}/cancelar`. */
    fun enSuscripciones(error: Exception): String {
        if (error is HttpException) {
            when (error.code()) {
                400 -> return ApiErrorHandler.mensajeDelServidor(error) ?: "Elige uno de los planes."
                403 -> return "Aruy Pro es solo para prestadores."
                404 -> return "No encontramos tu suscripción."
                409 -> return "Ya tienes una suscripción pendiente o activa."
            }
        }
        return ApiErrorHandler.obtenerMensaje(error)
    }
}
