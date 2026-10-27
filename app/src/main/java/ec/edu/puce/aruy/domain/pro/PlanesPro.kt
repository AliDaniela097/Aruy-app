package ec.edu.puce.aruy.domain.pro

import ec.edu.puce.aruy.domain.model.EstadoSuscripcion
import ec.edu.puce.aruy.domain.model.Suscripcion
import ec.edu.puce.aruy.domain.model.TipoPlan
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Los dos planes que acepta el backend (`PLANES` en routes/suscripciones.js) y reglas de la pantalla Aruy Pro. */
object PlanesPro {

    data class Plan(val tipo: TipoPlan, val titulo: String, val dias: Int, val descripcion: String)

    val PLANES = listOf(
        Plan(TipoPlan.ARUY_PRO_SEMANAL, "Aruy Pro semanal", 7, "Siete días para probarlo."),
        Plan(TipoPlan.ARUY_PRO_MENSUAL, "Aruy Pro mensual", 30, "Treinta días seguidos.")
    )

    fun plan(tipo: TipoPlan): Plan? = PLANES.firstOrNull { it.tipo == tipo }

    fun etiquetaEstado(estado: EstadoSuscripcion): String = when (estado) {
        EstadoSuscripcion.ACTIVA -> "Activa"
        EstadoSuscripcion.PENDIENTE -> "Pendiente"
        EstadoSuscripcion.CANCELADA -> "Cancelada"
        EstadoSuscripcion.VENCIDA -> "Vencida"
    }

    /** El servidor solo admite una suscripción PENDIENTE o ACTIVA por prestador; el historial no cuenta. */
    fun puedeContratar(actual: Suscripcion?): Boolean =
        actual == null || actual.estado == EstadoSuscripcion.CANCELADA || actual.estado == EstadoSuscripcion.VENCIDA

    fun puedeCancelar(actual: Suscripcion?): Boolean =
        actual != null && (actual.estado == EstadoSuscripcion.ACTIVA || actual.estado == EstadoSuscripcion.PENDIENTE)

    private val FORMATO = DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-EC"))

    /** "2026-10-25T12:00:00.000Z" -> "25 de octubre de 2026"; null si la fecha no se puede leer. */
    fun fecha(iso: String?, zona: ZoneId = ZoneId.systemDefault()): String? {
        if (iso.isNullOrBlank()) return null
        return runCatching { FORMATO.format(Instant.parse(iso).atZone(zona)) }.getOrNull()
    }
}
