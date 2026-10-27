package ec.edu.puce.aruy.domain.catalogo

/** "hace 5 min", "hace 2 h", "hace 3 días": para el historial de contactos y los reportes. */
object TiempoRelativo {

    private const val MINUTO = 60_000L
    private const val HORA = 60 * MINUTO
    private const val DIA = 24 * HORA

    fun texto(fechaMs: Long, ahoraMs: Long = System.currentTimeMillis()): String {
        val diferencia = (ahoraMs - fechaMs).coerceAtLeast(0)
        return when {
            diferencia < MINUTO -> "hace un momento"
            diferencia < HORA -> "hace ${diferencia / MINUTO} min"
            diferencia < DIA -> "hace ${diferencia / HORA} h"
            diferencia < 2 * DIA -> "ayer"
            diferencia < 30 * DIA -> "hace ${diferencia / DIA} días"
            else -> "hace más de un mes"
        }
    }
}
