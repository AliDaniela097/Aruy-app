package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.validation.Validaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacionesTest {

    @Test
    fun nombre_exige_nombre_y_apellido() {
        assertNull(Validaciones.nombre("Lucía Cevallos"))
        assertNotNull(Validaciones.nombre(""))
        assertNotNull(Validaciones.nombre("Lucía"))
        assertNotNull(Validaciones.nombre("A B"))
        assertNotNull(Validaciones.nombre("x".repeat(61) + " apellido"))
    }

    @Test
    fun correo_valida_formato() {
        assertNull(Validaciones.correo("nombre@correo.com"))
        assertNull(Validaciones.correo("  nombre@correo.com  "))
        assertNotNull(Validaciones.correo("nombre@correo"))
        assertNotNull(Validaciones.correo("nombre correo.com"))
        assertNotNull(Validaciones.correo(""))
    }

    @Test
    fun celular_acepta_formato_ecuatoriano() {
        assertNull(Validaciones.celular("0998765432"))
        assertNull(Validaciones.celular("099 876-5432"))
        assertNull(Validaciones.celular("+593998765432"))
        assertNotNull(Validaciones.celular("0298765432"))
        assertNotNull(Validaciones.celular("099876543"))
        assertNotNull(Validaciones.celular(""))
    }

    @Test
    fun celular_se_normaliza_antes_de_enviarse() {
        assertEquals("0998765432", Validaciones.normalizarCelular("(099) 876-5432"))
    }

    @Test
    fun password_exige_8_caracteres_con_letras_y_numeros() {
        assertNull(Validaciones.password("Aruy2026x"))
        assertNotNull(Validaciones.password(""))
        assertNotNull(Validaciones.password("abc123"))
        assertNotNull(Validaciones.password("soloLetrasAqui"))
        assertNotNull(Validaciones.password("12345678"))
    }

    @Test
    fun password_no_supera_72_bytes_porque_bcrypt_los_ignora() {
        assertNotNull(Validaciones.password("a1".repeat(37)))
        assertNull(Validaciones.password("a1".repeat(36)))
    }

    @Test
    fun fuerza_del_password_sube_con_la_variedad() {
        assertEquals(0, Validaciones.fuerzaPassword("abc"))
        assertEquals(2, Validaciones.fuerzaPassword("abcd1234"))
        assertEquals(4, Validaciones.fuerzaPassword("Abcd1234!xyz"))
    }
}
