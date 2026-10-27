package ec.edu.puce.aruy.ui.screen.prestador

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhotoLibrary
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ec.edu.puce.aruy.ui.components.AruyHoja
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.HojaAgregarFoto
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.components.rememberAccionesFoto
import ec.edu.puce.aruy.ui.viewmodel.MisServiciosViewModel

/** Portafolio (`portafolio` en `prestador.js`): todas las fotos de tus servicios, con alta y baja. */
@Composable
fun PortafolioScreen(
    viewModel: MisServiciosViewModel,
    onPublicarServicio: () -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var elegirServicio by rememberSaveable { mutableStateOf(false) }
    var destino by rememberSaveable { mutableStateOf<String?>(null) }
    var hojaFoto by rememberSaveable { mutableStateOf(false) }
    var porQuitar by remember { mutableStateOf<Pair<String, String>?>(null) }

    val acciones = rememberAccionesFoto { uri, desdeCamara ->
        destino?.let { viewModel.subirFoto(it, uri, desdeCamara) }
    }

    LaunchedEffect(Unit) { viewModel.cargarSiHaceFalta() }
    LaunchedEffect(Unit) { viewModel.mensajes.collect { snackbar.avisar(it) } }

    Box(Modifier.fillMaxSize()) {
        when {
            !ui.cargado && ui.cargando -> EstadoCargando("Cargando tu portafolio…")
            ui.servicios.isEmpty() -> EstadoVacio(
                titulo = "Primero publica un servicio",
                texto = "Las fotos de tus trabajos se suben dentro de un servicio.",
                icono = Icons.Filled.PhotoLibrary,
                textoBoton = "Publicar mi primer servicio",
                onBoton = onPublicarServicio
            )
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Las fotos de tus trabajos generan confianza. Cada una se analiza en tu teléfono y se sube en WebP.",
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Tus trabajos", style = MaterialTheme.typography.titleLarge)
                    Pastilla("${ui.totalFotos} ${if (ui.totalFotos == 1) "foto" else "fotos"}")
                }
                if (ui.subiendoFoto) {
                    AruyTarjeta(tipo = TipoTarjeta.ACENTO, modifier = Modifier.fillMaxWidth()) {
                        Text("Subiendo foto…", style = MaterialTheme.typography.titleMedium)
                        LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
                    }
                }
                BotonRelleno(
                    texto = "Agregar foto",
                    onClick = { elegirServicio = true },
                    icono = Icons.Filled.AddAPhoto,
                    ancho = true,
                    habilitado = !ui.subiendoFoto
                )
                ui.servicios.forEach { propio ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(propio.servicio.titulo, style = MaterialTheme.typography.titleMedium)
                        if (propio.fotos.isEmpty()) {
                            Text("Este servicio todavía no tiene fotos.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        propio.fotos.chunked(3).forEach { fila ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                fila.forEach { foto ->
                                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                                        AsyncImage(
                                            model = foto.url,
                                            contentDescription = foto.descripcion ?: "Foto de ${propio.servicio.titulo}",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize().clip(MaterialTheme.shapes.medium)
                                        )
                                        IconButton(
                                            onClick = { porQuitar = propio.servicio.id to foto.id },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .size(40.dp)
                                                .padding(6.dp)
                                                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                                        ) {
                                            Icon(Icons.Filled.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                repeat(3 - fila.size) { Box(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (elegirServicio) {
        AruyHoja("¿A qué servicio va la foto?", onCerrar = { elegirServicio = false }) {
            ui.servicios.forEach { propio ->
                AruyTarjeta(
                    onClick = { destino = propio.servicio.id; elegirServicio = false; hojaFoto = true },
                    tipo = TipoTarjeta.CONTORNO,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(propio.servicio.titulo, style = MaterialTheme.typography.titleMedium)
                    Text("${propio.fotos.size} de 10 fotos", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    if (hojaFoto) HojaAgregarFoto(acciones, onCerrar = { hojaFoto = false })

    porQuitar?.let { (servicioId, fotoId) ->
        DialogoConfirmacion(
            titulo = "¿Quitar esta foto?",
            texto = "Se borra del servicio y del almacenamiento. No se puede deshacer.",
            textoConfirmar = "Quitar",
            peligroso = true,
            onConfirmar = { porQuitar = null; viewModel.eliminarFoto(servicioId, fotoId) },
            onCancelar = { porQuitar = null }
        )
    }
}
