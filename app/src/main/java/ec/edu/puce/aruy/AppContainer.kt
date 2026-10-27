package ec.edu.puce.aruy

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import ec.edu.puce.aruy.data.local.DatabaseProvider
import ec.edu.puce.aruy.data.local.PreferenciasStore
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.repository.ActividadLocalRepository
import ec.edu.puce.aruy.data.repository.AuthRepository
import ec.edu.puce.aruy.data.repository.CatalogoRepository
import ec.edu.puce.aruy.data.repository.FavoritoRepository
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.data.sesion.SesionStore
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel
import ec.edu.puce.aruy.ui.viewmodel.AuthViewModel
import ec.edu.puce.aruy.ui.viewmodel.BuscarViewModel
import ec.edu.puce.aruy.ui.viewmodel.CatalogoViewModel
import ec.edu.puce.aruy.ui.viewmodel.fabricaVm
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

    val authRepository = AuthRepository(cliente.authApi, sesionManager)
    val catalogoRepository = CatalogoRepository(cliente.servicioApi, cliente.perfilApi, cliente.calificacionApi)
    val favoritoRepository = FavoritoRepository(database.favoritoDao())
    val actividadLocalRepository = ActividadLocalRepository(
        database.bloqueadoDao(), database.contactoDao(), database.reporteDao(), database.vistoDao()
    )

    val authViewModelFactory = AuthViewModel.fabrica(authRepository, sesionManager)
    val catalogoViewModelFactory: ViewModelProvider.Factory =
        fabricaVm { CatalogoViewModel(catalogoRepository, actividadLocalRepository, sesionManager) }
    val buscarViewModelFactory: ViewModelProvider.Factory = fabricaVm { BuscarViewModel() }
    val actividadLocalViewModelFactory: ViewModelProvider.Factory =
        fabricaVm { ActividadLocalViewModel(favoritoRepository, actividadLocalRepository, sesionManager) }

    init {
        // Lee la sesión guardada; hasta entonces el estado es "Cargando" y se ve el splash
        alcance.launch { sesionManager.cargar() }
    }
}
