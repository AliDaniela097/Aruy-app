package ec.edu.puce.aruy.data.mapper

import ec.edu.puce.aruy.data.remote.dto.ServicioDto
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.Servicio

fun ServicioDto.toDomain(): Servicio {
    return Servicio(
        id = id,
        prestadorId = prestadorId,
        titulo = titulo,
        descripcion = descripcion,
        categoria = runCatching { CategoriaServicio.valueOf(categoria) }
            .getOrDefault(CategoriaServicio.OTROS),
        precioReferencial = precioReferencial,
        disponible = disponible
    )
}