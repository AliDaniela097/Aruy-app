package ec.edu.puce.aruy.ui.screen.prestador

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WorkOff
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.ui.components.AruyInterruptor
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.tituloDeError
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.components.TituloSeccion
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.viewmodel.MisServiciosViewModel

/** Panel del prestador (`panel` en `prestador.js`), sin métricas porque el backend no las tiene. */
@Composable
fun PanelScreen(
    viewModel: MisServiciosViewModel,
    nombre: String,
    onNuevoServicio: () -> Unit,
    onVerServicios: () -> Unit,
    onEditar: (String) -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) { viewModel.cargarSiHaceFalta() }
    LaunchedEffect(Unit) { viewModel.mensajes.collect { snackbar.avisar(it) } }

    Box(Modifier.fillMaxSize()) {
        when {
            !ui.cargado && ui.cargando -> EstadoCargando("Cargando tu panel…")
            ui.error != null && ui.servicios.isEmpty() -> EstadoVacio(
                titulo = tituloDeError(ui.error.orEmpty()), texto = ui.error.orEmpty(), icono = Icons.Filled.WorkOff,
                textoBoton = "Reintentar", onBoton = viewModel::cargar
            )
            else -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column {
                        Text(
                            "Hola, ${nombre.trim().substringBefore(' ')}",
                            style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { heading() }
                        )
                        Text(
                            "Así va tu perfil.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                item {
                    AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
                        AruyInterruptor(
                            titulo = if (ui.recibeSolicitudes) "Recibiendo solicitudes" else "No recibes solicitudes",
                            activado = ui.recibeSolicitudes,
                            onCambio = viewModel::cambiarRecepcion,
                            descripcion = if (ui.recibeSolicitudes) "Apareces como «Disponible ahora» en las búsquedas."
                            else "Los clientes verán que no estás disponible por ahora."
                        )
                    }
                }
                item { CompletaTuPerfil(publicados = ui.servicios.size, fotos = ui.totalFotos) }
                item {
                    TituloSeccion("Tus servicios", accion = {
                        if (ui.servicios.isNotEmpty()) BotonTexto("Ver todos", onVerServicios)
                    })
                }
                if (ui.servicios.isEmpty()) {
                    item {
                        EstadoVacio(
                            titulo = "Aún no publicas ningún servicio",
                            texto = "Cuéntales a tus futuros clientes qué haces, cuánto cobras y muéstrales tu trabajo.",
                            icono = Icons.Filled.WorkOff,
                            textoBoton = "Publicar mi primer servicio",
                            onBoton = onNuevoServicio
                        )
                    }
                } else {
                    items(ui.servicios.take(3), key = { it.servicio.id }) { propio ->
                        FilaServicioPropio(propio, onClick = { onEditar(propio.servicio.id) })
                    }
                    item { BotonRelleno("Nuevo servicio", onNuevoServicio, ancho = true) }
                }
            }
        }
        if (ui.cargando && ui.cargado) {
            LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/** "Completa tu perfil": lo que falta para que el perfil genere confianza. */
@Composable
private fun CompletaTuPerfil(publicados: Int, fotos: Int) {
    val pasos = listOf(
        "Publica tu primer servicio" to (publicados > 0),
        "Sube 3 fotos de tus trabajos" to (fotos >= 3)
    )
    val hechos = pasos.count { it.second }
    AruyTarjeta(tipo = TipoTarjeta.ACENTO, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Completa tu perfil", style = MaterialTheme.typography.titleMedium)
            Text("$hechos de ${pasos.size}", style = MaterialTheme.typography.labelLarge)
        }
        LinearProgressIndicator(
            progress = { hechos / pasos.size.toFloat() },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )
        pasos.forEach { (texto, hecho) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    if (hecho) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                    contentDescription = if (hecho) "Hecho" else "Pendiente",
                    modifier = Modifier.size(20.dp)
                )
                Text(texto, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
