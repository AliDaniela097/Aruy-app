package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.local.entity.FavoritoEntity
import ec.edu.puce.aruy.data.repository.FavoritoRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FavoritoViewModel(
    private val repository: FavoritoRepository
) : ViewModel() {

    val favoritos: StateFlow<List<FavoritoEntity>> =
        repository.obtenerFavoritos().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun guardarFavorito(servicioId: String) {
        viewModelScope.launch {
            repository.guardarFavorito(servicioId)
        }
    }

    fun eliminarFavorito(servicioId: String) {
        viewModelScope.launch {
            repository.eliminarFavorito(servicioId)
        }
    }
}