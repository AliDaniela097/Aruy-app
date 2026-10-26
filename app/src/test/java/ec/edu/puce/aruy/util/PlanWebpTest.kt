package ec.edu.puce.aruy.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanWebpTest {

    @Test
    fun ajusta_sin_deformar_y_sin_agrandar() {
        assertEquals(PlanWebp.Dimensiones(1280, 960), PlanWebp.ajustar(4000, 3000, 1280))
        assertEquals(PlanWebp.Dimensiones(960, 1280), PlanWebp.ajustar(3000, 4000, 1280))
        assertEquals(PlanWebp.Dimensiones(800, 600), PlanWebp.ajustar(800, 600, 1280))
        assertEquals(PlanWebp.Dimensiones(1280, 1280), PlanWebp.ajustar(1280, 1280, 1280))
    }

    @Test
    fun el_muestreo_deja_el_lado_largo_en_el_objetivo_o_mas() {
        assertEquals(1, PlanWebp.muestreo(1000, 800, 1280))
        assertEquals(2, PlanWebp.muestreo(4000, 3000, 1280))
        assertEquals(8, PlanWebp.muestreo(12000, 9000, 1280))
    }

    @Test
    fun usa_calidad_80_y_1280_cuando_la_foto_cabe() {
        val elegida = PlanWebp.elegir { _, _ -> ByteArray(100 * 1024) }
        assertNotNull(elegida)
        assertEquals(1280, elegida!!.lado)
        assertEquals(80, elegida.calidad)
    }

    @Test
    fun baja_la_calidad_primero_y_el_lado_despues() {
        val intentos = mutableListOf<Pair<Int, Int>>()
        val elegida = PlanWebp.elegir { lado, calidad ->
            intentos += lado to calidad
            // Solo cabe reduciendo a 1024 px con calidad 70 o menos
            if (lado == 1024 && calidad <= 70) ByteArray(300 * 1024) else ByteArray(PlanWebp.MAX_BYTES + 1)
        }
        assertEquals(1024, elegida!!.lado)
        assertEquals(70, elegida.calidad)
        assertEquals(1280 to 80, intentos.first())
        assertEquals(PlanWebp.CALIDADES.size + 3, intentos.size)
    }

    @Test
    fun el_limite_de_500_kb_es_inclusivo() {
        assertNotNull(PlanWebp.elegir { _, _ -> ByteArray(PlanWebp.MAX_BYTES) })
        assertNull(PlanWebp.elegir { _, _ -> ByteArray(PlanWebp.MAX_BYTES + 1) })
    }

    @Test
    fun reconoce_un_archivo_webp_por_su_cabecera() {
        val webp = "RIFF".toByteArray() + byteArrayOf(10, 0, 0, 0) + "WEBPVP8 ".toByteArray()
        assertTrue(PlanWebp.esWebp(webp))
        assertFalse(PlanWebp.esWebp(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 0, 0, 0, 0, 0, 0, 0, 0, 0)))
        assertFalse(PlanWebp.esWebp(ByteArray(4)))
    }
}
