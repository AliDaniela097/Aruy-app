
package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ec.edu.puce.aruy.data.repository.ServicioRepository
import ec.edu.puce.aruy.data.repository.UsuarioRepository

class ServicioViewModelFactory(
    private val repository: ServicioRepository,
    private val usuarioRepository: UsuarioRepository
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {

        if (modelClass.isAssignableFrom(ServicioViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ServicioViewModel(repository, usuarioRepository) as T
        }

        throw IllegalArgumentException(
            "ViewModel desconocido: ${modelClass.name}"
        )
    }
}
