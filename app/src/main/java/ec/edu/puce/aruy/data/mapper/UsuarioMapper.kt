package ec.edu.puce.aruy.data.mapper

import ec.edu.puce.aruy.data.remote.dto.UsuarioDto
import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.EstadoVerificacion

fun UsuarioDto.toDomain(): Usuario {
    return Usuario(
        id = id,
        nombre = nombre,
        telefono = telefono,
        rol = runCatching { RolUsuario.valueOf(rol) }
            .getOrDefault(RolUsuario.CONTRATANTE),
        estadoVerificacion = runCatching {
            EstadoVerificacion.valueOf(estadoVerificacion)
        }.getOrDefault(EstadoVerificacion.NO_SOLICITADA),
        fotoPerfil = fotoPerfil,
        activo = activo
    )
}