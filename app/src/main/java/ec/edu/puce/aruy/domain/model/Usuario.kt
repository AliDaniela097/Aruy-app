package ec.edu.puce.aruy.domain.model

data class Usuario(
    val id: String,
    val nombre: String,
    val telefono: String,
    val rol: RolUsuario,
    val estadoVerificacion: EstadoVerificacion = EstadoVerificacion.NO_SOLICITADA,
    val fotoPerfil: String? = null,
    val activo: Boolean = true
)
