package ec.edu.puce.aruy.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)

/**
 * Cuerpo de POST /auth/registro. Los campos de prestador van solo si el rol es PRESTADOR;
 * los nulos no se envían (Gson los omite).
 */
data class RegistroRequest(
    @SerializedName("nombre") val nombre: String,
    @SerializedName("telefono") val telefono: String,
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String,
    @SerializedName("rol") val rol: String,
    @SerializedName("oficio_principal") val oficioPrincipal: String? = null,
    @SerializedName("zona_cobertura") val zonaCobertura: String? = null,
    @SerializedName("descripcion_profesional") val descripcionProfesional: String? = null,
    @SerializedName("anios_experiencia") val aniosExperiencia: Int? = null
)

/** Respuesta de /auth/login y /auth/registro. */
data class AuthRespuestaDto(
    @SerializedName("token") val token: String,
    @SerializedName("usuario") val usuario: UsuarioDto,
    @SerializedName("perfil_prestador_id") val perfilPrestadorId: String? = null
)
