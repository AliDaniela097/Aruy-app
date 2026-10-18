package ec.edu.puce.aruy.domain.model

data class FotoPortafolio(
    val id: String,
    val servicioId: String,
    val referenciaImagen: String,
    val descripcion: String? = null,
    val etiquetasIA: List<String> = emptyList(),
    val orden: Int = 0
)