package ec.edu.puce.aruy.data.remote.api

import ec.edu.puce.aruy.data.remote.dto.UsuarioDto
import retrofit2.http.GET
import retrofit2.http.Path

interface UsuarioApi {

    @GET("usuarios/{id}")
    suspend fun obtenerUsuario(
        @Path("id") id: String
    ): UsuarioDto
}