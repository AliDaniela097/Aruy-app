package ec.edu.puce.aruy.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Icon
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ec.edu.puce.aruy.AppContainer
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.screen.FavoritosScreen
import ec.edu.puce.aruy.ui.screen.ServiciosScreen
import ec.edu.puce.aruy.ui.screen.UsuarioScreen
import ec.edu.puce.aruy.ui.viewmodel.FavoritoViewModel
import ec.edu.puce.aruy.ui.viewmodel.ServicioViewModel
import ec.edu.puce.aruy.ui.viewmodel.UsuarioViewModel
import androidx.compose.runtime.collectAsState

/**
 * Estructura principal: Scaffold con barra inferior según el rol y un NavHost.
 * Las pantallas se van reemplazando por las reales en cada fase del plan.
 */
@Composable
fun AruyApp(contenedor: AppContainer, rol: RolNavegacion) {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val items = remember(rol) { itemsDeNavegacion(rol) }
    val snackbarHostState = remember { SnackbarHostState() }

    val favoritoViewModel: FavoritoViewModel = viewModel(factory = contenedor.favoritoViewModelFactory)
    val usuarioViewModel: UsuarioViewModel = viewModel(factory = contenedor.usuarioViewModelFactory)
    val servicioViewModel: ServicioViewModel = viewModel(factory = contenedor.servicioViewModelFactory)

    // Aún no hay inicio de sesión (fase 2): se muestra el prestador del primer servicio
    val estadoServicios by servicioViewModel.estadoServicios.collectAsState()
    val usuarioIdPerfil = (estadoServicios as? EstadoApi.Exito)?.datos?.firstOrNull()?.prestadorId

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (items.any { it.ruta == rutaActual }) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = rutaActual == item.ruta,
                            onClick = {
                                navController.navigate(item.ruta) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icono, contentDescription = null) },
                            label = { Text(item.titulo) }
                        )
                    }
                }
            }
        }
    ) { relleno ->
        Box(Modifier.padding(relleno)) {
            NavHost(navController = navController, startDestination = rutaInicial(rol)) {
                composable(Rutas.INICIO) { ServiciosScreen(servicioViewModel) }
                composable(Rutas.GUARDADOS) { FavoritosScreen(favoritoViewModel) }
                composable(Rutas.PERFIL) { UsuarioScreen(usuarioViewModel, usuarioIdPerfil) }
                composable(Rutas.BUSCAR) { PantallaEnConstruccion("Buscar") }
                composable(Rutas.REGISTRO) { PantallaEnConstruccion("Crear cuenta") }
                composable(Rutas.PANEL) { PantallaEnConstruccion("Panel") }
                composable(Rutas.MIS_SERVICIOS) { PantallaEnConstruccion("Mis servicios") }
                composable(Rutas.PORTAFOLIO) { PantallaEnConstruccion("Portafolio") }
            }
        }
    }
}

@Composable
private fun PantallaEnConstruccion(titulo: String) {
    EstadoVacio(
        titulo = titulo,
        texto = "Esta pantalla se construye en una de las próximas fases.",
        icono = Icons.Filled.Construction
    )
}
