package ec.edu.puce.aruy.data.repository

import com.google.gson.GsonBuilder
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import ec.edu.puce.aruy.data.mapper.toDomain
import ec.edu.puce.aruy.data.remote.api.PrestadorApi
import ec.edu.puce.aruy.data.remote.api.ServicioApi
import ec.edu.puce.aruy.data.remote.dto.CrearServicioRequest
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.model.Servicio
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody

/** Datos del formulario de un servicio, listos para enviar. */
data class DatosServicio(
    val titulo: String,
    val descripcion: String,
    val categoria: CategoriaServicio,
    val precio: Double?,
    val disponible: Boolean
)

/** Servicios y fotos del propio prestador (PR #17 del backend). */
class PrestadorRepository(
    private val api: PrestadorApi,
    private val servicioApi: ServicioApi
) {

    /** Disponibilidad del perfil: `true` = "Recibiendo solicitudes". */
    suspend fun disponibilidad(): Boolean = api.miPerfil().disponible

    suspend fun cambiarDisponibilidad(disponible: Boolean): Boolean {
        val cuerpo = JsonObject().apply { addProperty("disponible", disponible) }
        return api.editarMiPerfil(comoJson(cuerpo)).disponible
    }

    suspend fun misServicios(perfilId: String): List<Servicio> =
        servicioApi.obtenerServiciosPorPrestador(perfilId).map { it.toDomain() }

    suspend fun servicio(id: String): Servicio = servicioApi.obtenerServicioPorId(id).toDomain()

    suspend fun fotosDe(servicioId: String): List<FotoPortafolio> =
        servicioApi.obtenerFotos(servicioId).map { it.toDomain(servicioId) }.sortedBy { it.orden }

    suspend fun crear(datos: DatosServicio): Servicio =
        api.crearServicio(
            CrearServicioRequest(
                titulo = datos.titulo.trim(),
                descripcion = datos.descripcion.trim(),
                categoria = datos.categoria.name,
                precioReferencial = datos.precio,
                disponible = datos.disponible
            )
        ).toDomain()

    suspend fun editar(id: String, datos: DatosServicio): Servicio =
        api.editarServicio(id, comoJson(cuerpoDeEdicion(datos))).toDomain()

    suspend fun cambiarDisponibilidadDelServicio(id: String, disponible: Boolean): Servicio {
        val cuerpo = JsonObject().apply { addProperty("disponible", disponible) }
        return api.editarServicio(id, comoJson(cuerpo)).toDomain()
    }

    suspend fun eliminar(id: String) = api.eliminarServicio(id)

    /**
     * Sube una foto ya convertida a WebP. El servidor vuelve a comprobar el formato real, las
     * dimensiones y los 500 KB, y guarda en Neon solo la ruta del archivo en Supabase Storage.
     */
    suspend fun subirFoto(
        servicioId: String,
        webp: ByteArray,
        etiquetas: List<String> = emptyList(),
        descripcion: String? = null
    ): FotoPortafolio {
        val foto = api.subirFoto(
            servicioId = servicioId,
            imagen = webp.toRequestBody(TIPO_WEBP.toMediaType()),
            etiquetas = unirEtiquetas(etiquetas),
            descripcion = descripcion?.trim()?.takeIf { it.isNotEmpty() }
        )
        return FotoPortafolio(
            id = foto.id,
            servicioId = servicioId,
            referenciaImagen = foto.referenciaImagen,
            url = foto.url,
            descripcion = foto.descripcion,
            etiquetasIA = foto.etiquetas.orEmpty(),
            orden = foto.orden ?: 0
        )
    }

    suspend fun eliminarFoto(fotoId: String) = api.eliminarFoto(fotoId)

    companion object {
        const val TIPO_WEBP = "image/webp"
        const val MAX_FOTOS_POR_SERVICIO = 10
        private const val MAX_ETIQUETAS = 20
        private const val MAX_LARGO_ETIQUETA = 50

        /** El backend acepta hasta 20 etiquetas de 50 caracteres, separadas por coma en la dirección. */
        fun unirEtiquetas(etiquetas: List<String>): String? =
            etiquetas
                .map { it.replace(',', ' ').trim().take(MAX_LARGO_ETIQUETA) }
                .filter { it.isNotEmpty() }
                .distinct()
                .take(MAX_ETIQUETAS)
                .joinToString(",")
                .ifEmpty { null }

        private val gsonConNulos = GsonBuilder().serializeNulls().create()

        /** Texto JSON que conserva los `null` (el convertidor de Retrofit los quitaría). */
        fun comoJson(cuerpo: JsonObject): RequestBody =
            gsonConNulos.toJson(cuerpo).toRequestBody("application/json".toMediaType())

        /** Todos los campos del formulario; el precio vacío viaja como `null` para borrarlo. */
        fun cuerpoDeEdicion(datos: DatosServicio): JsonObject = JsonObject().apply {
            addProperty("titulo", datos.titulo.trim())
            addProperty("descripcion", datos.descripcion.trim())
            addProperty("categoria", datos.categoria.name)
            if (datos.precio == null) add("precio_referencial", JsonNull.INSTANCE) else addProperty("precio_referencial", datos.precio)
            addProperty("disponible", datos.disponible)
        }
    }
}
