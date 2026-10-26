package ec.edu.puce.aruy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import ec.edu.puce.aruy.R

private val pesos = listOf(
    FontWeight.Normal,
    FontWeight.Medium,
    FontWeight.SemiBold,
    FontWeight.Bold,
    FontWeight.ExtraBold
)

// Fuentes variables incluidas en la app (licencia OFL): funcionan sin conexión
@OptIn(ExperimentalTextApi::class)
val FuenteTitulos = FontFamily(
    pesos.map { peso ->
        Font(
            resId = R.font.bricolage,
            weight = peso,
            variationSettings = FontVariation.Settings(
                FontVariation.weight(peso.weight),
                FontVariation.opticalSizing(28.sp)
            )
        )
    }
)

@OptIn(ExperimentalTextApi::class)
val FuenteTexto = FontFamily(
    pesos.map { peso ->
        Font(
            resId = R.font.atkinson,
            weight = peso,
            variationSettings = FontVariation.Settings(FontVariation.weight(peso.weight))
        )
    }
)

// Escala del prototipo (t-display ... t-label-m, css/base.css)
val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = FuenteTitulos, fontWeight = FontWeight.Bold,
        fontSize = 32.sp, lineHeight = 36.sp, letterSpacing = (-0.02).em
    ),
    headlineMedium = TextStyle(
        fontFamily = FuenteTitulos, fontWeight = FontWeight.Bold,
        fontSize = 24.sp, lineHeight = 29.sp, letterSpacing = (-0.012).em
    ),
    titleLarge = TextStyle(
        fontFamily = FuenteTitulos, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FuenteTexto, fontWeight = FontWeight.Bold,
        fontSize = 16.sp, lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FuenteTexto, fontWeight = FontWeight.Normal,
        fontSize = 16.sp, lineHeight = 25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FuenteTexto, fontWeight = FontWeight.Normal,
        fontSize = 15.sp, lineHeight = 22.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FuenteTexto, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FuenteTexto, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 18.sp
    )
)
