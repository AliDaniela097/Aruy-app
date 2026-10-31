package ec.edu.puce.aruy.data.repository

import com.google.gson.JsonNull
import com.google.gson.JsonObject
import ec.edu.puce.aruy.data.mapper.toDomain
import ec.edu.puce.aruy.data.remote.api.PrestadorApi
import ec.edu.puce.aruy.data.remote.api.UsuarioApi
import ec.edu.puce.aruy.data.remote.dto.EditarUsuarioRequest
import ec.edu.puce.aruy.data.remote.dto.PerfilPropioDto
import ec.edu.puce.aruy.domain.model.PerfilPrestador
import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.domain.validation.Validaciones

/** Mis datos de cuenta: el correo solo lo devuelve `GET /usuarios/yo`. */
data class MisDatos(val usuario: Usuario, val email: String?)

/** Datos que el prestador puede editar de su perfil público. */
data class DatosPerfilPrestador(
    val oficio: String,
    val zona: String,
    val descripcion: String?,
    val anios: Int?
)

/** Perfil y cuenta propios: editar datos y darse de baja. */
class PerfilRepository(
    private val usuarioApi: UsuarioApi,
    private val prestadorApi: PrestadorApi
) {

    suspend fun misDatos(): MisDatos {
        val dto = usuarioApi.miUsuario()
        return MisDatos(dto.toDomain(), dto.email)
    }

    suspend fun actualizarMisDatos(nombre: String, telefono: String): Usuario =
        usuarioApi.editarMiUsuario(
            EditarUsuarioRequest(nombre = nombre.trim(), telefono = Validaciones.normalizarCelular(telefono))
        ).toDomain()

    suspend fun miPerfil(): PerfilPrestador = prestadorApi.miPerfil().toPerfil()

    suspend fun actualizarMiPerfil(datos: DatosPerfilPrestador): PerfilPrestador =
        prestadorApi.editarMiPerfil(PrestadorRepository.comoJson(cuerpoDePerfil(datos))).toPerfil()

    /** El servidor borra la cuenta y todos sus datos (servicios, fotos, opiniones y suscripción). */
    suspend fun eliminarCuenta() = usuarioApi.eliminarMiCuenta()

    private fun PerfilPropioDto.toPerfil() = PerfilPrestador(
        id = id,
        usuarioId = usuarioId.orEmpty(),
        oficioPrincipal = oficioPrincipal.orEmpty(),
        descripcionProfesional = descripcionProfesional,
        zonaCobertura = zonaCobertura.orEmpty(),
        aniosExperiencia = aniosExperiencia,
        disponible = disponible
    )

    companion object {
        /** La descripción y los años vacíos viajan como `null` para borrarlos. */
        fun cuerpoDePerfil(datos: DatosPerfilPrestador): JsonObject = JsonObject().apply {
            addProperty("oficio_principal", datos.oficio.trim())
            addProperty("zona_cobertura", datos.zona.trim())
            val descripcion = datos.descripcion?.trim().orEmpty()
            if (descripcion.isEmpty()) add("descripcion_profesional", JsonNull.INSTANCE)
            else addProperty("descripcion_profesional", descripcion)
            if (datos.anios == null) add("anios_experiencia", JsonNull.INSTANCE)
            else addProperty("anios_experiencia", datos.anios)
        }
    }
}
