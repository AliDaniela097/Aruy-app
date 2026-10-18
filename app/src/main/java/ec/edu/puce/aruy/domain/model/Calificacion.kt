package ec.edu.puce.aruy.domain.model

data class Calificacion(
    val id: String,
    val contratanteId: String,
    val perfilPrestadorId: String,
    val puntuacion: Int,
    val fechaCreacion: String,
    val comentario: String? = null
)
