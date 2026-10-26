package ec.edu.puce.aruy.data.mapper

import ec.edu.puce.aruy.data.remote.dto.CalificacionesDto
import ec.edu.puce.aruy.data.remote.dto.FotoDto
import ec.edu.puce.aruy.data.remote.dto.PerfilPublicoDto
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.model.Opinion
import ec.edu.puce.aruy.domain.model.PrestadorPublico
import ec.edu.puce.aruy.domain.model.ResumenOpiniones

fun PerfilPublicoDto.toDomain(): PrestadorPublico = PrestadorPublico(
    id = id,
    usuarioId = usuarioId.orEmpty(),
    nombre = nombre?.takeIf { it.isNotBlank() } ?: "Prestador",
    telefono = telefono?.takeIf { it.isNotBlank() },
    fotoPerfil = fotoPerfil,
    verificado = estadoVerificacion == "VERIFICADA",
    oficio = oficioPrincipal.orEmpty(),
    descripcion = descripcionProfesional,
    zona = zonaCobertura.orEmpty(),
    aniosExperiencia = aniosExperiencia,
    disponible = disponible ?: true,
    promedio = calificacionPromedio,
    totalCalificaciones = totalCalificaciones ?: 0
)

fun FotoDto.toDomain(servicioId: String): FotoPortafolio = FotoPortafolio(
    id = id,
    servicioId = servicioId,
    referenciaImagen = referenciaImagen,
    url = url,
    descripcion = descripcion,
    etiquetasIA = etiquetas.orEmpty(),
    orden = orden ?: 0
)

fun CalificacionesDto.toDomain(): ResumenOpiniones = ResumenOpiniones(
    promedio = promedio,
    total = total ?: calificaciones.orEmpty().size,
    opiniones = calificaciones.orEmpty().map {
        Opinion(
            id = it.id,
            puntuacion = it.puntuacion,
            comentario = it.comentario?.takeIf { c -> c.isNotBlank() },
            fecha = it.fechaCreacion.orEmpty(),
            autor = it.contratante?.takeIf { c -> c.isNotBlank() } ?: "Persona de Aruy"
        )
    }
)
