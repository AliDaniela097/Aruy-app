package ec.edu.puce.aruy.domain.validation

/**
 * Validaciones de formularios, portadas de `js/core/util.js` del prototipo.
 * Cada función devuelve el mensaje de error, o null si el valor es válido.
 */
object Validaciones {

    const val NOMBRE_MAX = 60
    const val PASSWORD_MIN = 8
    const val PASSWORD_MAX_BYTES = 72 // bcrypt solo usa los primeros 72 bytes

    private val REGEX_CORREO = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]{2,}$")
    private val REGEX_CELULAR_LOCAL = Regex("^09\\d{8}$")
    private val REGEX_CELULAR_INTERNACIONAL = Regex("^\\+5939\\d{8}$")
    private val REGEX_SEPARADORES = Regex("[\\s\\-().]")
    private val REGEX_ESPACIOS = Regex("\\s+")

    fun nombre(valor: String): String? {
        val limpio = valor.trim()
        return when {
            limpio.isEmpty() -> "Escribe tu nombre completo."
            limpio.length > NOMBRE_MAX -> "El nombre es muy largo. Usa máximo $NOMBRE_MAX letras."
            limpio.length < 5 || limpio.split(REGEX_ESPACIOS).size < 2 ->
                "Escribe tu nombre y tu apellido. Ejemplo: Lucía Cevallos."
            else -> null
        }
    }

    fun correo(valor: String): String? =
        if (REGEX_CORREO.matches(valor.trim())) null
        else "Este correo no parece válido. Ejemplo: nombre@correo.com."

    /** Quita espacios, guiones, paréntesis y puntos: "099 876-5432" -> "0998765432". */
    fun normalizarCelular(valor: String): String = valor.replace(REGEX_SEPARADORES, "")

    fun celular(valor: String): String? {
        val limpio = normalizarCelular(valor)
        return if (REGEX_CELULAR_LOCAL.matches(limpio) || REGEX_CELULAR_INTERNACIONAL.matches(limpio)) null
        else "Este número no es válido. Escríbelo así: 0998765432 o +593998765432."
    }

    fun password(valor: String): String? = when {
        valor.isEmpty() -> "Escribe tu contraseña."
        valor.length < PASSWORD_MIN -> "Usa al menos $PASSWORD_MIN caracteres."
        valor.toByteArray(Charsets.UTF_8).size > PASSWORD_MAX_BYTES -> "La contraseña es demasiado larga."
        !valor.any { it.isLetter() } || !valor.any { it.isDigit() } -> "Combina letras y números."
        else -> null
    }

    /** Puntaje 0..4 para el medidor de contraseña. */
    fun fuerzaPassword(valor: String): Int {
        var puntos = 0
        if (valor.length >= PASSWORD_MIN) puntos++
        if (valor.any { it.isLetter() } && valor.any { it.isDigit() }) puntos++
        if (valor.any { it.isUpperCase() } && valor.any { it.isLowerCase() }) puntos++
        if (valor.any { !it.isLetterOrDigit() } || valor.length >= 12) puntos++
        return puntos
    }

    fun textoFuerzaPassword(puntaje: Int): String = when (puntaje) {
        0 -> "Combina letras y números."
        1 -> "Débil: agrega más caracteres."
        2 -> "Regular: agrega mayúsculas o un símbolo."
        3 -> "Buena contraseña."
        else -> "Muy buena contraseña."
    }
}
