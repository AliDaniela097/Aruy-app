package ec.edu.puce.aruy.data.sesion

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import ec.edu.puce.aruy.domain.model.EstadoVerificacion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Sesion
import ec.edu.puce.aruy.domain.model.Usuario
import kotlinx.coroutines.flow.first

private val Context.almacenSesion: DataStore<Preferences> by preferencesDataStore(name = "sesion")

/** Dónde se guarda la sesión. Es una interfaz para poder probar el [SesionManager] sin Android. */
interface AlmacenSesion {
    suspend fun leer(): Sesion?
    suspend fun guardar(sesion: Sesion)
    suspend fun borrar()
}

/** Guarda la sesión en DataStore. El token va cifrado con el Keystore; los datos del usuario no son secretos. */
class SesionStore(
    private val contexto: Context,
    private val cifrador: CifradorTexto = CifradorKeystore()
) : AlmacenSesion {

    private val gson = Gson()
    private val claveToken = stringPreferencesKey("token_cifrado")
    private val claveUsuario = stringPreferencesKey("usuario")
    private val clavePerfil = stringPreferencesKey("perfil_prestador_id")

    private data class UsuarioGuardado(
        val id: String,
        val nombre: String,
        val telefono: String,
        val rol: String,
        val estadoVerificacion: String,
        val fotoPerfil: String?
    )

    override suspend fun leer(): Sesion? {
        val p = contexto.almacenSesion.data.first()
        val token = p[claveToken]?.let { cifrador.descifrar(it) } ?: return null
        val u = runCatching { gson.fromJson(p[claveUsuario], UsuarioGuardado::class.java) }.getOrNull() ?: return null
        return Sesion(
            token = token,
            usuario = Usuario(
                id = u.id,
                nombre = u.nombre,
                telefono = u.telefono,
                rol = runCatching { RolUsuario.valueOf(u.rol) }.getOrDefault(RolUsuario.CONTRATANTE),
                estadoVerificacion = runCatching { EstadoVerificacion.valueOf(u.estadoVerificacion) }
                    .getOrDefault(EstadoVerificacion.NO_SOLICITADA),
                fotoPerfil = u.fotoPerfil
            ),
            perfilPrestadorId = p[clavePerfil]
        )
    }

    override suspend fun guardar(sesion: Sesion) {
        val u = sesion.usuario
        val json = gson.toJson(
            UsuarioGuardado(u.id, u.nombre, u.telefono, u.rol.name, u.estadoVerificacion.name, u.fotoPerfil)
        )
        contexto.almacenSesion.edit {
            it[claveToken] = cifrador.cifrar(sesion.token)
            it[claveUsuario] = json
            if (sesion.perfilPrestadorId != null) it[clavePerfil] = sesion.perfilPrestadorId else it.remove(clavePerfil)
        }
    }

    override suspend fun borrar() {
        contexto.almacenSesion.edit { it.clear() }
    }
}
