package ec.edu.puce.aruy.domain.model

data class Suscripcion(
    val id: String,
    val prestadorId: String,
    val tipoPlan: TipoPlan,
    val estado: EstadoSuscripcion,
    val fechaInicio: String,
    val fechaFin: String? = null,
    val renovacionAutomatica: Boolean = false
)