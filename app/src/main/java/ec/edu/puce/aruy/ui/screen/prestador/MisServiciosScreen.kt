package ec.edu.puce.aruy.ui.screen.prestador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.WorkOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.tituloDeError
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.TipoPastilla
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.components.icono
import ec.edu.puce.aruy.ui.components.textoPrecio
import ec.edu.puce.aruy.ui.viewmodel.MisServiciosViewModel
import ec.edu.puce.aruy.ui.viewmodel.ServicioPropio

/** Fila de un servicio propio: portada (primera foto), título, precio y estado. */
@Composable
fun FilaServicioPropio(
    propio: ServicioPropio,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    menu: (@Composable () -> Unit)? = null
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Row(
            Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val portada = propio.fotos.firstOrNull()?.url
            Box(
                Modifier
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (portada != null) {
                    AsyncImage(portada, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Icon(propio.servicio.categoria.icono, null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(propio.servicio.titulo, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(textoPrecio(propio.servicio.precioReferencial), style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (propio.servicio.disponible) Pastilla("Publicado", tipo = TipoPastilla.OK)
                    else Pastilla("Pausado", tipo = TipoPastilla.ADVERTENCIA)
                    Pastilla("${propio.fotos.size} ${if (propio.fotos.size == 1) "foto" else "fotos"}")
                }
            }
            menu?.invoke()
        }
    }
}

@Composable
fun MisServiciosScreen(
    viewModel: MisServiciosViewModel,
    onNuevo: () -> Unit,
    onEditar: (String) -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var porEliminar by rememberSaveable { mutableStateOf<String?>(null) }
    var menuAbierto by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { viewModel.cargarSiHaceFalta() }
    LaunchedEffect(Unit) { viewModel.mensajes.collect { snackbar.avisar(it) } }

    Box(Modifier.fillMaxSize()) {
        when {
            !ui.cargado && ui.cargando -> EstadoCargando("Cargando tus servicios…")
            ui.error != null && ui.servicios.isEmpty() -> EstadoVacio(
                titulo = tituloDeError(ui.error.orEmpty()), texto = ui.error.orEmpty(), icono = Icons.Filled.WorkOff,
                textoBoton = "Reintentar", onBoton = viewModel::cargar
            )
            ui.servicios.isEmpty() -> EstadoVacio(
                titulo = "Aún no publicas servicios",
                texto = "Cuéntales a tus futuros clientes qué haces, cuánto cobras y muéstrales tu trabajo.",
                icono = Icons.Filled.WorkOff,
                textoBoton = "Publicar mi primer servicio",
                onBoton = onNuevo
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(ui.servicios, key = { it.servicio.id }) { propio ->
                    FilaServicioPropio(
                        propio = propio,
                        onClick = { onEditar(propio.servicio.id) },
                        menu = {
                            Box {
                                IconButton(onClick = { menuAbierto = propio.servicio.id }) {
                                    Icon(Icons.Filled.MoreVert, contentDescription = "Opciones de ${propio.servicio.titulo}")
                                }
                                DropdownMenu(expanded = menuAbierto == propio.servicio.id, onDismissRequest = { menuAbierto = null }) {
                                    DropdownMenuItem(text = { Text("Editar") }, onClick = { menuAbierto = null; onEditar(propio.servicio.id) })
                                    DropdownMenuItem(
                                        text = { Text(if (propio.servicio.disponible) "Pausar" else "Reanudar") },
                                        onClick = { menuAbierto = null; viewModel.pausarOReanudar(propio.servicio) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Eliminar", color = MaterialTheme.colorScheme.error) },
                                        onClick = { menuAbierto = null; porEliminar = propio.servicio.id }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
        if (ui.cargando && ui.cargado) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        if (ui.servicios.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onNuevo,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Nuevo servicio") },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    val aEliminar = ui.servicios.firstOrNull { it.servicio.id == porEliminar }
    if (aEliminar != null) {
        DialogoConfirmacion(
            titulo = "¿Eliminar «${aEliminar.servicio.titulo}»?",
            texto = "Dejará de aparecer en las búsquedas y se borrarán sus ${aEliminar.fotos.size} fotos. No se puede deshacer.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            onConfirmar = { porEliminar = null; viewModel.eliminarServicio(aEliminar.servicio.id) },
            onCancelar = { porEliminar = null }
        )
    }
}
