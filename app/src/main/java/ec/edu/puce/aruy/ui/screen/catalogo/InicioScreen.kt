package ec.edu.puce.aruy.ui.screen.catalogo

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.catalogo.FiltroCatalogo
import ec.edu.puce.aruy.domain.catalogo.OrdenCatalogo
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.Esqueleto
import ec.edu.puce.aruy.ui.components.FilaServicio
import ec.edu.puce.aruy.ui.components.TarjetaServicio
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.components.TituloSeccion
import ec.edu.puce.aruy.ui.components.icono
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel
import ec.edu.puce.aruy.ui.viewmodel.CatalogoViewModel

/** Pantalla de inicio del contratante o del invitado (`home.js` del prototipo, sin distancia ni anuncios). */
@Composable
fun InicioScreen(
    catalogo: CatalogoViewModel,
    actividad: ActividadLocalViewModel,
    nombre: String?,
    invitado: Boolean,
    onBuscar: () -> Unit,
    onCategoria: (CategoriaServicio) -> Unit,
    onVerTodos: (OrdenCatalogo?) -> Unit,
    onServicio: (String) -> Unit,
    onCrearCuenta: () -> Unit
) {
    val estado by catalogo.catalogo.collectAsState()
    val vistosIds by actividad.vistosIds.collectAsState()

    LaunchedEffect(Unit) { catalogo.cargarSiHaceFalta() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (nombre != null) "Hola, ${nombre.trim().substringBefore(' ')}" else "Hola",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        "¿Qué servicio necesitas hoy?",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = catalogo::recargar, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Filled.Refresh, contentDescription = "Actualizar")
                }
            }
        }

        if (invitado) {
            item {
                AruyTarjeta(tipo = TipoTarjeta.ACENTO, modifier = Modifier.fillMaxWidth()) {
                    Text("Estás explorando sin cuenta.", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Crea una para ver precios, calificaciones y contactar.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    BotonRelleno("Crear cuenta", onCrearCuenta, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        item { CajaDeBusqueda(onBuscar) }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TituloSeccion("Categorías")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(CategoriaServicio.entries) { categoria ->
                        CategoriaCirculo(categoria) { onCategoria(categoria) }
                    }
                }
            }
        }

        when (val e = estado) {
            EstadoApi.Inactivo, EstadoApi.Cargando -> item { CargandoLista() }
            is EstadoApi.Error -> item {
                EstadoVacio(
                    titulo = "No pudimos cargar",
                    texto = e.mensaje,
                    icono = Icons.Filled.SearchOff,
                    textoBoton = "Reintentar",
                    onBoton = catalogo::recargar
                )
            }
            is EstadoApi.Exito -> {
                val servicios = e.datos
                if (servicios.isEmpty()) {
                    item {
                        EstadoVacio(
                            titulo = "Aún no hay servicios",
                            texto = "Cuando se publiquen, aparecerán aquí.",
                            icono = Icons.Filled.SearchOff
                        )
                    }
                } else {
                    contenido(servicios, vistosIds, invitado, catalogo, onVerTodos, onServicio)
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.contenido(
    servicios: List<ServicioConPrestador>,
    vistosIds: List<String>,
    invitado: Boolean,
    catalogo: CatalogoViewModel,
    onVerTodos: (OrdenCatalogo?) -> Unit,
    onServicio: (String) -> Unit
) {
    val disponibles = servicios.filter(FiltroCatalogo::disponibleAhora).take(8)
    val mejores = FiltroCatalogo.ordenar(servicios, OrdenCatalogo.MEJOR_CALIFICACION).take(5)
    val vistos = vistosIds.mapNotNull { id -> servicios.firstOrNull { it.id == id } }

    if (disponibles.isNotEmpty()) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                TituloSeccion("Disponibles ahora", accion = {
                    androidx.compose.material3.TextButton(onClick = { onVerTodos(null) }) { Text("Ver todos") }
                })
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(disponibles, key = { it.id }) { item ->
                        TarjetaServicio(item, invitado, catalogo::portada, onClick = { onServicio(item.id) })
                    }
                }
            }
        }
    }

    item {
        TituloSeccion("Mejor valorados", accion = {
            androidx.compose.material3.TextButton(onClick = { onVerTodos(OrdenCatalogo.MEJOR_CALIFICACION) }) { Text("Ver todos") }
        })
    }
    items(mejores, key = { "mejor-${it.id}" }) { item ->
        FilaServicio(item, invitado, catalogo::portada, onClick = { onServicio(item.id) })
    }

    if (vistos.isNotEmpty() && !invitado) {
        item { TituloSeccion("Vistos recientemente") }
        items(vistos, key = { "visto-${it.id}" }) { item ->
            FilaServicio(item, invitado, catalogo::portada, onClick = { onServicio(item.id) })
        }
    }
}

@Composable
private fun CajaDeBusqueda(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
    ) {
        Row(Modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Busca plomero, manicure, pintura…",
                modifier = Modifier.padding(start = 12.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoriaCirculo(categoria: CategoriaServicio, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(role = Role.Button, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(categoria.icono, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
        }
        Text(
            categoria.etiqueta,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

@Composable
private fun CargandoLista() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(3) { Esqueleto(Modifier.fillMaxWidth(), alto = 96.dp) }
        Text(
            "Cargando servicios…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
