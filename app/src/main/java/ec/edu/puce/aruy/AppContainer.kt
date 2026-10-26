package ec.edu.puce.aruy

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ec.edu.puce.aruy.data.local.DatabaseProvider
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.repository.FavoritoRepository
import ec.edu.puce.aruy.data.repository.ServicioRepository
import ec.edu.puce.aruy.data.repository.UsuarioRepository
import ec.edu.puce.aruy.ui.viewmodel.FavoritoViewModel
import ec.edu.puce.aruy.ui.viewmodel.ServicioViewModelFactory
import ec.edu.puce.aruy.ui.viewmodel.UsuarioViewModelFactory

/**
 * Inyección de dependencias manual: crea una sola vez los repositorios y las fábricas de ViewModel.
 * Las demás fases agregan aquí sus repositorios (auth, fotos, perfil...).
 */
class AppContainer(contexto: Context) {

    private val database = DatabaseProvider.getDatabase(contexto.applicationContext)

    val favoritoRepository = FavoritoRepository(database.favoritoDao())
    val servicioRepository = ServicioRepository(RetrofitClient.servicioApi)
    val usuarioRepository = UsuarioRepository(RetrofitClient.usuarioApi)

    val favoritoViewModelFactory = object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FavoritoViewModel(favoritoRepository) as T
    }
    val usuarioViewModelFactory = UsuarioViewModelFactory(usuarioRepository)
    val servicioViewModelFactory = ServicioViewModelFactory(servicioRepository, usuarioRepository)
}
