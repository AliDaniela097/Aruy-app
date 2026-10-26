package ec.edu.puce.aruy.domain.model

/**
 * Datos públicos de un prestador, tal como los necesita el catálogo.
 * `id` es el id del PERFIL (perfiles_prestadores): es el que trae `servicios.prestador_id`.
 */
data class PrestadorPublico(
    val id: String,
    val usuarioId: String,
    val nombre: String,
    val telefono: String?,
    val fotoPerfil: String?,
    val verificado: Boolean,
    val oficio: String,
    val descripcion: String?,
    val zona: String,
    val aniosExperiencia: Int?,
    val disponible: Boolean,
    val promedio: Double?,
    val totalCalificaciones: Int
)

/** Un servicio con los datos de quien lo ofrece. `prestador` es null si el backend aún no los entrega. */
data class ServicioConPrestador(
    val servicio: Servicio,
    val prestador: PrestadorPublico?
) {
    val id: String get() = servicio.id
    val nombrePrestador: String get() = prestador?.nombre ?: "Prestador"
}

data class Opinion(
    val id: String,
    val puntuacion: Int,
    val comentario: String?,
    val fecha: String,
    val autor: String
)

data class ResumenOpiniones(
    val promedio: Double?,
    val total: Int,
    val opiniones: List<Opinion>
)
