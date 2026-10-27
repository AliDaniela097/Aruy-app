package ec.edu.puce.aruy.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.ui.theme.AruyTheme

/** "Sin conexión" si falló la red; si no, el título general de error (textos del prototipo). */
fun tituloDeError(mensaje: String, general: String = "No pudimos cargar"): String =
    if (ec.edu.puce.aruy.data.remote.util.ApiErrorHandler.esDeRed(mensaje)) "Sin conexión" else general

/** Estado vacío del prototipo: ícono, título, texto y botón opcional. */
@Composable
fun EstadoVacio(
    titulo: String,
    texto: String,
    icono: ImageVector,
    modifier: Modifier = Modifier,
    textoBoton: String? = null,
    onBoton: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(112.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Text(titulo, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (textoBoton != null && onBoton != null) BotonRelleno(textoBoton, onBoton)
    }
}

/** Bloque gris con brillo para las cargas ("esqueleto" del prototipo). */
@Composable
fun Esqueleto(modifier: Modifier = Modifier, alto: Dp = 16.dp) {
    val reducir = AruyTheme.reducirAnimaciones
    val base = MaterialTheme.colorScheme.surfaceContainerHigh
    val brillo = MaterialTheme.colorScheme.surfaceContainerHighest
    val color = if (reducir) {
        base
    } else {
        val transicion = rememberInfiniteTransition(label = "esqueleto")
        val mezcla = transicion.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
            label = "brillo"
        )
        androidx.compose.ui.graphics.lerp(base, brillo, mezcla.value)
    }
    Box(
        modifier
            .height(alto)
            .clip(MaterialTheme.shapes.small)
            .background(color)
            .semantics { contentDescription = "Cargando" }
    )
}

/** Pantalla completa de carga con texto, por ejemplo "Cargando servicios cerca de ti…". */
@Composable
fun EstadoCargando(texto: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Tejido(Modifier.fillMaxWidth(0.5f))
        Text(
            texto,
            modifier = Modifier.padding(top = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * "Tejido": banda de rombos escalonados con los colores de la marca.
 * Se usa en splash, bienvenida y cargas, como en el prototipo (components.css `tejido`).
 */
@Composable
fun Tejido(modifier: Modifier = Modifier, alto: Dp = 12.dp) {
    val primario = MaterialTheme.colorScheme.primary
    val secundario = MaterialTheme.colorScheme.secondary
    Canvas(modifier.height(alto)) {
        val lado = size.height
        var x = 0f
        var indice = 0
        while (x < size.width) {
            val color: Color = if (indice % 2 == 0) primario else secundario
            val rombo = Path().apply {
                moveTo(x + lado / 2, 0f)
                lineTo(x + lado, lado / 2)
                lineTo(x + lado / 2, lado)
                lineTo(x, lado / 2)
                close()
            }
            drawPath(rombo, color)
            x += lado
            indice++
        }
    }
}
