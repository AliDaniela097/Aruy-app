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
        rol = RolUsuario.valueOf(rol),
        estadoVerificacion =
        EstadoVerificacion.valueOf(estadoVerificacion),
        fotoPerfil = fotoPerfil,
        activo = activo
    )
}