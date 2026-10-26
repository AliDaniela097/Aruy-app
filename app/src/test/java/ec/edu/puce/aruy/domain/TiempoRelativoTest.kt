package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.catalogo.TiempoRelativo
import org.junit.Assert.assertEquals
import org.junit.Test

class TiempoRelativoTest {

    private val ahora = 1_800_000_000_000L
    private val minuto = 60_000L
    private val hora = 60 * minuto
    private val dia = 24 * hora

    @Test
    fun describe_el_tiempo_transcurrido() {
        assertEquals("hace un momento", TiempoRelativo.texto(ahora - 10_000, ahora))
        assertEquals("hace 5 min", TiempoRelativo.texto(ahora - 5 * minuto, ahora))
        assertEquals("hace 3 h", TiempoRelativo.texto(ahora - 3 * hora, ahora))
        assertEquals("ayer", TiempoRelativo.texto(ahora - 30 * hora, ahora))
        assertEquals("hace 4 días", TiempoRelativo.texto(ahora - 4 * dia, ahora))
        assertEquals("hace más de un mes", TiempoRelativo.texto(ahora - 40 * dia, ahora))
    }

    @Test
    fun una_fecha_futura_no_da_numeros_negativos() {
        assertEquals("hace un momento", TiempoRelativo.texto(ahora + hora, ahora))
    }
}
