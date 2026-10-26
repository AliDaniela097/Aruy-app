package ec.edu.puce.aruy.ui.screen.prestador

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.validation.ValidacionesServicio
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.AruyChip
import ec.edu.puce.aruy.ui.components.AruyInterruptor
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonPeligro
import ec.edu.puce.aruy.ui.components.BotonRelleno

import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.HojaAgregarFoto
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.components.rememberAccionesFoto
import ec.edu.puce.aruy.ui.viewmodel.EditorServicioViewModel
import ec.edu.puce.aruy.ui.viewmodel.EstadoSubida
import ec.edu.puce.aruy.ui.viewmodel.FotoPendiente

/** Nuevo servicio o editar uno existente, con fotos que se convierten a WebP en el teléfono (`editor.js`). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorServicioScreen(
    viewModel: EditorServicioViewModel,
    onVolver: () -> Unit,
    onGuardado: () -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var hojaFoto by rememberSaveable { mutableStateOf(false) }
    var fotoPorQuitar by remember { mutableStateOf<FotoPortafolio?>(null) }
    var confirmarEliminar by rememberSaveable { mutableStateOf(false) }

    val acciones = rememberAccionesFoto { uri, desdeCamara -> viewModel.agregarFoto(uri, desdeCamara) }

    LaunchedEffect(Unit) { viewModel.mensajes.collect { snackbar.avisar(it) } }
    LaunchedEffect(ui.finalizado) { if (ui.finalizado) onGuardado() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BarraSuperior(if (ui.esEdicion) "Editar servicio" else "Nuevo servicio", onVolver)
            if (ui.cargando) {
                EstadoCargando("Abriendo el servicio…")
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SeccionFotos(
                        ui = ui,
                        onAgregar = { hojaFoto = true },
                        onQuitarPendiente = viewModel::quitarPendiente,
                        onQuitarGuardada = { fotoPorQuitar = it }
                    )

                    ui.categoriaSugerida?.takeIf { it != ui.categoria }?.let { sugerida ->
                        AruyTarjeta(tipo = TipoTarjeta.ACENTO, modifier = Modifier.fillMaxWidth()) {
                            Text("Categoría sugerida por la foto: ${sugerida.etiqueta}", style = MaterialTheme.typography.titleMedium)
                            BotonRelleno("Usar «${sugerida.etiqueta}»", viewModel::usarCategoriaSugerida, modifier = Modifier.padding(top = 8.dp))
                        }
                    }

                    AruyCampo(
                        valor = ui.titulo,
                        onValor = viewModel::cambiarTitulo,
                        etiqueta = "Título del servicio",
                        placeholder = "Ejemplo: Instalación de breakers y luminarias",
                        error = ui.errorTitulo,
                        maxCaracteres = ValidacionesServicio.TITULO_MAX,
                        teclado = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Categoría", style = MaterialTheme.typography.titleMedium)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            CategoriaServicio.entries.forEach { categoria ->
                                AruyChip(categoria.etiqueta, ui.categoria == categoria, { viewModel.cambiarCategoria(categoria) })
                            }
                        }
                        ui.errorCategoria?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                    }

                    AruyCampo(
                        valor = ui.descripcion,
                        onValor = viewModel::cambiarDescripcion,
                        etiqueta = "Descripción",
                        placeholder = "Cuenta qué incluye, cómo trabajas y en qué zonas atiendes.",
                        ayuda = "${ui.descripcion.length} / ${ValidacionesServicio.DESCRIPCION_MAX}",
                        error = ui.errorDescripcion,
                        unaLinea = false,
                        lineasMin = 5,
                        maxCaracteres = ValidacionesServicio.DESCRIPCION_MAX
                    )

                    AruyCampo(
                        valor = ui.precio,
                        onValor = viewModel::cambiarPrecio,
                        etiqueta = "Precio referencial (opcional)",
                        placeholder = "15",
                        ayuda = "Es una guía en dólares; lo acuerdas por WhatsApp.",
                        error = ui.errorPrecio,
                        teclado = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done)
                    )

                    AruyInterruptor(
                        titulo = "Disponible para nuevas solicitudes",
                        activado = ui.disponible,
                        onCambio = viewModel::cambiarDisponible,
                        descripcion = "Si lo pausas, el servicio sigue visible pero se marca como no disponible."
                    )

                    ui.progreso?.let {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            LinearProgressIndicator(Modifier.fillMaxWidth())
                            Text(it, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        }
                    }

                    BotonRelleno(
                        texto = if (ui.esEdicion) "Guardar cambios" else "Publicar servicio",
                        onClick = viewModel::guardar,
                        grande = true,
                        ancho = true,
                        cargando = ui.guardando
                    )
                    if (ui.esEdicion) {
                        BotonPeligro("Eliminar servicio", { confirmarEliminar = true }, soloTexto = true)
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (hojaFoto) HojaAgregarFoto(acciones, onCerrar = { hojaFoto = false })

    fotoPorQuitar?.let { foto ->
        DialogoConfirmacion(
            titulo = "¿Quitar esta foto?",
            texto = "Se borra del servicio y del almacenamiento. No se puede deshacer.",
            textoConfirmar = "Quitar",
            peligroso = true,
            onConfirmar = { fotoPorQuitar = null; viewModel.quitarFotoGuardada(foto) },
            onCancelar = { fotoPorQuitar = null }
        )
    }
    if (confirmarEliminar) {
        DialogoConfirmacion(
            titulo = "¿Eliminar este servicio?",
            texto = "Dejará de aparecer en las búsquedas y se borrarán sus fotos. No se puede deshacer.",
            textoConfirmar = "Eliminar",
            peligroso = true,
            onConfirmar = { confirmarEliminar = false; viewModel.eliminarServicio() },
            onCancelar = { confirmarEliminar = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SeccionFotos(
    ui: ec.edu.puce.aruy.ui.viewmodel.EditorUi,
    onAgregar: () -> Unit,
    onQuitarPendiente: (Long) -> Unit,
    onQuitarGuardada: (FotoPortafolio) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Fotos de tu trabajo", style = MaterialTheme.typography.titleMedium)
            Text("${ui.totalFotos} de 10", style = MaterialTheme.typography.labelLarge)
        }
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ui.fotosGuardadas.forEach { foto ->
                MiniaturaFoto(onQuitar = { onQuitarGuardada(foto) }, descripcion = "Foto guardada del servicio") {
                    AsyncImage(foto.url, null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            ui.pendientes.forEach { pendiente ->
                MiniaturaFoto(
                    onQuitar = { onQuitarPendiente(pendiente.id) },
                    descripcion = "Foto nueva, todavía sin subir",
                    quitarHabilitado = pendiente.estado !is EstadoSubida.Subiendo
                ) {
                    Image(
                        pendiente.imagen.miniatura.asImageBitmap(), null,
                        contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()
                    )
                    EstadoDeLaFoto(pendiente)
                }
            }
            if (ui.totalFotos < 10) {
                Surface(
                    onClick = onAgregar,
                    enabled = ui.puedeAgregarFoto,
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.size(96.dp)
                ) {
                    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        if (ui.analizandoFoto) {
                            LinearProgressIndicator(Modifier.padding(horizontal = 12.dp))
                            Text("Analizando…", style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 8.dp))
                        } else {
                            Icon(Icons.Filled.AddAPhoto, contentDescription = null)
                            Text(if (ui.totalFotos == 0) "Agregar foto" else "Otra foto", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        Text(
            "Aruy analiza cada foto en tu teléfono y te sugiere una categoría. Se envían en WebP, de máximo 1280 px y 500 KB.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (ui.etiquetasUltimaFoto.isNotEmpty()) {
            Text("Etiquetas de tu última foto", style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ui.etiquetasUltimaFoto.forEach { etiqueta ->
                    ec.edu.puce.aruy.ui.components.Pastilla("${etiqueta.texto} ${(etiqueta.confianza * 100).toInt()} %")
                }
            }
        }
    }
}

@Composable
private fun MiniaturaFoto(
    onQuitar: () -> Unit,
    descripcion: String,
    quitarHabilitado: Boolean = true,
    contenido: @Composable () -> Unit
) {
    Box(Modifier.size(96.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .semantics { contentDescription = descripcion }
        ) { contenido() }
        IconButton(
            onClick = onQuitar,
            enabled = quitarHabilitado,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(36.dp)
                .padding(4.dp)
                .background(Color.Black.copy(alpha = 0.55f), CircleShape)
        ) {
            Icon(Icons.Filled.Close, contentDescription = "Quitar foto", tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EstadoDeLaFoto(pendiente: FotoPendiente) {
    when (val estado = pendiente.estado) {
        EstadoSubida.Subiendo -> Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)), contentAlignment = Alignment.Center) {
            LinearProgressIndicator(Modifier.padding(horizontal = 12.dp))
        }
        is EstadoSubida.Fallo -> Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.ErrorOutline, contentDescription = estado.mensaje, tint = Color.White)
        }
        EstadoSubida.Pendiente -> Unit
    }
}
