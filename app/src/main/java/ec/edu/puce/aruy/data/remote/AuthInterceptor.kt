package ec.edu.puce.aruy.data.remote

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Agrega `Authorization: Bearer <token>` a cada petición cuando hay sesión. Si el servidor
 * responde 401 a una petición que llevaba token, el token venció (dura 8 horas) o se dio de baja
 * la cuenta: se avisa con [alExpirar] para cerrar la sesión.
 * Un 401 del login (contraseña incorrecta) no pasa por aquí porque esa petición no lleva token.
 */
class AuthInterceptor(
    private val token: () -> String?,
    private val alExpirar: () -> Unit
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val tokenActual = token()
        val peticion = if (tokenActual != null) {
            chain.request().newBuilder().header("Authorization", "Bearer $tokenActual").build()
        } else {
            chain.request()
        }
        val respuesta = chain.proceed(peticion)
        if (tokenActual != null && respuesta.code == 401) alExpirar()
        return respuesta
    }
}
