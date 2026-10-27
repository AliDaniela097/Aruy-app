package ec.edu.puce.aruy.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ec.edu.puce.aruy.ui.theme.ModoTema
import ec.edu.puce.aruy.ui.theme.TamanoTexto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.almacenPreferencias: DataStore<Preferences> by preferencesDataStore(name = "preferencias")

data class PreferenciasUi(
    val modoTema: ModoTema = ModoTema.SISTEMA,
    val tamanoTexto: TamanoTexto = TamanoTexto.NORMAL,
    val reducirAnimaciones: Boolean = false
)

/** Preferencias de Ajustes (tema, tamaño del texto, animaciones), guardadas en el teléfono. */
class PreferenciasStore(private val contexto: Context) {

    private val claveTema = stringPreferencesKey("tema")
    private val claveTexto = stringPreferencesKey("texto")
    private val claveMovimiento = booleanPreferencesKey("reducir_animaciones")

    val preferencias: Flow<PreferenciasUi> = contexto.almacenPreferencias.data.map { p ->
        PreferenciasUi(
            modoTema = p[claveTema].comoEnum(ModoTema.SISTEMA),
            tamanoTexto = p[claveTexto].comoEnum(TamanoTexto.NORMAL),
            reducirAnimaciones = p[claveMovimiento] ?: false
        )
    }

    suspend fun guardarTema(modo: ModoTema) {
        contexto.almacenPreferencias.edit { it[claveTema] = modo.name }
    }

    suspend fun guardarTamanoTexto(tamano: TamanoTexto) {
        contexto.almacenPreferencias.edit { it[claveTexto] = tamano.name }
    }

    suspend fun guardarReducirAnimaciones(reducir: Boolean) {
        contexto.almacenPreferencias.edit { it[claveMovimiento] = reducir }
    }
}

private inline fun <reified E : Enum<E>> String?.comoEnum(porDefecto: E): E =
    enumValues<E>().firstOrNull { it.name == this } ?: porDefecto
