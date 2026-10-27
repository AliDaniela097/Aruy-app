package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.mapper.toDomain
import ec.edu.puce.aruy.data.remote.api.CalificacionApi
import ec.edu.puce.aruy.data.remote.api.PerfilApi
import ec.edu.puce.aruy.data.remote.api.ServicioApi
import ec.edu.puce.aruy.data.remote.dto.CalificarRequest
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.model.PrestadorPublico
import ec.edu.puce.aruy.domain.model.ResumenOpiniones
import ec.edu.puce.aruy.domain.model.Servicio
import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.util.concurrent.ConcurrentHashMap

/**
 * Catálogo del contratante: junta los servicios con los datos públicos de sus prestadores.
 * Los datos del prestador vienen de `GET /perfiles` (pendiente en el backend); si ese endpoint
 * falla, el catálogo sigue funcionando y los servicios salen sin nombre ni calificación.
 */
class CatalogoRepository(
    private val servicioApi: ServicioApi,
    private val perfilApi: PerfilApi,
    private val calificacionApi: CalificacionApi
) {

    private val fotosEnMemoria = ConcurrentHashMap<String, List<FotoPortafolio>>()

    suspend fun cargarCatalogo(): List<ServicioConPrestador> = coroutineScope {
        val servicios = async { servicioApi.obtenerServicios().map { it.toDomain() } }
        val prestadores = async { intentar { perfilApi.listar().map { it.toDomain() } }.orEmpty() }
        val porId = prestadores.await().associateBy { it.id }
        servicios.await().map { ServicioConPrestador(it, porId[it.prestadorId]) }
    }

    /** null si el backend aún no tiene el endpoint público de perfiles. */
    suspend fun prestador(perfilId: String): PrestadorPublico? =
        intentar { perfilApi.obtener(perfilId).toDomain() }

    suspend fun servicio(id: String): Servicio = servicioApi.obtenerServicioPorId(id).toDomain()

    suspend fun serviciosDe(perfilId: String): List<Servicio> =
        servicioApi.obtenerServiciosPorPrestador(perfilId).map { it.toDomain() }

    /** Las fotos se guardan en memoria: el catálogo pide la portada de cada tarjeta al mostrarla. */
    suspend fun fotos(servicioId: String, forzar: Boolean = false): List<FotoPortafolio> {
        if (!forzar) fotosEnMemoria[servicioId]?.let { return it }
        val fotos = servicioApi.obtenerFotos(servicioId)
            .map { it.toDomain(servicioId) }
            .sortedBy { it.orden }
        fotosEnMemoria[servicioId] = fotos
        return fotos
    }

    suspend fun opiniones(perfilId: String): ResumenOpiniones =
        calificacionApi.listar(perfilId).toDomain()

    suspend fun calificar(perfilId: String, puntuacion: Int, comentario: String?) {
        calificacionApi.calificar(CalificarRequest(perfilId, puntuacion, comentario?.trim()?.takeIf { it.isNotEmpty() }))
    }

    private suspend fun <T> intentar(bloque: suspend () -> T): T? =
        try {
            bloque()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
