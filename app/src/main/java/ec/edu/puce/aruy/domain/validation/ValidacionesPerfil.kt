package ec.edu.puce.aruy.domain.validation

/** Validaciones de "Editar perfil" (límites del backend en routes/perfiles.js y usuarios.js). */
object ValidacionesPerfil {

    const val OFICIO_MAX = 100
    const val ZONA_MAX = 100
    const val DESCRIPCION_MAX = 500
    const val ANIOS_MAX = 80

    fun oficio(valor: String): String? = when {
        valor.isBlank() -> "Escribe tu oficio. Ejemplo: Plomero."
        valor.trim().length > OFICIO_MAX -> "Usa máximo $OFICIO_MAX letras."
        else -> null
    }

    fun zona(valor: String): String? = when {
        valor.isBlank() -> "Escribe dónde atiendes. Ejemplo: Norte de Quito."
        valor.trim().length > ZONA_MAX -> "Usa máximo $ZONA_MAX letras."
        else -> null
    }

    /** Los años de experiencia son opcionales: vacío está bien. */
    data class Anios(val valor: Int?, val error: String?)

    fun anios(texto: String): Anios {
        val limpio = texto.trim()
        if (limpio.isEmpty()) return Anios(null, null)
        val numero = limpio.toIntOrNull()
        return if (numero != null && numero in 0..ANIOS_MAX) Anios(numero, null)
        else Anios(null, "Escribe un número entre 0 y $ANIOS_MAX.")
    }

    /** La confirmación para darse de baja se escribe a mano para evitar toques por error. */
    const val PALABRA_BAJA = "BAJA"

    fun confirmaBaja(texto: String): Boolean = texto.trim().equals(PALABRA_BAJA, ignoreCase = true)
}
