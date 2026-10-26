package ec.edu.puce.aruy.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import kotlinx.coroutines.flow.first

private val Context.almacenBorradores: DataStore<Preferences> by preferencesDataStore(name = "borradores")

/** Lo escrito en el formulario de un servicio nuevo. Las fotos no se guardan en el borrador. */
data class BorradorServicio(
    val titulo: String = "",
    val descripcion: String = "",
    val categoria: String? = null,
    val precio: String = "",
    val disponible: Boolean = true
) {
    val vacio: Boolean get() = titulo.isBlank() && descripcion.isBlank() && categoria == null && precio.isBlank()
}

/** Borrador del servicio que se está escribiendo, uno por cuenta, guardado en el teléfono. */
class BorradorStore(private val contexto: Context) {

    private val gson = Gson()

    private fun clave(usuarioId: String) = stringPreferencesKey("servicio_$usuarioId")

    suspend fun leer(usuarioId: String): BorradorServicio? {
        val json = contexto.almacenBorradores.data.first()[clave(usuarioId)] ?: return null
        return runCatching { gson.fromJson(json, BorradorServicio::class.java) }.getOrNull()
    }

    /** Un borrador vacío se borra en vez de guardarse. */
    suspend fun guardar(usuarioId: String, borrador: BorradorServicio) {
        if (borrador.vacio) return borrar(usuarioId)
        contexto.almacenBorradores.edit { it[clave(usuarioId)] = gson.toJson(borrador) }
    }

    suspend fun borrar(usuarioId: String) {
        contexto.almacenBorradores.edit { it.remove(clave(usuarioId)) }
    }
}
