package ec.edu.puce.aruy.domain.validation

import ec.edu.puce.aruy.domain.model.CategoriaServicio

/** Validaciones del formulario de servicio, portadas de `editor.js` del prototipo. */
object ValidacionesServicio {

    const val TITULO_MAX = 60
    const val DESCRIPCION_MAX = 500
    const val PRECIO_MAX = 10_000.0

    private fun letras(texto: String) = texto.count { it.isLetter() }

    fun titulo(valor: String): String? =
        if (letras(valor) >= 4) null else "Escribe un título de al menos 4 letras."

    fun descripcion(valor: String): String? =
        if (letras(valor) >= 20) null else "Cuenta un poco más: escribe al menos 20 letras."

    fun categoria(valor: CategoriaServicio?): String? =
        if (valor != null) null else "Elige la categoría del servicio."

    /** El precio es opcional; si se escribe debe ser mayor que 0 y menor que 10 000. */
    data class Precio(val valor: Double?, val error: String?)

    fun precio(texto: String): Precio {
        val limpio = texto.trim().replace(',', '.')
        if (limpio.isEmpty()) return Precio(null, null)
        val numero = limpio.toDoubleOrNull()
        return if (numero != null && numero > 0 && numero < PRECIO_MAX) Precio(numero, null)
        else Precio(null, "Escribe un precio válido. Ejemplo: 15 o 12,50.")
    }
}
