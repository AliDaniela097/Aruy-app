package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.remote.api.SuscripcionApi
import ec.edu.puce.aruy.data.remote.dto.SuscribirseRequest
import ec.edu.puce.aruy.data.remote.dto.SuscripcionDto
import ec.edu.puce.aruy.domain.model.EstadoSuscripcion
import ec.edu.puce.aruy.domain.model.Suscripcion
import ec.edu.puce.aruy.domain.model.TipoPlan
import retrofit2.HttpException

/** Aruy Pro (suscripciones simuladas del backend: no hay cobro real). */
class SuscripcionRepository(private val api: SuscripcionApi) {

    /** La suscripción más reciente, o null si nunca tuviste una (el servidor responde 404). */
    suspend fun miSuscripcion(): Suscripcion? = try {
        api.miSuscripcion().toDomain()
    } catch (e: HttpException) {
        if (e.code() == 404) null else throw e
    }

    suspend fun suscribirse(plan: TipoPlan, renovacionAutomatica: Boolean): Suscripcion {
        require(plan != TipoPlan.GRATUITO) { "El plan gratuito no se contrata" }
        return api.suscribirse(SuscribirseRequest(plan.name, renovacionAutomatica)).toDomain()
    }

    suspend fun cancelar(id: String): Suscripcion = api.cancelar(id).toDomain()

    private fun SuscripcionDto.toDomain() = Suscripcion(
        id = id,
        prestadorId = prestadorId.orEmpty(),
        tipoPlan = runCatching { TipoPlan.valueOf(tipoPlan) }.getOrDefault(TipoPlan.GRATUITO),
        estado = runCatching { EstadoSuscripcion.valueOf(estado) }.getOrDefault(EstadoSuscripcion.PENDIENTE),
        fechaInicio = fechaInicio.orEmpty(),
        fechaFin = fechaFin,
        renovacionAutomatica = renovacionAutomatica
    )
}
