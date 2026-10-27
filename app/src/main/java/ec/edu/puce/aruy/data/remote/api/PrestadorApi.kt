package ec.edu.puce.aruy.data.remote.api

import ec.edu.puce.aruy.data.remote.dto.CrearServicioRequest
import ec.edu.puce.aruy.data.remote.dto.FotoSubidaDto
import ec.edu.puce.aruy.data.remote.dto.PerfilPropioDto
import ec.edu.puce.aruy.data.remote.dto.ServicioDto
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

/** Lo que solo puede hacer un prestador con sesión: su perfil, sus servicios y sus fotos. */
interface PrestadorApi {

    @GET("perfiles/yo")
    suspend fun miPerfil(): PerfilPropioDto

    /**
     * Solo viajan los campos que cambian. Va como [RequestBody] ya armado porque para vaciar la
     * descripción hay que mandar `null`, y el convertidor de Gson descarta los `null`.
     */
    @PATCH("perfiles/yo")
    suspend fun editarMiPerfil(@Body cambios: RequestBody): PerfilPropioDto

    @POST("servicios")
    suspend fun crearServicio(@Body cuerpo: CrearServicioRequest): ServicioDto

    /**
     * El cuerpo es un JSON con solo los campos a cambiar; `precio_referencial: null` borra el precio.
     * Va como [RequestBody] ya armado porque el convertidor de Gson descarta los `null` de un JsonObject.
     */
    @PATCH("servicios/{id}")
    suspend fun editarServicio(@Path("id") id: String, @Body cambios: RequestBody): ServicioDto

    /** Borra el servicio, sus fotos, sus etiquetas y los archivos en Supabase Storage. */
    @DELETE("servicios/{id}")
    suspend fun eliminarServicio(@Path("id") id: String)

    /**
     * Sube una foto: el cuerpo es la imagen tal cual, como `image/webp`. No es multipart ni base64.
     * `etiquetas` va separado por comas (resultado de ML Kit).
     */
    @POST("servicios/{id}/fotos")
    suspend fun subirFoto(
        @Path("id") servicioId: String,
        @Body imagen: RequestBody,
        @Query("etiquetas") etiquetas: String? = null,
        @Query("descripcion") descripcion: String? = null
    ): FotoSubidaDto

    /** Borra la foto, sus etiquetas y el archivo en Supabase Storage. */
    @DELETE("fotos/{id}")
    suspend fun eliminarFoto(@Path("id") fotoId: String)
}
