package ec.edu.puce.aruy.ui.screen.catalogo

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import ec.edu.puce.aruy.domain.catalogo.Contacto
import ec.edu.puce.aruy.domain.catalogo.FiltroCatalogo
import ec.edu.puce.aruy.domain.model.FotoPortafolio
import ec.edu.puce.aruy.domain.model.ResumenOpiniones
import ec.edu.puce.aruy.ui.components.AruyAvatar
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.BotonWhatsApp
import ec.edu.puce.aruy.ui.components.Calificacion
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.SelloVerificado
import ec.edu.puce.aruy.ui.components.TipoPastilla
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.components.icono
import ec.edu.puce.aruy.ui.components.textoPrecio
import ec.edu.puce.aruy.ui.navigation.RolNavegacion
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel
import ec.edu.puce.aruy.ui.viewmodel.DetalleViewModel
import kotlinx.coroutines.launch

private val PESTANAS = listOf("Servicio", "Trabajos", "Opiniones")

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun DetalleServicioScreen(
    viewModel: DetalleViewModel,
    actividad: ActividadLocalViewModel,
    rol: RolNavegacion,
    onVolver: () -> Unit,
    onPrestador: (String) -> Unit,
    onCuentaRequerida: () -> Unit
) {
    val ui by viewModel.ui.collectAsState()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    var pestana by rememberSaveable { mutableStateOf(0) }
    var hoja by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmarBloqueo by rememberSaveable { mutableStateOf(false) }
    var fotoAbierta by rememberSaveable { mutableStateOf<Int?>(null) }

    val servicioId = ui.item?.id
    val esFavorito by remember(servicioId) {
        if (servicioId == null) kotlinx.coroutines.flow.flowOf(false) else actividad.esFavorito(servicioId)
    }.collectAsState(initial = false)

    LaunchedEffect(servicioId) { servicioId?.let(actividad::registrarVisto) }
    LaunchedEffect(Unit) { viewModel.mensajes.collect { snackbar.avisar(it) } }

    fun avisar(texto: String) { alcance.launch { snackbar.avisar(texto) } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            ui.item?.let { item ->
                BarraContacto(
                    nombre = item.nombrePrestador,
                    tieneTelefono = Contacto.numeroWhatsApp(item.prestador?.telefono) != null,
                    disponible = FiltroCatalogo.disponibleAhora(item),
                    onContactar = { hoja = "contactar" }
                )
            }
        }
    ) { relleno ->
        val item = ui.item
        when {
            item == null && ui.cargando -> EstadoCargando("Abriendo el servicio…", Modifier.padding(relleno))
            item == null -> EstadoVacio(
                titulo = "No encontramos este servicio",
                texto = ui.error ?: "Puede que ya no esté disponible.",
                icono = Icons.Filled.Close,
                modifier = Modifier.padding(relleno),
                textoBoton = "Volver",
                onBoton = onVolver
            )
            else -> Column(
                Modifier
                    .padding(relleno)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Galeria(
                    fotos = ui.fotos,
                    categoria = item.servicio.categoria,
                    esFavorito = esFavorito,
                    onVolver = onVolver,
                    onCompartir = { compartir(contexto, item.servicio.titulo) },
                    onFavorito = { actividad.alternarFavorito(item.id, esFavorito) { ahora ->
                        avisar(if (ahora) "Guardaste este servicio en favoritos." else "Lo quitaste de favoritos.")
                    } },
                    onMas = { hoja = "opciones" },
                    onAbrirFoto = { fotoAbierta = it }
                )

                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        item.servicio.titulo,
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(textoPrecio(item.servicio.precioReferencial), style = MaterialTheme.typography.titleLarge)

                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Calificacion(
                            ui.opiniones?.promedio ?: item.prestador?.promedio,
                            ui.opiniones?.total ?: item.prestador?.totalCalificaciones ?: 0
                        )
                        if (FiltroCatalogo.disponibleAhora(item)) Pastilla("Disponible ahora", tipo = TipoPastilla.OK)
                        else Pastilla("No recibe solicitudes", tipo = TipoPastilla.ADVERTENCIA)
                        if (item.prestador?.verificado == true) SelloVerificado(conTexto = true)
                    }

                    AruyTarjeta(onClick = { onPrestador(item.servicio.prestadorId) }, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AruyAvatar(item.nombrePrestador, tamano = 48.dp)
                            Column {
                                Text(item.nombrePrestador, style = MaterialTheme.typography.titleMedium)
                                val detalle = listOfNotNull(
                                    item.prestador?.oficio?.takeIf { it.isNotBlank() },
                                    item.prestador?.aniosExperiencia?.let { "$it años de experiencia" }
                                ).joinToString(" · ")
                                if (detalle.isNotEmpty()) {
                                    Text(detalle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    TabRow(selectedTabIndex = pestana) {
                        PESTANAS.forEachIndexed { i, titulo ->
                            Tab(selected = pestana == i, onClick = { pestana = i }, text = { Text(titulo) })
                        }
                    }
                    when (pestana) {
                        0 -> PestanaServicio(item.servicio.descripcion, item.servicio.categoria.etiqueta, item.prestador?.zona, item.prestador?.descripcion, item.nombrePrestador)
                        1 -> PestanaTrabajos(ui.fotos, onAbrirFoto = { fotoAbierta = it })
                        else -> PestanaOpiniones(
                            opiniones = ui.opiniones,
                            rol = rol,
                            onEscribir = {
                                if (rol == RolNavegacion.INVITADO) onCuentaRequerida() else hoja = "calificar"
                            }
                        )
                    }
                }
            }
        }
    }

    val item = ui.item
    if (item != null) {
        when (hoja) {
            "contactar" -> HojaContactar(
                nombre = item.nombrePrestador,
                oficio = item.prestador?.oficio,
                tituloServicio = item.servicio.titulo,
                disponible = FiltroCatalogo.disponibleAhora(item),
                onAbrirWhatsApp = { mensaje ->
                    val numero = Contacto.numeroWhatsApp(item.prestador?.telefono)
                    hoja = null
                    if (numero == null) {
                        avisar("Este prestador no tiene un número de WhatsApp válido.")
                    } else if (abrirWhatsApp(contexto, numero, mensaje)) {
                        actividad.registrarContacto(item.servicio.prestadorId, item.id, item.nombrePrestador, mensaje)
                        avisar("Abrimos WhatsApp con tu mensaje. Quedó en tu historial.")
                    } else {
                        avisar("No pudimos abrir WhatsApp en este teléfono.")
                    }
                },
                onCopiar = { mensaje ->
                    (contexto.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                        .setPrimaryClip(ClipData.newPlainText("Mensaje para ${item.nombrePrestador}", mensaje))
                    hoja = null
                    avisar("Mensaje copiado")
                },
                onCerrar = { hoja = null }
            )
            "opciones" -> HojaMasOpciones(
                nombre = item.nombrePrestador,
                onReportar = { hoja = "reportar" },
                onBloquear = { hoja = null; confirmarBloqueo = true },
                onCerrar = { hoja = null }
            )
            "reportar" -> HojaReportar(
                objetivo = "este servicio",
                onEnviar = { motivo, detalle ->
                    actividad.reportar("SERVICIO", item.id, item.servicio.titulo, motivo.titulo, detalle)
                    hoja = null
                    avisar("Reporte guardado en este teléfono.")
                },
                onCerrar = { hoja = null }
            )
            "calificar" -> HojaCalificar(
                nombre = item.nombrePrestador,
                enviando = ui.enviandoOpinion,
                onPublicar = { puntos, comentario -> viewModel.calificar(puntos, comentario) { hoja = null; pestana = 2 } },
                onCerrar = { hoja = null }
            )
        }
        if (confirmarBloqueo) {
            DialogoConfirmacion(
                titulo = "¿Bloquear a ${item.nombrePrestador}?",
                texto = "Dejarás de ver sus servicios y su perfil. Puedes desbloquearla desde tu perfil cuando quieras.",
                textoConfirmar = "Bloquear",
                peligroso = true,
                onConfirmar = {
                    confirmarBloqueo = false
                    actividad.bloquear(item.servicio.prestadorId, item.nombrePrestador)
                    onVolver()
                },
                onCancelar = { confirmarBloqueo = false }
            )
        }
    }

    fotoAbierta?.let { inicial ->
        VisorDeFotos(ui.fotos, inicial, onCerrar = { fotoAbierta = null })
    }
}

private fun abrirWhatsApp(contexto: Context, numero: String, mensaje: String): Boolean = try {
    contexto.startActivity(
        Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero?text=${Uri.encode(mensaje)}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
    true
} catch (e: ActivityNotFoundException) {
    false
}

private fun compartir(contexto: Context, titulo: String) {
    val envio = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Mira este servicio en Aruy: $titulo")
    }
    contexto.startActivity(Intent.createChooser(envio, "Compartir este servicio").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}

@Composable
private fun BarraContacto(nombre: String, tieneTelefono: Boolean, disponible: Boolean, onContactar: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainer, tonalElevation = 3.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!disponible) {
                Text(
                    "$nombre no recibe solicitudes ahora. Puedes dejarle un mensaje.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            BotonWhatsApp("Contactar por WhatsApp", onContactar, icono = Icons.AutoMirrored.Filled.Chat, habilitado = tieneTelefono)
            if (!tieneTelefono) {
                Text(
                    "El contacto por WhatsApp estará disponible cuando el servidor entregue el teléfono del prestador.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Galeria(
    fotos: List<FotoPortafolio>,
    categoria: ec.edu.puce.aruy.domain.model.CategoriaServicio,
    esFavorito: Boolean,
    onVolver: () -> Unit,
    onCompartir: () -> Unit,
    onFavorito: () -> Unit,
    onMas: () -> Unit,
    onAbrirFoto: (Int) -> Unit
) {
    val estado = rememberPagerState { maxOf(fotos.size, 1) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .background(MaterialTheme.colorScheme.primaryContainer)
    ) {
        HorizontalPager(state = estado, modifier = Modifier.fillMaxSize()) { pagina ->
            val foto = fotos.getOrNull(pagina)
            if (foto?.url != null) {
                AsyncImage(
                    model = foto.url,
                    contentDescription = foto.descripcion ?: "Foto ${pagina + 1} del trabajo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable { onAbrirFoto(pagina) }
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(categoria.icono, null, Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            BotonSobreFoto(Icons.AutoMirrored.Filled.ArrowBack, "Volver", onVolver)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonSobreFoto(Icons.Filled.Share, "Compartir este servicio", onCompartir)
                BotonSobreFoto(
                    if (esFavorito) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    if (esFavorito) "Quitar de favoritos" else "Guardar en favoritos",
                    onFavorito
                )
                BotonSobreFoto(Icons.Filled.MoreVert, "Más opciones", onMas)
            }
        }
        if (fotos.size > 1) {
            Surface(
                color = Color.Black.copy(alpha = 0.55f),
                shape = CircleShape,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                Text(
                    "${estado.currentPage + 1} / ${fotos.size}",
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun BotonSobreFoto(icono: androidx.compose.ui.graphics.vector.ImageVector, descripcion: String, onClick: () -> Unit) {
    Surface(color = Color.Black.copy(alpha = 0.45f), shape = CircleShape) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            Icon(icono, contentDescription = descripcion, tint = Color.White)
        }
    }
}

@Composable
private fun PestanaServicio(descripcion: String, categoria: String, zona: String?, sobrePrestador: String?, nombre: String) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        Text("Sobre este servicio", style = MaterialTheme.typography.titleLarge)
        Text(descripcion, style = MaterialTheme.typography.bodyLarge)
        Dato("Categoría", categoria)
        if (!zona.isNullOrBlank()) Dato("Zona", zona)
        if (!sobrePrestador.isNullOrBlank()) {
            Text("Sobre $nombre", style = MaterialTheme.typography.titleLarge)
            Text(sobrePrestador, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun Dato(titulo: String, valor: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("$titulo:", style = MaterialTheme.typography.labelLarge)
        Text(valor, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
internal fun PestanaTrabajos(fotos: List<FotoPortafolio>, onAbrirFoto: (Int) -> Unit) {
    if (fotos.isEmpty()) {
        EstadoVacio(
            titulo = "Todavía no hay fotos",
            texto = "Este prestador aún no subió fotos de sus trabajos.",
            icono = Icons.Filled.Close
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
        fotos.chunked(3).forEachIndexed { fila, grupo ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                grupo.forEachIndexed { columna, foto ->
                    AsyncImage(
                        model = foto.url,
                        contentDescription = foto.descripcion ?: "Foto del trabajo ${fila * 3 + columna + 1}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(MaterialTheme.shapes.medium)
                            .clickable { onAbrirFoto(fila * 3 + columna) }
                    )
                }
                repeat(3 - grupo.size) { Box(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
internal fun PestanaOpiniones(opiniones: ResumenOpiniones?, rol: RolNavegacion, onEscribir: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(top = 8.dp)) {
        if (opiniones == null || opiniones.total == 0) {
            EstadoVacio(
                titulo = "Aún no hay opiniones",
                texto = "Sé la primera persona en contar cómo le fue.",
                icono = Icons.Filled.FavoriteBorder
            )
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    opiniones.promedio?.let { String.format(java.util.Locale.forLanguageTag("es-EC"), "%.1f", it) } ?: "–",
                    style = MaterialTheme.typography.displayLarge
                )
                Column {
                    Calificacion(opiniones.promedio, opiniones.total)
                    Text(
                        "${opiniones.total} ${if (opiniones.total == 1) "opinión" else "opiniones"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            opiniones.opiniones.forEach { opinion ->
                AruyTarjeta(tipo = TipoTarjeta.CONTORNO, modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text(opinion.autor, style = MaterialTheme.typography.titleMedium)
                        Text("${opinion.puntuacion} ★", style = MaterialTheme.typography.labelLarge)
                    }
                    opinion.comentario?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
        if (rol != RolNavegacion.PRESTADOR) {
            BotonContorno("Escribir una opinión", onEscribir, ancho = true)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun VisorDeFotos(fotos: List<FotoPortafolio>, inicial: Int, onCerrar: () -> Unit) {
    Dialog(onDismissRequest = onCerrar, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val estado = rememberPagerState(initialPage = inicial.coerceIn(0, maxOf(fotos.size - 1, 0))) { fotos.size }
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(state = estado, modifier = Modifier.fillMaxSize()) { pagina ->
                AsyncImage(
                    model = fotos[pagina].url,
                    contentDescription = fotos[pagina].descripcion ?: "Foto ${pagina + 1} de ${fotos.size}",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
            IconButton(onClick = onCerrar, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(48.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color.White)
            }
            Text(
                "${estado.currentPage + 1} de ${fotos.size}",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}
