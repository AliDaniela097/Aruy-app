package ec.edu.puce.aruy.domain.model

data class PerfilPrestador(
    val id: String,
    val usuarioId: String,
    val oficioPrincipal: String,
    val descripcionProfesional: String? = null,
    val zonaCobertura: String,
    val aniosExperiencia: Int? = null,
    val disponible: Boolean = true
)
