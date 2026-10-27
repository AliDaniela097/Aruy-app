package ec.edu.puce.aruy.data.remote.api

import ec.edu.puce.aruy.data.remote.dto.FotoDto
import ec.edu.puce.aruy.data.remote.dto.ServicioDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ServicioApi {

    @GET("servicios")
    suspend fun obtenerServicios(): List<ServicioDto>

    @GET("servicios/{id}")
    suspend fun obtenerServicioPorId(
        @Path("id") id: String
    ): ServicioDto

    @GET("servicios")
    suspend fun obtenerServiciosPorPrestador(
        @Query("prestador_id") prestadorId: String
    ): List<ServicioDto>

    @GET("servicios/{id}/fotos")
    suspend fun obtenerFotos(
        @Path("id") id: String
    ): List<FotoDto>
}
