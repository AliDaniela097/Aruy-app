package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.puce.aruy.ui.theme.AruyTheme
import ec.edu.puce.aruy.ui.theme.FuenteTitulos
import java.util.Locale

/** Variantes de tarjeta del prototipo (`card`, `card--outlined`, `--accent`, `--warn`, `--err`, `--ok`). */
enum class TipoTarjeta { NORMAL, CONTORNO, ACENTO, ADVERTENCIA, ERROR, OK }

@Composable
fun AruyTarjeta(
    modifier: Modifier = Modifier,
    tipo: TipoTarjeta = TipoTarjeta.NORMAL,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colores = AruyTheme.colores
    val esquema = MaterialTheme.colorScheme
    val (fondo, borde) = when (tipo) {
        TipoTarjeta.NORMAL -> esquema.surfaceContainerLow to null
        TipoTarjeta.CONTORNO -> esquema.surface to BorderStroke(1.5.dp, esquema.outlineVariant)
        TipoTarjeta.ACENTO -> esquema.primaryContainer to null
        TipoTarjeta.ADVERTENCIA -> colores.advertenciaContainer to null
        TipoTarjeta.ERROR -> esquema.errorContainer to null
        TipoTarjeta.OK -> colores.okContainer to null
    }
    val colorCards = CardDefaults.cardColors(containerColor = fondo)
    val forma = MaterialTheme.shapes.large
    val relleno = Modifier.padding(16.dp)
    if (onClick != null) {
        Card(
            onClick = onClick, modifier = modifier, shape = forma, colors = colorCards, border = borde
        ) { Column(relleno, content = content) }
    } else {
        Card(modifier = modifier, shape = forma, colors = colorCards, border = borde) {
            Column(relleno, content = content)
        }
    }
}

/** Chip de filtro de 48 dp; activo = fondo `secondaryContainer` con marca de verificación. */
@Composable
fun AruyChip(
    texto: String,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(texto, style = MaterialTheme.typography.labelLarge) },
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = seleccionado,
            borderColor = MaterialTheme.colorScheme.outline,
            selectedBorderColor = Color.Transparent,
            borderWidth = 1.5.dp
        )
    )
}

enum class TipoPastilla { NEUTRA, OK, INFO, ADVERTENCIA, ERROR, ROSA }

/** Pastilla informativa: "Publicado", "En revisión", "Pausado", "Prestador"... */
@Composable
fun Pastilla(texto: String, modifier: Modifier = Modifier, tipo: TipoPastilla = TipoPastilla.NEUTRA) {
    val colores = AruyTheme.colores
    val esquema = MaterialTheme.colorScheme
    val (fondo, tinta) = when (tipo) {
        TipoPastilla.NEUTRA -> esquema.surfaceContainerHigh to esquema.onSurfaceVariant
        TipoPastilla.OK -> colores.okContainer to colores.onOkContainer
        TipoPastilla.INFO -> esquema.primaryContainer to esquema.onPrimaryContainer
        TipoPastilla.ADVERTENCIA -> colores.advertenciaContainer to colores.onAdvertenciaContainer
        TipoPastilla.ERROR -> esquema.errorContainer to esquema.onErrorContainer
        TipoPastilla.ROSA -> esquema.secondaryContainer to esquema.onSecondaryContainer
    }
    Surface(color = fondo, contentColor = tinta, shape = CircleShape, modifier = modifier) {
        Text(
            texto,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/** Avatar con iniciales y 5 tonos (`avatar` + `data-tone`). El tono sale del texto, así es estable. */
@Composable
fun AruyAvatar(nombre: String, modifier: Modifier = Modifier, tamano: Dp = 48.dp) {
    val esquema = MaterialTheme.colorScheme
    val colores = AruyTheme.colores
    val tonos = listOf(
        esquema.primaryContainer to esquema.onPrimaryContainer,
        esquema.secondaryContainer to esquema.onSecondaryContainer,
        colores.okContainer to colores.onOkContainer,
        colores.advertenciaContainer to colores.onAdvertenciaContainer,
        esquema.surfaceContainerHigh to esquema.onSurface
    )
    val (fondo, tinta) = tonos[Math.floorMod(nombre.hashCode(), tonos.size)]
    Box(
        modifier = modifier
            .size(tamano)
            .clip(CircleShape)
            .background(fondo)
            .semantics { contentDescription = nombre },
        contentAlignment = Alignment.Center
    ) {
        Text(
            iniciales(nombre),
            color = tinta,
            fontFamily = FuenteTitulos,
            fontWeight = FontWeight.Bold,
            fontSize = (tamano.value * 0.38f).sp
        )
    }
}

/** "Lucía Cevallos" -> "LC". */
fun iniciales(nombre: String): String =
    nombre.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.take(2)
        .joinToString("") { it.first().toString() }
        .uppercase(Locale.ROOT)
        .ifEmpty { "?" }

/** Calificación con estrella: "4,8 (30)" o "Nuevo en Aruy" cuando aún no hay opiniones. */
@Composable
fun Calificacion(promedio: Double?, total: Int, modifier: Modifier = Modifier) {
    if (promedio == null || total == 0) {
        Pastilla("Nuevo en Aruy", modifier, TipoPastilla.INFO)
        return
    }
    val texto = String.format(Locale.forLanguageTag("es-EC"), "%.1f", promedio)
    Row(
        modifier = modifier.semantics { contentDescription = "Calificación $texto de 5, $total opiniones" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(Icons.Filled.Star, null, tint = AruyTheme.colores.estrella, modifier = Modifier.size(18.dp))
        Text(texto, style = MaterialTheme.typography.labelLarge)
        Text("($total)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Sello de "Cuenta verificada". */
@Composable
fun SelloVerificado(modifier: Modifier = Modifier, conTexto: Boolean = false) {
    Row(
        modifier = modifier.semantics { contentDescription = "Cuenta verificada" },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Filled.Verified, null, tint = AruyTheme.colores.ok, modifier = Modifier.size(20.dp))
        if (conTexto) {
            Spacer(Modifier.width(4.dp))
            Text("Cuenta verificada", style = MaterialTheme.typography.labelMedium, color = AruyTheme.colores.ok)
        }
    }
}

/** Título de sección con acción opcional ("Ver todos"). */
@Composable
fun TituloSeccion(texto: String, modifier: Modifier = Modifier, accion: (@Composable () -> Unit)? = null) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(texto, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { })
        accion?.invoke()
    }
}
