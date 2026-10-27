package ec.edu.puce.aruy.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

/**
 * Hoja que ve una persona invitada al tocar un servicio, un perfil o un favorito
 * ("Crea tu cuenta para ver los detalles", `flows.js` del prototipo).
 */
@Composable
fun HojaCuentaRequerida(
    onCrearCuenta: () -> Unit,
    onIniciarSesion: () -> Unit,
    onCerrar: () -> Unit
) {
    AruyHoja(titulo = "Crea tu cuenta para ver los detalles", onCerrar = onCerrar) {
        Text(
            "Con tu cuenta ves precios, calificaciones y fotos de trabajos, guardas favoritos " +
                "y contactas por WhatsApp.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BotonRelleno("Crear cuenta", onCrearCuenta, grande = true, ancho = true)
        BotonContorno("Iniciar sesión", onIniciarSesion, grande = true, ancho = true)
        BotonTexto("Seguir explorando", onCerrar)
    }
}
