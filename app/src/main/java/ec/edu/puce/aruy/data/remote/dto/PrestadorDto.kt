package ec.edu.puce.aruy.data.remote.dto

import com.google.gson.annotations.SerializedName

/** `GET /perfiles/yo` y `PATCH /perfiles/yo`: mi propio perfil de prestador. */
data class PerfilPropioDto(
    @SerializedName("id") val id: String,
    @SerializedName("usuario_id") val usuarioId: String? = null,
    @SerializedName("oficio_principal") val oficioPrincipal: String? = null,
    @SerializedName("descripcion_profesional") val descripcionProfesional: String? = null,
    @SerializedName("zona_cobertura") val zonaCobertura: String? = null,
    @SerializedName("anios_experiencia") val aniosExperiencia: Int? = null,
    @SerializedName("disponible") val disponible: Boolean = true
)

/** Cuerpo de `POST /servicios`. Las fotos ya no van aquí: se suben después, una por una. */
data class CrearServicioRequest(
    @SerializedName("titulo") val titulo: String,
    @SerializedName("descripcion") val descripcion: String,
    @SerializedName("categoria") val categoria: String,
    @SerializedName("precio_referencial") val precioReferencial: Double? = null,
    @SerializedName("disponible") val disponible: Boolean = true
)

/** Respuesta 201 de `POST /servicios/{id}/fotos`. */
data class FotoSubidaDto(
    @SerializedName("id") val id: String,
    @SerializedName("referencia_imagen") val referenciaImagen: String,
    @SerializedName("url") val url: String? = null,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("orden") val orden: Int? = null,
    @SerializedName("etiquetas") val etiquetas: List<String>? = null,
    @SerializedName("formato") val formato: String? = null,
    @SerializedName("ancho") val ancho: Int? = null,
    @SerializedName("alto") val alto: Int? = null,
    @SerializedName("bytes") val bytes: Int? = null,
    @SerializedName("calidad") val calidad: Int? = null
)
