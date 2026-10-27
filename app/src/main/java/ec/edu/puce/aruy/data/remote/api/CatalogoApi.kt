package ec.edu.puce.aruy.data.remote.api

import ec.edu.puce.aruy.data.remote.dto.CalificacionesDto
import ec.edu.puce.aruy.data.remote.dto.CalificarRequest
import ec.edu.puce.aruy.data.remote.dto.FotoDto
import ec.edu.puce.aruy.data.remote.dto.OpinionDto
import ec.edu.puce.aruy.data.remote.dto.PerfilPublicoDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Perfiles públicos de prestadores (solo lectura). */
interface PerfilApi {

    @GET("perfiles")
    suspend fun listar(): List<PerfilPublicoDto>

    @GET("perfiles/{id}")
    suspend fun obtener(@Path("id") id: String): PerfilPublicoDto
}

interface CalificacionApi {

    @GET("calificaciones")
    suspend fun listar(@Query("perfil_prestador_id") perfilPrestadorId: String): CalificacionesDto

    @POST("calificaciones")
    suspend fun calificar(@Body cuerpo: CalificarRequest): OpinionDto
}
