package ec.edu.puce.aruy.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.MarcaAruy
import ec.edu.puce.aruy.ui.components.Tejido

/** Bienvenida: crear cuenta, iniciar sesión o explorar sin cuenta (`auth.js`). */
@Composable
fun BienvenidaScreen(
    onCrearCuenta: () -> Unit,
    onIniciarSesion: () -> Unit,
    onExplorar: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Tejido(Modifier.fillMaxWidth())
        MarcaAruy(tamano = 56.dp, modifier = Modifier.padding(top = 16.dp))
        Text(
            "Aruy",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            "Encuentra a quien necesitas, cerca de ti.",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            "Conecta a personas que ofrecen un servicio con quienes lo necesitan en Quito: " +
                "plomería, electricidad, belleza y más. Ves su trabajo y los contactas directo por WhatsApp.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(
            modifier = Modifier.padding(top = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "¿Quieres crear una cuenta o ya tienes una?",
                style = MaterialTheme.typography.titleMedium
            )
            BotonRelleno("Crear cuenta", onCrearCuenta, grande = true, ancho = true)
            BotonContorno("Iniciar sesión", onIniciarSesion, grande = true, ancho = true)
            BotonTexto("Explorar sin cuenta", onExplorar)
        }
    }
}
