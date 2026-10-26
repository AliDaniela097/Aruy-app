package ec.edu.puce.aruy.ui.screen.catalogo

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.catalogo.Contacto
import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import ec.edu.puce.aruy.ui.components.AruyAvatar
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonWhatsApp
import ec.edu.puce.aruy.ui.components.Calificacion
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.FilaServicio
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.SelloVerificado
import ec.edu.puce.aruy.ui.components.TipoPastilla
import ec.edu.puce.aruy.ui.components.TituloSeccion
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.navigation.RolNavegacion
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel
import ec.edu.puce.aruy.ui.viewmodel.PrestadorViewModel
import kotlinx.coroutines.launch

/** Perfil público de un prestador (`prestadorPublico` en `detalle.js`). */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PrestadorPublicoScreen(
    perfilId: String,
    viewModel: PrestadorViewModel,
    actividad: ActividadLocalViewModel,
    rol: RolNavegacion,
    cargarPortada: suspend (String) -> String?,
    onVolver: () -> Unit,
    onServicio: (String) -> Unit,
    onCuentaRequerida: () -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()
    var hoja by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmarBloqueo by rememberSaveable { mutableStateOf(false) }
    var fotoAbierta by rememberSaveable { mutableStateOf<Int?>(null) }

    fun avisar(texto: String) { alcance.launch { snackbar.avisar(texto) } }
    LaunchedEffect(Unit) { viewModel.cargar() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
        topBar = {
            BarraSuperior("Perfil", onVolver)
        }
    ) { relleno ->
        val prestador = ui.prestador
        when {
            ui.cargando && prestador == null && ui.servicios.isEmpty() -> EstadoCargando("Abriendo el perfil…", Modifier.padding(relleno))
            ui.error != null && prestador == null && ui.servicios.isEmpty() -> EstadoVacio(
                titulo = "No pudimos abrir este perfil", texto = ui.error.orEmpty(), icono = Icons.Filled.PersonOff,
                modifier = Modifier.padding(relleno), textoBoton = "Reintentar", onBoton = viewModel::cargar
            )
            else -> {
                val nombre = prestador?.nombre ?: "Prestador"
                Column(
                    Modifier
                        .padding(relleno)
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        AruyAvatar(nombre, tamano = 96.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(nombre, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
                            prestador?.oficio?.takeIf { it.isNotBlank() }?.let {
                                Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { hoja = "opciones" }) { Icon(Icons.Filled.MoreVert, contentDescription = "Más opciones") }
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Calificacion(ui.opiniones?.promedio ?: prestador?.promedio, ui.opiniones?.total ?: prestador?.totalCalificaciones ?: 0)
                        if (prestador?.disponible != false) Pastilla("Disponible ahora", tipo = TipoPastilla.OK)
                        else Pastilla("No recibe solicitudes", tipo = TipoPastilla.ADVERTENCIA)
                        if (prestador?.verificado == true) SelloVerificado(conTexto = true)
                        prestador?.zona?.takeIf { it.isNotBlank() }?.let { Pastilla("Zona $it") }
                    }
                    prestador?.descripcion?.takeIf { it.isNotBlank() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyLarge)
                    }
                    BotonWhatsApp(
                        "Contactar por WhatsApp",
                        onClick = { if (rol == RolNavegacion.INVITADO) onCuentaRequerida() else hoja = "contactar" },
                        icono = Icons.AutoMirrored.Filled.Chat,
                        habilitado = Contacto.numeroWhatsApp(prestador?.telefono) != null
                    )

                    TituloSeccion("Servicios")
                    if (ui.servicios.isEmpty()) {
                        Text("Aún no publicó servicios.", style = MaterialTheme.typography.bodyMedium)
                    }
                    ui.servicios.forEach { servicio ->
                        FilaServicio(ServicioConPrestador(servicio, prestador), false, cargarPortada, onClick = { onServicio(servicio.id) })
                    }

                    TituloSeccion("Trabajos")
                    PestanaTrabajos(ui.fotos, onAbrirFoto = { fotoAbierta = it })

                    TituloSeccion("Opiniones")
                    PestanaOpiniones(ui.opiniones, RolNavegacion.PRESTADOR, onEscribir = {})
                }
            }
        }
    }

    val prestador = ui.prestador
    val nombre = prestador?.nombre ?: "Prestador"
    when (hoja) {
        "contactar" -> HojaContactar(
            nombre = nombre,
            oficio = prestador?.oficio,
            tituloServicio = null,
            disponible = prestador?.disponible != false,
            onAbrirWhatsApp = { mensaje ->
                val numero = Contacto.numeroWhatsApp(prestador?.telefono)
                hoja = null
                if (numero == null) {
                    avisar("Este prestador no tiene un número de WhatsApp válido.")
                } else {
                    val abierto = try {
                        contexto.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                        true
                    } catch (e: ActivityNotFoundException) {
                        false
                    }
                    if (abierto) {
                        actividad.registrarContacto(perfilId, null, nombre, mensaje)
                        avisar("Abrimos WhatsApp con tu mensaje. Quedó en tu historial.")
                    } else {
                        avisar("No pudimos abrir WhatsApp en este teléfono.")
                    }
                }
            },
            onCopiar = { mensaje ->
                (contexto.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager)
                    .setPrimaryClip(android.content.ClipData.newPlainText("Mensaje para $nombre", mensaje))
                hoja = null
                avisar("Mensaje copiado")
            },
            onCerrar = { hoja = null }
        )
        "opciones" -> HojaMasOpciones(
            nombre = nombre, objetivo = "perfil",
            onReportar = { hoja = "reportar" },
            onBloquear = { hoja = null; confirmarBloqueo = true },
            onCerrar = { hoja = null }
        )
        "reportar" -> HojaReportar(
            objetivo = "este perfil",
            onEnviar = { motivo, detalle ->
                actividad.reportar("PERFIL", perfilId, nombre, motivo.titulo, detalle)
                hoja = null
                avisar("Reporte guardado en este teléfono.")
            },
            onCerrar = { hoja = null }
        )
    }
    if (confirmarBloqueo) {
        DialogoConfirmacion(
            titulo = "¿Bloquear a $nombre?",
            texto = "Dejarás de ver sus servicios y su perfil. Puedes desbloquearla desde tu perfil cuando quieras.",
            textoConfirmar = "Bloquear",
            peligroso = true,
            onConfirmar = {
                confirmarBloqueo = false
                actividad.bloquear(perfilId, nombre)
                onVolver()
            },
            onCancelar = { confirmarBloqueo = false }
        )
    }
    fotoAbierta?.let { VisorDeFotos(ui.fotos, it, onCerrar = { fotoAbierta = null }) }
}

