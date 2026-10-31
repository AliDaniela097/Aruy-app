package ec.edu.puce.aruy.data.remote.api

import ec.edu.puce.aruy.data.remote.dto.EditarUsuarioRequest
import ec.edu.puce.aruy.data.remote.dto.UsuarioDto
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

interface UsuarioApi {

    @GET("usuarios/{id}")
    suspend fun obtenerUsuario(
        @Path("id") id: String
    ): UsuarioDto

    /** Mis datos; es la única consulta que incluye mi correo. */
    @GET("usuarios/yo")
    suspend fun miUsuario(): UsuarioDto

    @PATCH("usuarios/yo")
    suspend fun editarMiUsuario(@Body cuerpo: EditarUsuarioRequest): UsuarioDto

    /** Eliminar mi cuenta: el servidor borra la cuenta, el perfil, los servicios, las fotos y las opiniones. */
    @DELETE("usuarios/yo")
    suspend fun eliminarMiCuenta()
}
