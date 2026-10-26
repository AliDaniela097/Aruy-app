package ec.edu.puce.aruy.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import ec.edu.puce.aruy.domain.model.CategoriaServicio

/** Rutas de la app, equivalentes a la tabla de `js/core/router.js` del prototipo. */
object Rutas {
    const val SPLASH = "splash"
    const val BIENVENIDA = "bienvenida"
    const val LOGIN = "login"
    const val REGISTRO = "registro"

    const val INICIO = "inicio"
    const val BUSCAR = "buscar"
    const val GUARDADOS = "guardados"
    const val PERFIL = "perfil"

    const val PANEL = "panel"
    const val MIS_SERVICIOS = "servicios"
    const val PORTAFOLIO = "portafolio"

    const val SERVICIO = "servicio"
    const val PRESTADOR_PUBLICO = "prestador"
    const val BLOQUEADOS = "bloqueados"
    const val REPORTES = "reportes"
    const val NORMAS = "normas"

    const val EDITOR = "editor"

    /** Patrones con argumentos, tal como se registran en el NavHost. */
    const val PATRON_BUSCAR = "$BUSCAR?cat={cat}&foco={foco}"
    const val PATRON_EDITOR = "$EDITOR?id={id}"
    const val PATRON_SERVICIO = "$SERVICIO/{id}"
    const val PATRON_PRESTADOR = "$PRESTADOR_PUBLICO/{id}"

    fun buscar(categoria: CategoriaServicio? = null, enfocar: Boolean = false) =
        "$BUSCAR?cat=${categoria?.name.orEmpty()}&foco=$enfocar"

    /** Sin id abre el formulario de un servicio nuevo. */
    fun editor(servicioId: String? = null) = "$EDITOR?id=${servicioId.orEmpty()}"

    fun servicio(id: String) = "$SERVICIO/$id"

    fun prestador(perfilId: String) = "$PRESTADOR_PUBLICO/$perfilId"

    /** Quita los argumentos: "servicio/{id}" y "buscar?cat=..." quedan como "servicio" y "buscar". */
    fun base(ruta: String?): String? = ruta?.substringBefore('?')?.substringBefore('/')

    /** Pantallas de entrada: con una sesión real redirigen al inicio del rol (`router.js`). */
    val ENTRADA = setOf(BIENVENIDA, LOGIN, REGISTRO)

    /** Rutas que se ven con la barra inferior. */
    val CON_BARRA = setOf(INICIO, BUSCAR, GUARDADOS, PERFIL, PANEL, MIS_SERVICIOS, PORTAFOLIO)
}

/** Quién está usando la app: define qué pestañas se ven y a qué pantallas puede entrar. */
enum class RolNavegacion { CONTRATANTE, INVITADO, PRESTADOR }

data class ItemNavegacion(
    val ruta: String,
    val titulo: String,
    val icono: ImageVector
)

/** Barra inferior de cada rol (`js/core/shell.js`). */
fun itemsDeNavegacion(rol: RolNavegacion): List<ItemNavegacion> = when (rol) {
    RolNavegacion.CONTRATANTE -> listOf(
        ItemNavegacion(Rutas.INICIO, "Inicio", Icons.Filled.Home),
        ItemNavegacion(Rutas.BUSCAR, "Buscar", Icons.Filled.Search),
        ItemNavegacion(Rutas.GUARDADOS, "Guardados", Icons.Filled.Favorite),
        ItemNavegacion(Rutas.PERFIL, "Perfil", Icons.Filled.Person)
    )
    RolNavegacion.INVITADO -> listOf(
        ItemNavegacion(Rutas.INICIO, "Inicio", Icons.Filled.Home),
        ItemNavegacion(Rutas.BUSCAR, "Buscar", Icons.Filled.Search),
        ItemNavegacion(Rutas.REGISTRO, "Crear cuenta", Icons.Filled.PersonAdd)
    )
    RolNavegacion.PRESTADOR -> listOf(
        ItemNavegacion(Rutas.PANEL, "Panel", Icons.Filled.Dashboard),
        ItemNavegacion(Rutas.MIS_SERVICIOS, "Servicios", Icons.Filled.Work),
        ItemNavegacion(Rutas.PORTAFOLIO, "Portafolio", Icons.Filled.Image),
        ItemNavegacion(Rutas.PERFIL, "Perfil", Icons.Filled.Person)
    )
}

/** Primera pantalla de cada rol (`AR.session.home()`). */
fun rutaInicial(rol: RolNavegacion): String =
    if (rol == RolNavegacion.PRESTADOR) Rutas.PANEL else Rutas.INICIO

/** Rutas permitidas por rol; el resto lleva a "acceso denegado" (guardas de `router.js`). */
fun rutaPermitida(ruta: String, rol: RolNavegacion): Boolean = when (ruta) {
    Rutas.PANEL, Rutas.MIS_SERVICIOS, Rutas.PORTAFOLIO, Rutas.EDITOR -> rol == RolNavegacion.PRESTADOR
    Rutas.GUARDADOS, Rutas.BLOQUEADOS -> rol == RolNavegacion.CONTRATANTE
    // Un invitado no ve el detalle: al tocar un servicio se le ofrece crear su cuenta
    Rutas.PERFIL, Rutas.SERVICIO, Rutas.PRESTADOR_PUBLICO, Rutas.REPORTES -> rol != RolNavegacion.INVITADO
    else -> true
}
