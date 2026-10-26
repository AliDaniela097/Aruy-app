package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

/** Fábrica de un ViewModel que no necesita más que lo que ya tiene el [ec.edu.puce.aruy.AppContainer]. */
inline fun <reified T : ViewModel> fabricaVm(crossinline crear: () -> T): ViewModelProvider.Factory =
    viewModelFactory { initializer { crear() } }
