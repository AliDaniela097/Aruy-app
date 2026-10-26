package ec.edu.puce.aruy.data.remote.api

import ec.edu.puce.aruy.data.remote.dto.SuscribirseRequest
import ec.edu.puce.aruy.data.remote.dto.SuscripcionDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

/** Aruy Pro: suscripciones SIMULADAS (no hay cobro real). Solo para prestadores. */
interface SuscripcionApi {

    /** La más reciente. Responde 404 si nunca tuviste una. */
    @GET("suscripciones/mia")
    suspend fun miSuscripcion(): SuscripcionDto

    /** Queda ACTIVA de inmediato por 7 o 30 días. Responde 409 si ya hay una pendiente o activa. */
    @POST("suscripciones")
    suspend fun suscribirse(@Body cuerpo: SuscribirseRequest): SuscripcionDto

    @PATCH("suscripciones/{id}/cancelar")
    suspend fun cancelar(@Path("id") id: String): SuscripcionDto
}
