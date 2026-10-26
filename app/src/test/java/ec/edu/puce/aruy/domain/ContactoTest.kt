package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.catalogo.Contacto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactoTest {

    @Test
    fun convierte_celulares_ecuatorianos_al_formato_de_whatsapp() {
        assertEquals("593998765432", Contacto.numeroWhatsApp("0998765432"))
        assertEquals("593998765432", Contacto.numeroWhatsApp("099 876-5432"))
        assertEquals("593998765432", Contacto.numeroWhatsApp("+593998765432"))
        assertEquals("593998765432", Contacto.numeroWhatsApp("593998765432"))
    }

    @Test
    fun rechaza_numeros_que_no_reconoce() {
        assertNull(Contacto.numeroWhatsApp(null))
        assertNull(Contacto.numeroWhatsApp(""))
        assertNull(Contacto.numeroWhatsApp("12345"))
        assertNull(Contacto.numeroWhatsApp("022345678"))
    }

    @Test
    fun el_mensaje_por_defecto_menciona_al_prestador_y_al_servicio() {
        val conServicio = Contacto.mensajePorDefecto("Luis", "Reparación de fugas")
        assertTrue(conServicio.startsWith("Hola Luis, vi tu servicio «Reparación de fugas» en Aruy"))
        assertTrue(Contacto.mensajePorDefecto("Luis", null).contains("tu perfil"))
    }
}
