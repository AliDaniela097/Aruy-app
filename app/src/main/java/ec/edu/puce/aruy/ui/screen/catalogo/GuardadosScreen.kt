package ec.edu.puce.aruy.ui.screen.catalogo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.catalogo.TiempoRelativo
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.BotonPeligro
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.tituloDeError
import ec.edu.puce.aruy.ui.components.FilaServicio
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel
import ec.edu.puce.aruy.ui.viewmodel.CatalogoViewModel

/** Guardados: favoritos y prestadores contactados (`guardados.js`). Todo vive en el teléfono. */
@Composable
fun GuardadosScreen(
    catalogo: CatalogoViewModel,
    actividad: ActividadLocalViewModel,
    onServicio: (String) -> Unit,
    onBuscar: () -> Unit
) {
    val estado by catalogo.catalogo.collectAsState()
    val favoritosIds by actividad.favoritosIds.collectAsState()
    val contactos by actividad.contactos.collectAsState()
    var pestana by rememberSaveable { mutableStateOf(0) }
    var confirmarBorrado by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { catalogo.cargarSiHaceFalta() }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = pestana) {
            Tab(selected = pestana == 0, onClick = { pestana = 0 }, text = { Text("Favoritos") })
            Tab(selected = pestana == 1, onClick = { pestana = 1 }, text = { Text("Contactados") })
        }
        if (pestana == 0) {
            when (val e = estado) {
                EstadoApi.Inactivo, EstadoApi.Cargando -> EstadoCargando("Cargando tus favoritos…")
                is EstadoApi.Error -> EstadoVacio(
                    titulo = tituloDeError(e.mensaje), texto = e.mensaje, icono = Icons.Filled.FavoriteBorder,
                    textoBoton = "Reintentar", onBoton = catalogo::recargar
                )
                is EstadoApi.Exito -> {
                    val items = favoritosIds.mapNotNull { id -> e.datos.firstOrNull { it.id == id } }
                    if (items.isEmpty()) {
                        EstadoVacio(
                            titulo = "Aún no guardas a nadie",
                            texto = "Toca el corazón en un servicio para encontrarlo rápido más tarde.",
                            icono = Icons.Filled.FavoriteBorder,
                            textoBoton = "Buscar servicios",
                            onBoton = onBuscar
                        )
                    } else {
                        LazyColumn(
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(items, key = { it.id }) { item ->
                                FilaServicio(
                                    item, invitado = false, cargarPortada = catalogo::portada,
                                    onClick = { onServicio(item.id) },
                                    accion = {
                                        IconButton(onClick = { actividad.alternarFavorito(item.id, true) }) {
                                            Icon(Icons.Filled.Favorite, contentDescription = "Quitar de favoritos: ${item.servicio.titulo}", tint = MaterialTheme.colorScheme.secondary)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            if (contactos.isEmpty()) {
                EstadoVacio(
                    titulo = "Todavía no contactaste a nadie",
                    texto = "Cuando escribas por WhatsApp a un prestador, aparecerá aquí.",
                    icono = Icons.AutoMirrored.Filled.Chat
                )
            } else {
                LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            "Los últimos prestadores que contactaste desde Aruy.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(contactos, key = { it.id }) { contacto ->
                        AruyTarjeta(tipo = TipoTarjeta.CONTORNO, modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(contacto.nombre, style = MaterialTheme.typography.titleMedium)
                                Text(TiempoRelativo.texto(contacto.fecha), style = MaterialTheme.typography.bodyMedium)
                            }
                            Text("«${contacto.mensaje}»", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
                            contacto.servicioId?.let { id ->
                                BotonContorno("Ver servicio", { onServicio(id) })
                            }
                        }
                    }
                    item { BotonPeligro("Borrar historial de contactos", { confirmarBorrado = true }, soloTexto = true) }
                }
            }
        }
    }

    if (confirmarBorrado) {
        DialogoConfirmacion(
            titulo = "¿Borrar el historial?",
            texto = "Se borra la lista de prestadores que contactaste desde este teléfono. Tus opiniones no cambian.",
            textoConfirmar = "Borrar historial",
            peligroso = true,
            onConfirmar = { confirmarBorrado = false; actividad.borrarHistorialDeContactos() },
            onCancelar = { confirmarBorrado = false }
        )
    }
}
