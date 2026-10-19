package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.repository.UsuarioRepository
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.domain.model.Usuario
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ec.edu.puce.aruy.data.remote.util.ApiErrorHandler

class UsuarioViewModel(
    private val repository: UsuarioRepository
) : ViewModel() {

    private val _estadoUsuario =
        MutableStateFlow<EstadoApi<Usuario>>(EstadoApi.Inactivo)


    val estadoUsuario: StateFlow<EstadoApi<Usuario>> =
        _estadoUsuario.asStateFlow()

    fun obtenerUsuario(id: String) {
        viewModelScope.launch {

            _estadoUsuario.value = EstadoApi.Cargando

            try {
                val usuario = repository.obtenerUsuario(id)

                _estadoUsuario.value = EstadoApi.Exito(usuario)


            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _estadoUsuario.value = EstadoApi.Error(
                    ApiErrorHandler.obtenerMensaje(e)
                )
            }

        }
    }
}