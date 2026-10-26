package ec.edu.puce.aruy.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Respuesta esperada de `GET /perfiles` y `GET /perfiles/{id}` (endpoint público de solo lectura,
 * pendiente en el backend: ver docs/backend-pedido-perfiles-publicos.md).
 */
data class PerfilPublicoDto(
    @SerializedName("id") val id: String,
    @SerializedName("usuario_id") val usuarioId: String? = null,
    @SerializedName("nombre") val nombre: String? = null,
    @SerializedName("telefono") val telefono: String? = null,
    @SerializedName("foto_perfil") val fotoPerfil: String? = null,
    @SerializedName("estado_verificacion") val estadoVerificacion: String? = null,
    @SerializedName("oficio_principal") val oficioPrincipal: String? = null,
    @SerializedName("descripcion_profesional") val descripcionProfesional: String? = null,
    @SerializedName("zona_cobertura") val zonaCobertura: String? = null,
    @SerializedName("anios_experiencia") val aniosExperiencia: Int? = null,
    @SerializedName("disponible") val disponible: Boolean? = null,
    @SerializedName("calificacion_promedio") val calificacionPromedio: Double? = null,
    @SerializedName("total_calificaciones") val totalCalificaciones: Int? = null
)

/** Elemento de `GET /servicios/{id}/fotos`. */
data class FotoDto(
    @SerializedName("id") val id: String,
    @SerializedName("referencia_imagen") val referenciaImagen: String,
    @SerializedName("url") val url: String? = null,
    @SerializedName("descripcion") val descripcion: String? = null,
    @SerializedName("orden") val orden: Int? = null,
    @SerializedName("etiquetas") val etiquetas: List<String>? = null
)

/** Respuesta de `GET /calificaciones?perfil_prestador_id=`. */
data class CalificacionesDto(
    @SerializedName("promedio") val promedio: Double? = null,
    @SerializedName("total") val total: Int? = null,
    @SerializedName("calificaciones") val calificaciones: List<OpinionDto>? = null
)

data class OpinionDto(
    @SerializedName("id") val id: String,
    @SerializedName("puntuacion") val puntuacion: Int,
    @SerializedName("comentario") val comentario: String? = null,
    @SerializedName("fecha_creacion") val fechaCreacion: String? = null,
    @SerializedName("contratante") val contratante: String? = null
)

/** Cuerpo de `POST /calificaciones`. */
data class CalificarRequest(
    @SerializedName("perfil_prestador_id") val perfilPrestadorId: String,
    @SerializedName("puntuacion") val puntuacion: Int,
    @SerializedName("comentario") val comentario: String? = null
)
