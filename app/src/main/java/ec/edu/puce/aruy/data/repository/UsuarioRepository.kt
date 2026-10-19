package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.mapper.toDomain
import ec.edu.puce.aruy.data.remote.api.UsuarioApi
import ec.edu.puce.aruy.domain.model.Usuario

class UsuarioRepository(
    private val usuarioApi: UsuarioApi
) {

    suspend fun obtenerUsuario(id: String): Usuario {
        val usuarioDto = usuarioApi.obtenerUsuario(id)
        return usuarioDto.toDomain()
    }
}