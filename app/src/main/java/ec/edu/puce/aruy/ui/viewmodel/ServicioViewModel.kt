package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.remote.util.ApiErrorHandler
import ec.edu.puce.aruy.data.repository.ServicioRepository
import ec.edu.puce.aruy.data.repository.UsuarioRepository
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.domain.model.Servicio
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ServicioViewModel(
    private val repository: ServicioRepository,
    private val usuarioRepository: UsuarioRepository
) : ViewModel() {

    // prestadorId -> nombre del prestador, para mostrarlo en cada servicio
    private val _nombresPrestadores =
        MutableStateFlow<Map<String, String>>(emptyMap())

    val nombresPrestadores: StateFlow<Map<String, String>> =
        _nombresPrestadores.asStateFlow()

    private val _estadoServicios =
        MutableStateFlow<EstadoApi<List<Servicio>>>(
            EstadoApi.Inactivo
        )

    val estadoServicios: StateFlow<EstadoApi<List<Servicio>>> =
        _estadoServicios.asStateFlow()

    fun obtenerServicios() {
        viewModelScope.launch {

            _estadoServicios.value = EstadoApi.Cargando

            try {
                val servicios = repository.obtenerServicios()

                _estadoServicios.value = EstadoApi.Exito(servicios)

                cargarNombresPrestadores(servicios)

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _estadoServicios.value = EstadoApi.Error(
                    ApiErrorHandler.obtenerMensaje(e)
                )
            }
        }
    }

    private suspend fun cargarNombresPrestadores(servicios: List<Servicio>) {
        val pendientes = servicios
            .map { it.prestadorId }
            .distinct()
            .filter { it !in _nombresPrestadores.value }

        for (id in pendientes) {
            try {
                val usuario = usuarioRepository.obtenerUsuario(id)
                _nombresPrestadores.value =
                    _nombresPrestadores.value + (id to usuario.nombre)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Si falla un prestador, el servicio se sigue mostrando sin su nombre
            }
        }
    }

    fun obtenerServiciosPorPrestador(prestadorId: String) {
        viewModelScope.launch {

            _estadoServicios.value = EstadoApi.Cargando

            try {
                val servicios =
                    repository.obtenerServiciosPorPrestador(prestadorId)

                _estadoServicios.value = EstadoApi.Exito(servicios)

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                _estadoServicios.value = EstadoApi.Error(
                    ApiErrorHandler.obtenerMensaje(e)
                )
            }
        }
    }
}