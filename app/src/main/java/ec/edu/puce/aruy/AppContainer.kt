package ec.edu.puce.aruy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ec.edu.puce.aruy.data.local.DatabaseProvider
import ec.edu.puce.aruy.data.local.PreferenciasStore
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.repository.AuthRepository
import ec.edu.puce.aruy.data.repository.FavoritoRepository
import ec.edu.puce.aruy.data.repository.ServicioRepository
import ec.edu.puce.aruy.data.repository.UsuarioRepository
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.data.sesion.SesionStore
import ec.edu.puce.aruy.ui.viewmodel.AuthViewModel
import ec.edu.puce.aruy.ui.viewmodel.FavoritoViewModel
import ec.edu.puce.aruy.ui.viewmodel.ServicioViewModelFactory
import ec.edu.puce.aruy.ui.viewmodel.UsuarioViewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Inyección de dependencias manual: crea una sola vez los repositorios y las fábricas de ViewModel.
 * Las demás fases agregan aquí sus repositorios (fotos, perfil...).
 */
class AppContainer(contexto: Context) {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database = DatabaseProvider.getDatabase(contexto.applicationContext)

    val preferencias = PreferenciasStore(contexto.applicationContext)

    val sesionManager = SesionManager(SesionStore(contexto.applicationContext), alcance)

    private val cliente = RetrofitClient(
        token = sesionManager::tokenActual,
        alExpirar = sesionManager::expirar
    )

    val favoritoRepository = FavoritoRepository(database.favoritoDao())
    val servicioRepository = ServicioRepository(cliente.servicioApi)
    val usuarioRepository = UsuarioRepository(cliente.usuarioApi)
    val authRepository = AuthRepository(cliente.authApi, sesionManager)

    val favoritoViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FavoritoViewModel(favoritoRepository) as T
    }
    val usuarioViewModelFactory = UsuarioViewModelFactory(usuarioRepository)
    val servicioViewModelFactory = ServicioViewModelFactory(servicioRepository, usuarioRepository)
    val authViewModelFactory = AuthViewModel.fabrica(authRepository, sesionManager)

    init {
        // Lee la sesión guardada; hasta entonces el estado es "Cargando" y se ve el splash
        alcance.launch { sesionManager.cargar() }
    }
}
