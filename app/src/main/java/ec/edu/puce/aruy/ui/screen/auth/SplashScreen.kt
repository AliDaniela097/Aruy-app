package ec.edu.puce.aruy.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.MarcaAruy
import ec.edu.puce.aruy.ui.components.Tejido
import ec.edu.puce.aruy.ui.theme.AruyTheme
import kotlinx.coroutines.delay

/**
 * Splash de `auth.js`: marca, nombre y lema. Avanza solo (2,1 s; 0,5 s con "Reducir animaciones")
 * en cuanto se terminó de leer la sesión guardada, o al tocar "Continuar".
 */
@Composable
fun SplashScreen(estado: EstadoSesion, onContinuar: () -> Unit) {
    val listo = estado !is EstadoSesion.Cargando
    val espera = if (AruyTheme.reducirAnimaciones) 500L else 2100L

    LaunchedEffect(listo) {
        if (listo) {
            delay(espera)
            onContinuar()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            MarcaAruy(tamano = 112.dp)
            Text(
                "Aruy",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.padding(top = 24.dp)
            )
            Text(
                "Encuentra a quien necesitas, cerca de ti.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Tejido(Modifier.fillMaxWidth())
        BotonTexto("Continuar", onClick = { if (listo) onContinuar() }, habilitado = listo)
    }
}
