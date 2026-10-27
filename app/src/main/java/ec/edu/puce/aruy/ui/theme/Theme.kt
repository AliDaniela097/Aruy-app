package ec.edu.puce.aruy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

/** Preferencia de tema de Ajustes: "Sistema", "Claro" u "Oscuro". */
enum class ModoTema { SISTEMA, CLARO, OSCURO }

/** Preferencia "Tamaño del texto" de Ajustes: Normal 100 %, Grande 112,5 %, Muy grande 131,25 %. */
enum class TamanoTexto(val escala: Float) {
    NORMAL(1f), GRANDE(1.125f), MUY_GRANDE(1.3125f)
}

/** Colores del prototipo que Material 3 no trae: éxito, estrella y botón de WhatsApp. */
@Immutable
data class ColoresAruy(
    val ok: Color,
    val okContainer: Color,
    val onOkContainer: Color,
    val estrella: Color,
    val whatsapp: Color,
    val whatsappPresionado: Color,
    val advertenciaContainer: Color,
    val onAdvertenciaContainer: Color
)

private val ColoresClaros = ColoresAruy(
    ok = Jade,
    okContainer = Color(0xFFCFF3E6),
    onOkContainer = Color(0xFF04392A),
    estrella = Color(0xFFB26A00),
    whatsapp = Color(0xFF15803D),
    whatsappPresionado = Color(0xFF166534),
    advertenciaContainer = ClaroTertiaryContainer,
    onAdvertenciaContainer = ClaroOnTertiaryContainer
)

private val ColoresOscuros = ColoresAruy(
    ok = Color(0xFF6FD9B6),
    okContainer = Color(0xFF0D4D3B),
    onOkContainer = Color(0xFFCFF3E6),
    estrella = Maiz,
    whatsapp = Color(0xFF15803D),
    whatsappPresionado = Color(0xFF166534),
    advertenciaContainer = OscuroTertiaryContainer,
    onAdvertenciaContainer = OscuroOnTertiaryContainer
)

private val EsquemaClaro = lightColorScheme(
    primary = ClaroPrimary,
    onPrimary = ClaroOnPrimary,
    primaryContainer = ClaroPrimaryContainer,
    onPrimaryContainer = ClaroOnPrimaryContainer,
    secondary = ClaroSecondary,
    onSecondary = ClaroOnSecondary,
    secondaryContainer = ClaroSecondaryContainer,
    onSecondaryContainer = ClaroOnSecondaryContainer,
    tertiary = Maiz,
    onTertiary = AnilNoche,
    tertiaryContainer = ClaroTertiaryContainer,
    onTertiaryContainer = ClaroOnTertiaryContainer,
    error = ClaroError,
    onError = ClaroOnError,
    errorContainer = ClaroErrorContainer,
    onErrorContainer = ClaroOnErrorContainer,
    background = ClaroSurface,
    onBackground = ClaroOnSurface,
    surface = ClaroSurface,
    onSurface = ClaroOnSurface,
    surfaceVariant = ClaroSurfaceHigh,
    onSurfaceVariant = ClaroOnSurfaceVariant,
    surfaceContainerLowest = ClaroSurfaceLowest,
    surfaceContainerLow = ClaroSurfaceLow,
    surfaceContainer = ClaroSurfaceContainer,
    surfaceContainerHigh = ClaroSurfaceHigh,
    surfaceContainerHighest = ClaroSurfaceHighest,
    outline = ClaroOutline,
    outlineVariant = ClaroOutlineVariant,
    inverseSurface = ClaroInverseSurface,
    inverseOnSurface = ClaroInverseOnSurface,
    inversePrimary = ClaroInversePrimary,
    scrim = Scrim
)

private val EsquemaOscuro = darkColorScheme(
    primary = OscuroPrimary,
    onPrimary = OscuroOnPrimary,
    primaryContainer = OscuroPrimaryContainer,
    onPrimaryContainer = OscuroOnPrimaryContainer,
    secondary = OscuroSecondary,
    onSecondary = OscuroOnSecondary,
    secondaryContainer = OscuroSecondaryContainer,
    onSecondaryContainer = OscuroOnSecondaryContainer,
    tertiary = Maiz,
    onTertiary = AnilNoche,
    tertiaryContainer = OscuroTertiaryContainer,
    onTertiaryContainer = OscuroOnTertiaryContainer,
    error = OscuroError,
    onError = OscuroOnError,
    errorContainer = OscuroErrorContainer,
    onErrorContainer = OscuroOnErrorContainer,
    background = OscuroSurface,
    onBackground = OscuroOnSurface,
    surface = OscuroSurface,
    onSurface = OscuroOnSurface,
    surfaceVariant = OscuroSurfaceHigh,
    onSurfaceVariant = OscuroOnSurfaceVariant,
    surfaceContainerLowest = OscuroSurfaceLowest,
    surfaceContainerLow = OscuroSurfaceLow,
    surfaceContainer = OscuroSurfaceContainer,
    surfaceContainerHigh = OscuroSurfaceHigh,
    surfaceContainerHighest = OscuroSurfaceHighest,
    outline = OscuroOutline,
    outlineVariant = OscuroOutlineVariant,
    inverseSurface = OscuroInverseSurface,
    inverseOnSurface = OscuroInverseOnSurface,
    inversePrimary = OscuroInversePrimary,
    scrim = Scrim
)

// Radios del prototipo: xs 4, s 8, m 12, l 16, xl 28
private val FormasAruy = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val LocalColoresAruy = staticCompositionLocalOf { ColoresClaros }
private val LocalReducirAnimaciones = staticCompositionLocalOf { false }

object AruyTheme {
    val colores: ColoresAruy
        @Composable @ReadOnlyComposable get() = LocalColoresAruy.current

    /** true si el usuario pidió "Reducir animaciones" en Ajustes. */
    val reducirAnimaciones: Boolean
        @Composable @ReadOnlyComposable get() = LocalReducirAnimaciones.current
}

@Composable
fun AruyTheme(
    modo: ModoTema = ModoTema.SISTEMA,
    tamanoTexto: TamanoTexto = TamanoTexto.NORMAL,
    reducirAnimaciones: Boolean = false,
    content: @Composable () -> Unit
) {
    val oscuro = when (modo) {
        ModoTema.SISTEMA -> isSystemInDarkTheme()
        ModoTema.CLARO -> false
        ModoTema.OSCURO -> true
    }
    val densidad = LocalDensity.current
    val densidadConTexto = Density(densidad.density, densidad.fontScale * tamanoTexto.escala)

    CompositionLocalProvider(
        LocalColoresAruy provides if (oscuro) ColoresOscuros else ColoresClaros,
        LocalReducirAnimaciones provides reducirAnimaciones,
        LocalDensity provides densidadConTexto
    ) {
        MaterialTheme(
            colorScheme = if (oscuro) EsquemaOscuro else EsquemaClaro,
            typography = Typography,
            shapes = FormasAruy,
            content = content
        )
    }
}
