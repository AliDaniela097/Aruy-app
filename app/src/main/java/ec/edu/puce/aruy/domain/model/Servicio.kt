package ec.edu.puce.aruy.domain.model

data class Servicio(
    val id: String,
    val prestadorId: String,
    val titulo: String,
    val descripcion: String,
    val categoria: CategoriaServicio,
    val precioReferencial: Double? = null,
    val disponible: Boolean = true
)
