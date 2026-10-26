package ec.edu.puce.aruy.domain.catalogo

/** Reglas para armar el enlace de WhatsApp de un prestador (Ecuador, código 593). */
object Contacto {

    private val REGEX_NO_NUMERICO = Regex("[^0-9+]")

    /**
     * Número en formato internacional sin "+" (el que pide `wa.me`), o null si no se reconoce.
     * "0998765432" -> "593998765432"; "+593998765432" -> "593998765432".
     */
    fun numeroWhatsApp(telefono: String?): String? {
        val limpio = telefono?.replace(REGEX_NO_NUMERICO, "") ?: return null
        val numero = when {
            limpio.startsWith("+") -> limpio.drop(1)
            limpio.startsWith("593") -> limpio
            limpio.startsWith("09") && limpio.length == 10 -> "593" + limpio.drop(1)
            else -> return null
        }
        return numero.takeIf { it.length in 11..15 && it.all(Char::isDigit) }
    }

    fun mensajePorDefecto(nombrePrestador: String, tituloServicio: String?): String {
        val que = if (tituloServicio != null) "tu servicio «$tituloServicio»" else "tu perfil"
        return "Hola $nombrePrestador, vi $que en Aruy y me gustaría saber si tienes disponibilidad. " +
            "¿Me ayudas con una cotización?"
    }
}

/** Motivos de reporte del prototipo (`flows.js`). */
enum class MotivoReporte(val titulo: String, val descripcion: String?) {
    FOTOS_AJENAS("Las fotos no son suyas", "Parecen sacadas de otro lugar o de otra persona."),
    CONTENIDO_INAPROPIADO("Contenido inapropiado", null),
    ESTAFA("Parece una estafa", "Pide pagos por adelantado o datos sensibles."),
    INFORMACION_FALSA("Información falsa", null),
    OTRO("Otro motivo", null)
}
