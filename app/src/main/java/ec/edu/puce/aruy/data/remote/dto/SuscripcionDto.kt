package ec.edu.puce.aruy.data.remote.dto

import com.google.gson.annotations.SerializedName

/** `GET /suscripciones/mia`, `POST /suscripciones` y `PATCH /suscripciones/{id}/cancelar`. */
data class SuscripcionDto(
    @SerializedName("id") val id: String,
    @SerializedName("prestador_id") val prestadorId: String? = null,
    @SerializedName("tipo_plan") val tipoPlan: String,
    @SerializedName("estado") val estado: String,
    @SerializedName("fecha_inicio") val fechaInicio: String? = null,
    @SerializedName("fecha_fin") val fechaFin: String? = null,
    @SerializedName("renovacion_automatica") val renovacionAutomatica: Boolean = false,
    /** El backend marca con `true` las suscripciones sin pago real. */
    @SerializedName("simulada") val simulada: Boolean? = null
)

data class SuscribirseRequest(
    @SerializedName("tipo_plan") val tipoPlan: String,
    @SerializedName("renovacion_automatica") val renovacionAutomatica: Boolean = false
)
