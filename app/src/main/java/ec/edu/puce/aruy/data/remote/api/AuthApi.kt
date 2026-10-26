package ec.edu.puce.aruy.data.remote.api

import ec.edu.puce.aruy.data.remote.dto.AuthRespuestaDto
import ec.edu.puce.aruy.data.remote.dto.LoginRequest
import ec.edu.puce.aruy.data.remote.dto.RegistroRequest
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {

    @POST("auth/login")
    suspend fun login(@Body cuerpo: LoginRequest): AuthRespuestaDto

    @POST("auth/registro")
    suspend fun registrar(@Body cuerpo: RegistroRequest): AuthRespuestaDto
}
