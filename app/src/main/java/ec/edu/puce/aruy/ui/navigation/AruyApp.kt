package ec.edu.puce.aruy.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import ec.edu.puce.aruy.ui.components.HojaCuentaRequerida
import ec.edu.puce.aruy.ui.screen.perfil.AjustesScreen
import ec.edu.puce.aruy.ui.screen.perfil.AruyProScreen
import ec.edu.puce.aruy.ui.screen.perfil.BajaCuentaScreen
import ec.edu.puce.aruy.ui.screen.perfil.EditarPerfilScreen
import ec.edu.puce.aruy.ui.screen.perfil.PerfilScreen
import ec.edu.puce.aruy.ui.screen.perfil.PermisosScreen
import ec.edu.puce.aruy.ui.screen.perfil.PrivacidadScreen
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
import ec.edu.puce.aruy.ui.screen.prestador.EditorServicioScreen
import ec.edu.puce.aruy.ui.screen.prestador.MisServiciosScreen
import ec.edu.puce.aruy.ui.screen.prestador.PanelScreen
import ec.edu.puce.aruy.ui.screen.prestador.PortafolioScreen
import ec.edu.puce.aruy.ui.screen.sistema.AccesoDenegadoScreen
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel
import ec.edu.puce.aruy.ui.viewmodel.AjustesViewModel
import ec.edu.puce.aruy.ui.viewmodel.AruyProViewModel
import ec.edu.puce.aruy.ui.viewmodel.AuthViewModel
import ec.edu.puce.aruy.ui.viewmodel.BajaCuentaViewModel
import ec.edu.puce.aruy.ui.viewmodel.BuscarViewModel
import ec.edu.puce.aruy.ui.viewmodel.EditarPerfilViewModel
import ec.edu.puce.aruy.ui.viewmodel.CatalogoViewModel
import ec.edu.puce.aruy.ui.viewmodel.DetalleViewModel
import ec.edu.puce.aruy.ui.viewmodel.EditorServicioViewModel
import ec.edu.puce.aruy.ui.viewmodel.MisServiciosViewModel
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
    val misServicios: MisServiciosViewModel = viewModel(factory = contenedor.misServiciosViewModelFactory)
    val misServiciosUi by misServicios.ui.collectAsState()

    var mostrarHojaCuenta by remember { mutableStateOf(false) }

    // Aviso "Sin conexión" y reintento automático del catálogo cuando vuelve internet
    val hayConexion by contenedor.conexion.hayConexion.collectAsState(initial = true)
    LaunchedEffect(hayConexion) { if (hayConexion) catalogo.recargarSiFallo() }

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
        Column(Modifier.padding(relleno)) {
        AvisoSinConexion(visible = !hayConexion && rutaActual != Rutas.SPLASH)
        Box(Modifier.weight(1f)) {
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
                    Protegida(Rutas.INICIO, rol, irAlInicio) {
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
                    Protegida(Rutas.BUSCAR, rol, irAlInicio) {
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
                    Protegida(Rutas.GUARDADOS, rol, irAlInicio) {
                        GuardadosScreen(
                            catalogo = catalogo,
                            actividad = actividad,
                            onServicio = abrirServicio,
                            onBuscar = { navController.navigate(Rutas.buscar()) }
                        )
                    }
                }
                composable(Rutas.PERFIL) {
                    Protegida(Rutas.PERFIL, rol, irAlInicio) {
                        (estadoSesion as? EstadoSesion.Autenticada)?.let { autenticada ->
                            PerfilScreen(
                                usuario = autenticada.sesion.usuario,
                                recibeSolicitudes = misServiciosUi.recibeSolicitudes,
                                onCambiarRecepcion = misServicios::cambiarRecepcion,
                                alAbrir = { if (rol == RolNavegacion.PRESTADOR) misServicios.cargarSiHaceFalta() },
                                onEditar = { navController.navigate(Rutas.EDITAR_PERFIL) },
                                onAjustes = { navController.navigate(Rutas.AJUSTES) },
                                onPro = { navController.navigate(Rutas.PRO) },
                                onPermisos = { navController.navigate(Rutas.PERMISOS) },
                                onPrivacidad = { navController.navigate(Rutas.PRIVACIDAD) },
                                onBloqueados = { navController.navigate(Rutas.BLOQUEADOS) },
                                onReportes = { navController.navigate(Rutas.REPORTES) },
                                onNormas = { navController.navigate(Rutas.NORMAS) },
                                onBaja = { navController.navigate(Rutas.BAJA) },
                                onCerrarSesion = authViewModel::cerrarSesion
                            )
                        }
                    }
                }
                composable(Rutas.EDITAR_PERFIL) {
                    Protegida(Rutas.EDITAR_PERFIL, rol, irAlInicio) {
                        val vm: EditarPerfilViewModel = viewModel(factory = contenedor.editarPerfilViewModelFactory)
                        EditarPerfilScreen(vm, onVolver = { navController.popBackStack() })
                    }
                }
                composable(Rutas.AJUSTES) {
                    Protegida(Rutas.AJUSTES, rol, irAlInicio) {
                        val vm: AjustesViewModel = viewModel(factory = contenedor.ajustesViewModelFactory)
                        AjustesScreen(vm, onVolver = { navController.popBackStack() })
                    }
                }
                composable(Rutas.PERMISOS) {
                    Protegida(Rutas.PERMISOS, rol, irAlInicio) {
                        PermisosScreen(onVolver = { navController.popBackStack() })
                    }
                }
                composable(Rutas.PRIVACIDAD) {
                    Protegida(Rutas.PRIVACIDAD, rol, irAlInicio) {
                        PrivacidadScreen(
                            onVolver = { navController.popBackStack() },
                            onPermisos = { navController.navigate(Rutas.PERMISOS) },
                            onBaja = { navController.navigate(Rutas.BAJA) }
                        )
                    }
                }
                composable(Rutas.BAJA) {
                    Protegida(Rutas.BAJA, rol, irAlInicio) {
                        val vm: BajaCuentaViewModel = viewModel(factory = contenedor.bajaCuentaViewModelFactory)
                        BajaCuentaScreen(vm, onVolver = { navController.popBackStack() })
                    }
                }
                composable(Rutas.PRO) {
                    Protegida(Rutas.PRO, rol, irAlInicio) {
                        val vm: AruyProViewModel = viewModel(factory = contenedor.aruyProViewModelFactory)
                        AruyProScreen(vm, onVolver = { navController.popBackStack() })
                    }
                }

                composable(Rutas.PATRON_SERVICIO) { entrada ->
                    val id = entrada.arguments?.getString("id").orEmpty()
                    Protegida(Rutas.SERVICIO, rol, irAlInicio) {
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
                    Protegida(Rutas.PRESTADOR_PUBLICO, rol, irAlInicio) {
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
                    Protegida(Rutas.BLOQUEADOS, rol, irAlInicio) {
                        BloqueadosScreen(actividad, onVolver = { navController.popBackStack() })
                    }
                }
                composable(Rutas.REPORTES) {
                    Protegida(Rutas.REPORTES, rol, irAlInicio) {
                        MisReportesScreen(actividad, onVolver = { navController.popBackStack() }, onVerNormas = { navController.navigate(Rutas.NORMAS) })
                    }
                }
                composable(Rutas.NORMAS) {
                    NormasScreen(onVolver = { navController.popBackStack() }, onVerReportes = { navController.navigate(Rutas.REPORTES) })
                }

                composable(Rutas.PANEL) {
                    Protegida(Rutas.PANEL, rol, irAlInicio) {
                        PanelScreen(
                            viewModel = misServicios,
                            nombre = (estadoSesion as? EstadoSesion.Autenticada)?.sesion?.usuario?.nombre.orEmpty(),
                            onNuevoServicio = { navController.navigate(Rutas.editor()) },
                            onVerServicios = { navController.navigate(Rutas.MIS_SERVICIOS) },
                            onEditar = { navController.navigate(Rutas.editor(it)) }
                        )
                    }
                }
                composable(Rutas.MIS_SERVICIOS) {
                    Protegida(Rutas.MIS_SERVICIOS, rol, irAlInicio) {
                        MisServiciosScreen(
                            viewModel = misServicios,
                            onNuevo = { navController.navigate(Rutas.editor()) },
                            onEditar = { navController.navigate(Rutas.editor(it)) }
                        )
                    }
                }
                composable(Rutas.PORTAFOLIO) {
                    Protegida(Rutas.PORTAFOLIO, rol, irAlInicio) {
                        PortafolioScreen(misServicios, onPublicarServicio = { navController.navigate(Rutas.editor()) })
                    }
                }
                composable(
                    Rutas.PATRON_EDITOR,
                    arguments = listOf(navArgument("id") { type = NavType.StringType; nullable = true; defaultValue = null })
                ) { entrada ->
                    val id = entrada.arguments?.getString("id")?.takeIf { it.isNotEmpty() }
                    Protegida(Rutas.EDITOR, rol, irAlInicio) {
                        val vm: EditorServicioViewModel = viewModel(factory = contenedor.editorViewModelFactory(id))
                        EditorServicioScreen(
                            viewModel = vm,
                            onVolver = { navController.popBackStack() },
                            onGuardado = {
                                misServicios.invalidar()
                                navController.popBackStack()
                            }
                        )
                    }
                }
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

/** Franja fija bajo la barra de estado cuando el teléfono no tiene internet (`shell.js` del prototipo). */
@Composable
private fun AvisoSinConexion(visible: Boolean) {
    if (!visible) return
    Surface(
        color = MaterialTheme.colorScheme.inverseSurface,
        contentColor = MaterialTheme.colorScheme.inverseOnSurface,
        modifier = Modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.WifiOff, contentDescription = null, modifier = Modifier.size(20.dp))
            Text("Sin conexión. Algunas pantallas no cargarán hasta que vuelvas a conectarte.", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Guarda de ruta (`router.js`): con un rol que no corresponde muestra "acceso denegado". Sin sesión no
 * muestra nada: al cerrarla o vencer el token, el efecto central de [AruyApp] decide a dónde ir
 * (bienvenida o login). Dos redirecciones a la vez dejaban a la persona en el login al cerrar sesión.
 */
@Composable
private fun Protegida(
    ruta: String,
    rol: RolNavegacion?,
    irAlInicio: () -> Unit,
    contenido: @Composable () -> Unit
) {
    when {
        rol == null -> Unit
        !rutaPermitida(ruta, rol) -> AccesoDenegadoScreen(rol, irAlInicio)
        else -> contenido()
    }
}
