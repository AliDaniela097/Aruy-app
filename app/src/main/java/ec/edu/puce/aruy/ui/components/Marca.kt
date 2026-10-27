package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ec.edu.puce.aruy.ui.theme.Anil
import ec.edu.puce.aruy.ui.theme.FuenteTitulos
import ec.edu.puce.aruy.ui.theme.Maiz

/** Marca de Aruy (assets/icon.svg del prototipo): cuadro anil, "A" blanca y barra de maíz cosida. */
@Composable
fun MarcaAruy(modifier: Modifier = Modifier, tamano: Dp = 96.dp) {
    Box(
        modifier = modifier
            .size(tamano)
            .clip(RoundedCornerShape(tamano * 0.22f))
            .background(Anil)
            .semantics { contentDescription = "Aruy" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            "A",
            color = androidx.compose.ui.graphics.Color.White,
            fontFamily = FuenteTitulos,
            fontWeight = FontWeight.ExtraBold,
            fontSize = (tamano.value * 0.58f).sp,
            modifier = Modifier.offset(y = -(tamano * 0.04f))
        )
        Box(
            Modifier
                .align(Alignment.BottomCenter)
                .offset(y = -(tamano * 0.2f))
                .width(tamano * 0.46f)
                .height(tamano * 0.07f)
                .background(Maiz)
        )
    }
}

/** Barra superior con botón de volver, para pantallas fuera de la barra inferior. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarraSuperior(titulo: String, onVolver: (() -> Unit)?, modifier: Modifier = Modifier) {
    CenterAlignedTopAppBar(
        title = { Text(titulo, style = MaterialTheme.typography.titleLarge) },
        modifier = modifier.fillMaxWidth(),
        navigationIcon = {
            if (onVolver != null) {
                IconButton(onClick = onVolver, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
    )
}
