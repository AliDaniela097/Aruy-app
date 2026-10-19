package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.mapper.toDomain
import ec.edu.puce.aruy.data.remote.api.ServicioApi
import ec.edu.puce.aruy.domain.model.Servicio

class ServicioRepository(
    private val servicioApi: ServicioApi
) {

    suspend fun obtenerServicios(): List<Servicio> {
        return servicioApi.obtenerServicios().map { dto ->
            dto.toDomain()
        }
    }

    suspend fun obtenerServicioPorId(id: String): Servicio {
        val servicioDto = servicioApi.obtenerServicioPorId(id)
        return servicioDto.toDomain()
    }

    suspend fun obtenerServiciosPorPrestador(
        prestadorId: String
    ): List<Servicio> {
        return servicioApi
            .obtenerServiciosPorPrestador(prestadorId)
            .map { dto ->
                dto.toDomain()
            }
    }
}