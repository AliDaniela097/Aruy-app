package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.mapper.toDomain
import ec.edu.puce.aruy.data.remote.api.AuthApi
import ec.edu.puce.aruy.data.remote.dto.AuthRespuestaDto
import ec.edu.puce.aruy.data.remote.dto.LoginRequest
import ec.edu.puce.aruy.data.remote.dto.RegistroRequest
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Sesion
import ec.edu.puce.aruy.domain.validation.Validaciones

/** Datos del formulario de registro. Los de prestador solo se usan si [rol] es PRESTADOR. */
data class DatosRegistro(
    val nombre: String,
    val correo: String,
    val celular: String,
    val password: String,
    val rol: RolUsuario,
    val oficio: String? = null,
    val zona: String? = null
)

class AuthRepository(
    private val authApi: AuthApi,
    private val sesionManager: SesionManager
) {

    suspend fun iniciarSesion(correo: String, password: String): Sesion {
        val respuesta = authApi.login(LoginRequest(correo.trim().lowercase(), password))
        return guardar(respuesta)
    }

    suspend fun registrar(datos: DatosRegistro): Sesion {
        val esPrestador = datos.rol == RolUsuario.PRESTADOR
        val respuesta = authApi.registrar(
            RegistroRequest(
                nombre = datos.nombre.trim(),
                telefono = Validaciones.normalizarCelular(datos.celular),
                email = datos.correo.trim().lowercase(),
                password = datos.password,
                rol = datos.rol.name,
                oficioPrincipal = if (esPrestador) datos.oficio?.trim() else null,
                zonaCobertura = if (esPrestador) datos.zona?.trim() else null
            )
        )
        return guardar(respuesta)
    }

    private suspend fun guardar(respuesta: AuthRespuestaDto): Sesion {
        val sesion = Sesion(
            token = respuesta.token,
            usuario = respuesta.usuario.toDomain(),
            perfilPrestadorId = respuesta.perfilPrestadorId
        )
        sesionManager.iniciar(sesion)
        return sesion
    }

    suspend fun cerrarSesion() = sesionManager.cerrar()
}
