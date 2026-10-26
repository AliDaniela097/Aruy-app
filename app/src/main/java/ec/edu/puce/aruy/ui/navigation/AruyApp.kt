package ec.edu.puce.aruy.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ec.edu.puce.aruy.AppContainer
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.screen.FavoritosScreen
import ec.edu.puce.aruy.ui.screen.PerfilBasicoScreen
import ec.edu.puce.aruy.ui.screen.ServiciosScreen
import ec.edu.puce.aruy.ui.screen.auth.BienvenidaScreen
import ec.edu.puce.aruy.ui.screen.auth.LoginScreen
import ec.edu.puce.aruy.ui.screen.auth.RegistroScreen
import ec.edu.puce.aruy.ui.screen.auth.SplashScreen
import ec.edu.puce.aruy.ui.screen.sistema.AccesoDenegadoScreen
import ec.edu.puce.aruy.ui.viewmodel.AuthViewModel
import ec.edu.puce.aruy.ui.viewmodel.FavoritoViewModel
import ec.edu.puce.aruy.ui.viewmodel.ServicioViewModel

/** null mientras no hay una persona identificada (cargando o sin sesión). */
internal fun rolDe(estado: EstadoSesion): RolNavegacion? = when (estado) {
    is EstadoSesion.Autenticada ->
        if (estado.sesion.usuario.rol == RolUsuario.PRESTADOR) RolNavegacion.PRESTADOR else RolNavegacion.CONTRATANTE
    EstadoSesion.Invitado -> RolNavegacion.INVITADO
    EstadoSesion.Cargando, EstadoSesion.SinSesion -> null
}

private fun NavController.irLimpio(ruta: String) = navigate(ruta) {
    popUpTo(0) { inclusive = true }
    launchSingleTop = true
}

/**
 * Estructura principal: NavHost con las guardas de `router.js` y barra inferior según el rol.
 * Las pantallas pendientes muestran un aviso hasta que se construyan en las próximas fases.
 */
@Composable
fun AruyApp(contenedor: AppContainer) {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val snackbarHostState = remember { SnackbarHostState() }

    val estadoSesion by contenedor.sesionManager.estado.collectAsState()
    val rol = rolDe(estadoSesion)
    val items = remember(rol) { rol?.let(::itemsDeNavegacion).orEmpty() }

    val authViewModel: AuthViewModel = viewModel(factory = contenedor.authViewModelFactory)
    val favoritoViewModel: FavoritoViewModel = viewModel(factory = contenedor.favoritoViewModelFactory)
    val servicioViewModel: ServicioViewModel = viewModel(factory = contenedor.servicioViewModelFactory)

    // Reacciona a los cambios de sesión: entrar, cerrar sesión o vencimiento del token
    var sesionAnterior by remember { mutableStateOf(estadoSesion) }
    LaunchedEffect(estadoSesion) {
        val antes = sesionAnterior
        sesionAnterior = estadoSesion
        val ruta = navController.currentDestination?.route
        when {
            estadoSesion is EstadoSesion.Autenticada && ruta in Rutas.ENTRADA ->
                navController.irLimpio(rutaInicial(rolDe(estadoSesion)!!))
            estadoSesion is EstadoSesion.SinSesion &&
                (antes is EstadoSesion.Autenticada || antes is EstadoSesion.Invitado) ->
                navController.irLimpio(if (contenedor.sesionManager.aviso.value != null) Rutas.LOGIN else Rutas.BIENVENIDA)
        }
    }

    val estadoActualizado by rememberUpdatedState(estadoSesion)
    val irAlInicio: () -> Unit = { rol?.let { navController.irLimpio(rutaInicial(it)) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (rutaActual in Rutas.CON_BARRA && items.isNotEmpty()) {
                NavigationBar {
                    items.forEach { item ->
                        NavigationBarItem(
                            selected = rutaActual == item.ruta,
                            onClick = {
                                if (item.ruta == Rutas.REGISTRO) {
                                    // "Crear cuenta" del invitado abre el registro, no es una pestaña
                                    navController.navigate(Rutas.REGISTRO)
                                } else {
                                    navController.navigate(item.ruta) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
            NavHost(navController = navController, startDestination = Rutas.SPLASH) {

                composable(Rutas.SPLASH) {
                    SplashScreen(estado = estadoSesion) {
                        val actual = estadoActualizado
                        navController.irLimpio(
                            when (actual) {
                                is EstadoSesion.Autenticada -> rutaInicial(rolDe(actual)!!)
                                EstadoSesion.Invitado -> Rutas.INICIO
                                else -> Rutas.BIENVENIDA
                            }
                        )
                    }
                }
                composable(Rutas.BIENVENIDA) {
                    BienvenidaScreen(
                        onCrearCuenta = { navController.navigate(Rutas.REGISTRO) },
                        onIniciarSesion = { navController.navigate(Rutas.LOGIN) },
                        onExplorar = {
                            authViewModel.entrarComoInvitado()
                            navController.irLimpio(Rutas.INICIO)
                        }
                    )
                }
                composable(Rutas.LOGIN) {
                    LoginScreen(
                        viewModel = authViewModel,
                        onVolver = { if (!navController.popBackStack()) navController.irLimpio(Rutas.BIENVENIDA) },
                        onCrearCuenta = { navController.navigate(Rutas.REGISTRO) }
                    )
                }
                composable(Rutas.REGISTRO) {
                    RegistroScreen(
                        viewModel = authViewModel,
                        onVolver = { if (!navController.popBackStack()) navController.irLimpio(Rutas.BIENVENIDA) },
                        onIniciarSesion = { navController.navigate(Rutas.LOGIN) }
                    )
                }

                composable(Rutas.INICIO) {
                    Protegida(Rutas.INICIO, rol, navController, irAlInicio) { ServiciosScreen(servicioViewModel) }
                }
                composable(Rutas.BUSCAR) {
                    Protegida(Rutas.BUSCAR, rol, navController, irAlInicio) { PantallaEnConstruccion("Buscar") }
                }
                composable(Rutas.GUARDADOS) {
                    Protegida(Rutas.GUARDADOS, rol, navController, irAlInicio) { FavoritosScreen(favoritoViewModel) }
                }
                composable(Rutas.PERFIL) {
                    Protegida(Rutas.PERFIL, rol, navController, irAlInicio) {
                        (estadoSesion as? EstadoSesion.Autenticada)?.let { autenticada ->
                            PerfilBasicoScreen(autenticada.sesion.usuario, onCerrarSesion = authViewModel::cerrarSesion)
                        }
                    }
                }
                composable(Rutas.PANEL) {
                    Protegida(Rutas.PANEL, rol, navController, irAlInicio) { PantallaEnConstruccion("Panel") }
                }
                composable(Rutas.MIS_SERVICIOS) {
                    Protegida(Rutas.MIS_SERVICIOS, rol, navController, irAlInicio) { PantallaEnConstruccion("Mis servicios") }
                }
                composable(Rutas.PORTAFOLIO) {
                    Protegida(Rutas.PORTAFOLIO, rol, navController, irAlInicio) { PantallaEnConstruccion("Portafolio") }
                }
            }
        }
    }
}

/**
 * Guarda de ruta (`router.js`): sin sesión lleva al login; con un rol que no corresponde
 * muestra "acceso denegado".
 */
@Composable
private fun Protegida(
    ruta: String,
    rol: RolNavegacion?,
    navController: NavController,
    irAlInicio: () -> Unit,
    contenido: @Composable () -> Unit
) {
    when {
        rol == null -> LaunchedEffect(Unit) { navController.irLimpio(Rutas.LOGIN) }
        !rutaPermitida(ruta, rol) -> AccesoDenegadoScreen(rol, irAlInicio)
        else -> contenido()
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
