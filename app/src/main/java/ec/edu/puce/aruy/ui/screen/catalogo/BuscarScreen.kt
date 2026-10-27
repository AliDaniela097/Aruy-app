package ec.edu.puce.aruy.ui.screen.catalogo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.catalogo.FiltroCatalogo
import ec.edu.puce.aruy.domain.catalogo.FiltrosCatalogo
import ec.edu.puce.aruy.domain.catalogo.OrdenCatalogo
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.ui.components.AruyChip
import ec.edu.puce.aruy.ui.components.AruyHoja
import ec.edu.puce.aruy.ui.components.AruyInterruptor
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.FilaServicio
import ec.edu.puce.aruy.ui.components.formatoDinero
import ec.edu.puce.aruy.ui.viewmodel.BuscarViewModel
import ec.edu.puce.aruy.ui.viewmodel.CatalogoViewModel

private val ZONAS = listOf("Norte", "Centro", "Sur", "Valles")
private const val PRECIO_MIN = 10f
private const val PRECIO_SIN_LIMITE = 100f

/** Buscar con filtros y orden (`buscar.js`), todo calculado en el teléfono sobre el catálogo descargado. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BuscarScreen(
    catalogo: CatalogoViewModel,
    buscar: BuscarViewModel,
    invitado: Boolean,
    enfocar: Boolean,
    onServicio: (String) -> Unit
) {
    val estado by catalogo.catalogo.collectAsState()
    val filtros by buscar.filtros.collectAsState()
    var hoja by rememberSaveable { mutableStateOf<String?>(null) }
    val foco = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        catalogo.cargarSiHaceFalta()
        if (enfocar) foco.requestFocus()
    }

    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = filtros.texto,
            onValueChange = { nuevo -> buscar.actualizar { it.copy(texto = nuevo) } },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .focusRequester(foco),
            placeholder = { Text("Servicio, oficio o nombre") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (filtros.texto.isNotEmpty()) {
                    IconButton(onClick = { buscar.actualizar { it.copy(texto = "") } }) {
                        Icon(Icons.Filled.Close, contentDescription = "Borrar búsqueda")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            shape = MaterialTheme.shapes.medium
        )

        FlowRow(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AruyChip(
                texto = if (filtros.cantidadActivos > 0) "Filtros (${filtros.cantidadActivos})" else "Filtros",
                seleccionado = filtros.cantidadActivos > 0,
                onClick = { hoja = "filtros" }
            )
            AruyChip(texto = filtros.orden.etiqueta, seleccionado = false, onClick = { hoja = "orden" })
            filtros.categoria?.let { InputChipQuitar(it.etiqueta) { buscar.actualizar { f -> f.copy(categoria = null) } } }
            filtros.zona?.let { InputChipQuitar(it) { buscar.actualizar { f -> f.copy(zona = null) } } }
            if (filtros.soloDisponibles) InputChipQuitar("Disponible ahora") { buscar.actualizar { it.copy(soloDisponibles = false) } }
            if (filtros.soloVerificados) InputChipQuitar("Cuenta verificada") { buscar.actualizar { it.copy(soloVerificados = false) } }
            filtros.precioMaximo?.let { InputChipQuitar("Hasta ${formatoDinero(it)}") { buscar.actualizar { f -> f.copy(precioMaximo = null) } } }
            if (filtros.calificacionMinima > 0) {
                InputChipQuitar(if (filtros.calificacionMinima >= 4.5) "Calificación 4,5 o más" else "Calificación 4 o más") {
                    buscar.actualizar { it.copy(calificacionMinima = 0.0) }
                }
            }
        }

        when (val e = estado) {
            EstadoApi.Inactivo, EstadoApi.Cargando -> EstadoCargando("Buscando servicios…")
            is EstadoApi.Error -> EstadoVacio(
                titulo = "No pudimos cargar",
                texto = e.mensaje,
                icono = Icons.Filled.SearchOff,
                textoBoton = "Reintentar",
                onBoton = catalogo::recargar
            )
            is EstadoApi.Exito -> {
                val resultados = remember(e.datos, filtros) { FiltroCatalogo.aplicar(e.datos, filtros) }
                Text(
                    if (resultados.size == 1) "1 servicio" else "${resultados.size} servicios",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite }
                )
                if (resultados.isEmpty()) {
                    EstadoVacio(
                        titulo = "No encontramos servicios",
                        texto = "Prueba con otra palabra, otra zona o quita algún filtro.",
                        icono = Icons.Filled.SearchOff,
                        textoBoton = if (filtros.cantidadActivos > 0 || filtros.texto.isNotEmpty()) "Quitar filtros" else null,
                        onBoton = buscar::limpiarTodo
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(resultados, key = { it.id }) { item ->
                            FilaServicio(item, invitado, catalogo::portada, onClick = { onServicio(item.id) })
                        }
                    }
                }
            }
        }
    }

    if (hoja == "filtros") {
        val total = (estado as? EstadoApi.Exito)?.datos
        HojaFiltros(
            filtros = filtros,
            cantidad = total?.let { FiltroCatalogo.aplicar(it, filtros).size },
            onCambio = { cambio -> buscar.actualizar(cambio) },
            onLimpiar = buscar::limpiar,
            onCerrar = { hoja = null }
        )
    }
    if (hoja == "orden") {
        AruyHoja("Ordenar por", onCerrar = { hoja = null }) {
            OrdenCatalogo.entries.forEach { orden ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .selectable(selected = filtros.orden == orden, role = Role.RadioButton) {
                            buscar.actualizar { it.copy(orden = orden) }
                            hoja = null
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = filtros.orden == orden, onClick = null)
                    Column(Modifier.padding(start = 12.dp)) {
                        Text(orden.etiqueta, style = MaterialTheme.typography.titleMedium)
                        Text(
                            orden.descripcion,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InputChipQuitar(texto: String, onQuitar: () -> Unit) {
    InputChip(
        selected = true,
        onClick = onQuitar,
        label = { Text(texto) },
        trailingIcon = { Icon(Icons.Filled.Close, contentDescription = "Quitar filtro: $texto", modifier = Modifier.padding(0.dp)) }
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HojaFiltros(
    filtros: FiltrosCatalogo,
    cantidad: Int?,
    onCambio: ((FiltrosCatalogo) -> FiltrosCatalogo) -> Unit,
    onLimpiar: () -> Unit,
    onCerrar: () -> Unit
) {
    AruyHoja("Filtros", onCerrar = onCerrar) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Categoría", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AruyChip("Todas", filtros.categoria == null, { onCambio { it.copy(categoria = null) } })
                CategoriaServicio.entries.forEach { c ->
                    AruyChip(c.etiqueta, filtros.categoria == c, { onCambio { it.copy(categoria = c) } })
                }
            }

            Text("Zona", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AruyChip("Todas", filtros.zona == null, { onCambio { it.copy(zona = null) } })
                ZONAS.forEach { z -> AruyChip(z, filtros.zona == z, { onCambio { it.copy(zona = z) } }) }
            }

            Text("Calificación", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AruyChip("Todas", filtros.calificacionMinima == 0.0, { onCambio { it.copy(calificacionMinima = 0.0) } })
                AruyChip("4 o más", filtros.calificacionMinima == 4.0, { onCambio { it.copy(calificacionMinima = 4.0) } })
                AruyChip("4,5 o más", filtros.calificacionMinima == 4.5, { onCambio { it.copy(calificacionMinima = 4.5) } })
            }

            AruyInterruptor("Solo disponibles ahora", filtros.soloDisponibles, { v -> onCambio { it.copy(soloDisponibles = v) } })
            AruyInterruptor(
                "Solo cuentas verificadas", filtros.soloVerificados, { v -> onCambio { it.copy(soloVerificados = v) } },
                descripcion = "Confirmaron su correo o celular."
            )

            val precio = filtros.precioMaximo?.toFloat() ?: PRECIO_SIN_LIMITE
            Text(
                "Precio máximo: " + if (filtros.precioMaximo == null) "sin límite" else formatoDinero(filtros.precioMaximo),
                style = MaterialTheme.typography.titleMedium
            )
            Slider(
                value = precio,
                onValueChange = { v ->
                    val redondeado = (Math.round(v / 5f) * 5).toFloat()
                    onCambio { it.copy(precioMaximo = if (redondeado >= PRECIO_SIN_LIMITE) null else redondeado.toDouble()) }
                },
                valueRange = PRECIO_MIN..PRECIO_SIN_LIMITE,
                steps = ((PRECIO_SIN_LIMITE - PRECIO_MIN) / 5f).toInt() - 1
            )

            BotonRelleno(
                texto = when (cantidad) {
                    null -> "Ver servicios"
                    1 -> "Ver 1 servicio"
                    else -> "Ver $cantidad servicios"
                },
                onClick = onCerrar,
                grande = true,
                ancho = true
            )
            BotonTexto("Limpiar", onLimpiar)
        }
    }
}
