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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ec.edu.puce.aruy.AppContainer
import ec.edu.puce.aruy.domain.catalogo.FiltrosCatalogo
import ec.edu.puce.aruy.domain.catalogo.OrdenCatalogo
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.HojaCuentaRequerida
import ec.edu.puce.aruy.ui.screen.PerfilBasicoScreen
import ec.edu.puce.aruy.ui.screen.auth.BienvenidaScreen
import ec.edu.puce.aruy.ui.screen.auth.LoginScreen
import ec.edu.puce.aruy.ui.screen.auth.RegistroScreen
import ec.edu.puce.aruy.ui.screen.auth.SplashScreen
import ec.edu.puce.aruy.ui.screen.catalogo.BloqueadosScreen
import ec.edu.puce.aruy.ui.screen.catalogo.BuscarScreen
import ec.edu.puce.aruy.ui.screen.catalogo.DetalleServicioScreen
import ec.edu.puce.aruy.ui.screen.catalogo.GuardadosScreen
import ec.edu.puce.aruy.ui.screen.catalogo.InicioScreen
import ec.edu.puce.aruy.ui.screen.catalogo.MisReportesScreen
import ec.edu.puce.aruy.ui.screen.catalogo.NormasScreen
import ec.edu.puce.aruy.ui.screen.catalogo.PrestadorPublicoScreen
import ec.edu.puce.aruy.ui.screen.sistema.AccesoDenegadoScreen
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel
import ec.edu.puce.aruy.ui.viewmodel.AuthViewModel
import ec.edu.puce.aruy.ui.viewmodel.BuscarViewModel
import ec.edu.puce.aruy.ui.viewmodel.CatalogoViewModel
import ec.edu.puce.aruy.ui.viewmodel.DetalleViewModel
import ec.edu.puce.aruy.ui.viewmodel.PrestadorViewModel
import ec.edu.puce.aruy.ui.viewmodel.fabricaVm

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
 * Las pantallas del prestador muestran un aviso hasta que se construyan en las próximas fases.
 */
@Composable
fun AruyApp(contenedor: AppContainer) {
    val navController = rememberNavController()
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = Rutas.base(entradaActual?.destination?.route)
    val snackbarHostState = remember { SnackbarHostState() }

    val estadoSesion by contenedor.sesionManager.estado.collectAsState()
    val rol = rolDe(estadoSesion)
    val items = remember(rol) { rol?.let(::itemsDeNavegacion).orEmpty() }

    val authViewModel: AuthViewModel = viewModel(factory = contenedor.authViewModelFactory)
    val catalogo: CatalogoViewModel = viewModel(factory = contenedor.catalogoViewModelFactory)
    val buscar: BuscarViewModel = viewModel(factory = contenedor.buscarViewModelFactory)
    val actividad: ActividadLocalViewModel = viewModel(factory = contenedor.actividadLocalViewModelFactory)

    var mostrarHojaCuenta by remember { mutableStateOf(false) }

    // Reacciona a los cambios de sesión: entrar, cerrar sesión o vencimiento del token
    var sesionAnterior by remember { mutableStateOf(estadoSesion) }
    LaunchedEffect(estadoSesion) {
        val antes = sesionAnterior
        sesionAnterior = estadoSesion
        val ruta = Rutas.base(navController.currentDestination?.route)
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

    /** Un invitado no entra al detalle: se le ofrece crear su cuenta (`router.js`, `flows.js`). */
    val abrirServicio: (String) -> Unit = { id ->
        if (rol == RolNavegacion.INVITADO) mostrarHojaCuenta = true else navController.navigate(Rutas.servicio(id))
    }

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
                    Protegida(Rutas.INICIO, rol, navController, irAlInicio) {
                        InicioScreen(
                            catalogo = catalogo,
                            actividad = actividad,
                            nombre = (estadoSesion as? EstadoSesion.Autenticada)?.sesion?.usuario?.nombre,
                            invitado = rol == RolNavegacion.INVITADO,
                            onBuscar = { navController.navigate(Rutas.buscar(enfocar = true)) },
                            onCategoria = { navController.navigate(Rutas.buscar(it)) },
                            onVerTodos = { orden ->
                                buscar.actualizar {
                                    FiltrosCatalogo(soloDisponibles = orden == null, orden = orden ?: OrdenCatalogo.MEJOR_CALIFICACION)
                                }
                                navController.navigate(Rutas.buscar())
                            },
                            onServicio = abrirServicio,
                            onCrearCuenta = { navController.navigate(Rutas.REGISTRO) }
                        )
                    }
                }
                composable(
                    Rutas.PATRON_BUSCAR,
                    arguments = listOf(
                        navArgument("cat") { type = NavType.StringType; nullable = true; defaultValue = null },
                        navArgument("foco") { type = NavType.BoolType; defaultValue = false }
                    )
                ) { entrada ->
                    val categoria = entrada.arguments?.getString("cat")?.takeIf { it.isNotEmpty() }
                    LaunchedEffect(categoria) {
                        if (categoria != null) buscar.aplicarCategoria(runCatching { CategoriaServicio.valueOf(categoria) }.getOrNull())
                    }
                    Protegida(Rutas.BUSCAR, rol, navController, irAlInicio) {
                        BuscarScreen(
                            catalogo = catalogo,
                            buscar = buscar,
                            invitado = rol == RolNavegacion.INVITADO,
                            enfocar = entrada.arguments?.getBoolean("foco") ?: false,
                            onServicio = abrirServicio
                        )
                    }
                }
                composable(Rutas.GUARDADOS) {
                    Protegida(Rutas.GUARDADOS, rol, navController, irAlInicio) {
                        GuardadosScreen(
                            catalogo = catalogo,
                            actividad = actividad,
                            onServicio = abrirServicio,
                            onBuscar = { navController.navigate(Rutas.buscar()) }
                        )
                    }
                }
                composable(Rutas.PERFIL) {
                    Protegida(Rutas.PERFIL, rol, navController, irAlInicio) {
                        (estadoSesion as? EstadoSesion.Autenticada)?.let { autenticada ->
                            PerfilBasicoScreen(
                                usuario = autenticada.sesion.usuario,
                                onBloqueados = { navController.navigate(Rutas.BLOQUEADOS) },
                                onReportes = { navController.navigate(Rutas.REPORTES) },
                                onNormas = { navController.navigate(Rutas.NORMAS) },
                                onCerrarSesion = authViewModel::cerrarSesion
                            )
                        }
                    }
                }

                composable(Rutas.PATRON_SERVICIO) { entrada ->
                    val id = entrada.arguments?.getString("id").orEmpty()
                    Protegida(Rutas.SERVICIO, rol, navController, irAlInicio) {
                        val vm: DetalleViewModel = viewModel(
                            factory = fabricaVm { DetalleViewModel(id, catalogo.servicioPorId(id), contenedor.catalogoRepository) }
                        )
                        DetalleServicioScreen(
                            viewModel = vm,
                            actividad = actividad,
                            rol = rol ?: RolNavegacion.INVITADO,
                            onVolver = { navController.popBackStack() },
                            onPrestador = { navController.navigate(Rutas.prestador(it)) },
                            onCuentaRequerida = { mostrarHojaCuenta = true }
                        )
                    }
                }
                composable(Rutas.PATRON_PRESTADOR) { entrada ->
                    val id = entrada.arguments?.getString("id").orEmpty()
                    Protegida(Rutas.PRESTADOR_PUBLICO, rol, navController, irAlInicio) {
                        val vm: PrestadorViewModel = viewModel(
                            factory = fabricaVm {
                                PrestadorViewModel(id, catalogo.prestadorPorId(id), contenedor.catalogoRepository)
                            }
                        )
                        PrestadorPublicoScreen(
                            perfilId = id,
                            viewModel = vm,
                            actividad = actividad,
                            rol = rol ?: RolNavegacion.INVITADO,
                            cargarPortada = catalogo::portada,
                            onVolver = { navController.popBackStack() },
                            onServicio = abrirServicio,
                            onCuentaRequerida = { mostrarHojaCuenta = true }
                        )
                    }
                }
                composable(Rutas.BLOQUEADOS) {
                    Protegida(Rutas.BLOQUEADOS, rol, navController, irAlInicio) {
                        BloqueadosScreen(actividad, onVolver = { navController.popBackStack() })
                    }
                }
                composable(Rutas.REPORTES) {
                    Protegida(Rutas.REPORTES, rol, navController, irAlInicio) {
                        MisReportesScreen(actividad, onVolver = { navController.popBackStack() }, onVerNormas = { navController.navigate(Rutas.NORMAS) })
                    }
                }
                composable(Rutas.NORMAS) {
                    NormasScreen(onVolver = { navController.popBackStack() }, onVerReportes = { navController.navigate(Rutas.REPORTES) })
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

    if (mostrarHojaCuenta) {
        HojaCuentaRequerida(
            onCrearCuenta = { mostrarHojaCuenta = false; navController.navigate(Rutas.REGISTRO) },
            onIniciarSesion = { mostrarHojaCuenta = false; navController.navigate(Rutas.LOGIN) },
            onCerrar = { mostrarHojaCuenta = false }
        )
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
