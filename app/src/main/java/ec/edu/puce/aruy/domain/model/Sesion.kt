package ec.edu.puce.aruy.domain.model

/** Lo que devuelve el login o el registro: el token JWT (8 horas), el usuario y, si es prestador, su perfil. */
data class Sesion(
    val token: String,
    val usuario: Usuario,
    val perfilPrestadorId: String? = null
)

/** Quién usa la app ahora mismo (`AR.session` del prototipo). */
sealed interface EstadoSesion {
    /** Todavía se está leyendo la sesión guardada en el teléfono. */
    data object Cargando : EstadoSesion

    data object SinSesion : EstadoSesion

    /** "Explorar sin cuenta": solo vive en memoria. */
    data object Invitado : EstadoSesion

    data class Autenticada(val sesion: Sesion) : EstadoSesion
}
