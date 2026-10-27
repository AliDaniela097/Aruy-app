package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.model.EstadoSuscripcion
import ec.edu.puce.aruy.domain.model.Suscripcion
import ec.edu.puce.aruy.domain.model.TipoPlan
import ec.edu.puce.aruy.domain.pro.PlanesPro
import ec.edu.puce.aruy.domain.validation.ValidacionesPerfil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class PerfilYProTest {

    private fun suscripcion(estado: EstadoSuscripcion) = Suscripcion(
        id = "x", prestadorId = "p", tipoPlan = TipoPlan.ARUY_PRO_MENSUAL, estado = estado, fechaInicio = "2026-10-25T12:00:00.000Z"
    )

    @Test
    fun solo_hay_dos_planes_de_7_y_30_dias() {
        assertEquals(listOf(TipoPlan.ARUY_PRO_SEMANAL, TipoPlan.ARUY_PRO_MENSUAL), PlanesPro.PLANES.map { it.tipo })
        assertEquals(7, PlanesPro.plan(TipoPlan.ARUY_PRO_SEMANAL)!!.dias)
        assertEquals(30, PlanesPro.plan(TipoPlan.ARUY_PRO_MENSUAL)!!.dias)
        assertNull(PlanesPro.plan(TipoPlan.GRATUITO))
    }

    @Test
    fun solo_se_puede_contratar_sin_una_vigente() {
        assertTrue(PlanesPro.puedeContratar(null))
        assertTrue(PlanesPro.puedeContratar(suscripcion(EstadoSuscripcion.CANCELADA)))
        assertTrue(PlanesPro.puedeContratar(suscripcion(EstadoSuscripcion.VENCIDA)))
        assertFalse(PlanesPro.puedeContratar(suscripcion(EstadoSuscripcion.ACTIVA)))
        assertFalse(PlanesPro.puedeContratar(suscripcion(EstadoSuscripcion.PENDIENTE)))
    }

    @Test
    fun solo_se_cancela_una_activa_o_pendiente() {
        assertTrue(PlanesPro.puedeCancelar(suscripcion(EstadoSuscripcion.ACTIVA)))
        assertTrue(PlanesPro.puedeCancelar(suscripcion(EstadoSuscripcion.PENDIENTE)))
        assertFalse(PlanesPro.puedeCancelar(suscripcion(EstadoSuscripcion.VENCIDA)))
        assertFalse(PlanesPro.puedeCancelar(null))
    }

    @Test
    fun formatea_las_fechas_en_espanol() {
        val utc = ZoneId.of("UTC")
        assertEquals("25 de octubre de 2026", PlanesPro.fecha("2026-10-25T12:00:00.000Z", utc))
        assertEquals("1 de noviembre de 2026", PlanesPro.fecha("2026-11-01T12:00:00.000Z", utc))
        assertNull(PlanesPro.fecha(null))
        assertNull(PlanesPro.fecha("no es una fecha"))
    }

    @Test
    fun valida_oficio_y_zona() {
        assertNull(ValidacionesPerfil.oficio("Plomero"))
        assertNotNull(ValidacionesPerfil.oficio("  "))
        assertNotNull(ValidacionesPerfil.zona(""))
        assertNotNull(ValidacionesPerfil.zona("x".repeat(101)))
    }

    @Test
    fun los_anios_de_experiencia_son_opcionales_y_de_0_a_80() {
        assertEquals(ValidacionesPerfil.Anios(null, null), ValidacionesPerfil.anios(" "))
        assertEquals(8, ValidacionesPerfil.anios("8").valor)
        assertEquals(0, ValidacionesPerfil.anios("0").valor)
        listOf("81", "-1", "ocho", "2,5").forEach { assertNotNull(ValidacionesPerfil.anios(it).error) }
    }

    @Test
    fun la_baja_se_confirma_escribiendo_la_palabra() {
        assertTrue(ValidacionesPerfil.confirmaBaja("baja"))
        assertTrue(ValidacionesPerfil.confirmaBaja("  BAJA "))
        assertFalse(ValidacionesPerfil.confirmaBaja("ELIMINAR"))
        assertFalse(ValidacionesPerfil.confirmaBaja(""))
    }
}
