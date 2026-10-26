package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.local.entity.BloqueadoEntity
import ec.edu.puce.aruy.data.local.entity.ContactoEntity
import ec.edu.puce.aruy.data.local.entity.ReporteEntity
import ec.edu.puce.aruy.data.repository.ActividadLocalRepository
import ec.edu.puce.aruy.data.repository.FavoritoRepository
import ec.edu.puce.aruy.data.sesion.SesionManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Lo que la persona guarda en este teléfono: favoritos, bloqueados, contactos, reportes y vistos.
 * Todo va por cuenta (usuarioId); un invitado no guarda nada.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ActividadLocalViewModel(
    private val favoritos: FavoritoRepository,
    private val local: ActividadLocalRepository,
    sesion: SesionManager
) : ViewModel() {

    private val usuarioId = sesion.usuarioIdFlow()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private fun <T> porUsuario(inicial: T, origen: (String) -> Flow<T>): StateFlow<T> =
        usuarioId.flatMapLatest { id -> if (id == null) flowOf(inicial) else origen(id) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), inicial)

    val favoritosIds: StateFlow<List<String>> =
        porUsuario(emptyList()) { id -> favoritos.obtenerFavoritos(id).map { l -> l.map { it.servicioId } } }

    val contactos: StateFlow<List<ContactoEntity>> = porUsuario(emptyList()) { local.contactos(it) }
    val bloqueados: StateFlow<List<BloqueadoEntity>> = porUsuario(emptyList()) { local.bloqueados(it) }
    val reportes: StateFlow<List<ReporteEntity>> = porUsuario(emptyList()) { local.reportes(it) }
    val vistosIds: StateFlow<List<String>> =
        porUsuario(emptyList()) { id -> local.vistos(id).map { l -> l.map { it.servicioId } } }

    fun esFavorito(servicioId: String): Flow<Boolean> =
        usuarioId.flatMapLatest { id -> if (id == null) flowOf(false) else favoritos.esFavorito(id, servicioId) }

    /** Devuelve el estado nuevo vía [alTerminar] (true = quedó como favorito). */
    fun alternarFavorito(servicioId: String, esFavoritoAhora: Boolean, alTerminar: (Boolean) -> Unit = {}) {
        val id = usuarioId.value ?: return
        viewModelScope.launch {
            if (esFavoritoAhora) favoritos.eliminarFavorito(id, servicioId) else favoritos.guardarFavorito(id, servicioId)
            alTerminar(!esFavoritoAhora)
        }
    }

    fun bloquear(prestadorId: String, nombre: String) {
        val id = usuarioId.value ?: return
        viewModelScope.launch { local.bloquear(id, prestadorId, nombre) }
    }

    fun desbloquear(prestadorId: String) {
        val id = usuarioId.value ?: return
        viewModelScope.launch { local.desbloquear(id, prestadorId) }
    }

    fun registrarContacto(prestadorId: String, servicioId: String?, nombre: String, mensaje: String) {
        val id = usuarioId.value ?: return
        viewModelScope.launch {
            local.registrarContacto(ContactoEntity(usuarioId = id, prestadorId = prestadorId, servicioId = servicioId, nombre = nombre, mensaje = mensaje))
        }
    }

    fun borrarHistorialDeContactos() {
        val id = usuarioId.value ?: return
        viewModelScope.launch { local.borrarHistorialDeContactos(id) }
    }

    fun reportar(tipo: String, objetivoId: String, objetivo: String, motivo: String, detalle: String?) {
        val id = usuarioId.value ?: return
        viewModelScope.launch {
            local.reportar(
                ReporteEntity(usuarioId = id, tipo = tipo, objetivoId = objetivoId, objetivo = objetivo, motivo = motivo, detalle = detalle?.trim()?.takeIf { it.isNotEmpty() })
            )
        }
    }

    fun registrarVisto(servicioId: String) {
        val id = usuarioId.value ?: return
        viewModelScope.launch { local.registrarVisto(id, servicioId) }
    }
}
