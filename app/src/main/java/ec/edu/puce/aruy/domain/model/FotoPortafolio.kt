package ec.edu.puce.aruy.domain.model

data class FotoPortafolio(
    val id: String,
    val servicioId: String,
    /** Ruta guardada en Neon (fotos_portafolio.referencia_imagen), por ejemplo `servicio/uuid.webp`. */
    val referenciaImagen: String,
    /** Dirección pública en Supabase Storage para mostrar la foto. */
    val url: String? = null,
    val descripcion: String? = null,
    val etiquetasIA: List<String> = emptyList(),
    val orden: Int = 0
)
