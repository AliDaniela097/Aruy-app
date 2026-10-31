package ec.edu.puce.aruy.ui.screen.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.validation.ValidacionesPerfil
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonPeligro
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.viewmodel.BajaCuentaViewModel

/**
 * Eliminar la cuenta. El backend borra la cuenta y todos sus datos (Google Play no acepta solo
 * desactivarla) y la app borra lo que guardaba de esa cuenta en el teléfono.
 */
@Composable
fun BajaCuentaScreen(viewModel: BajaCuentaViewModel, onVolver: () -> Unit) {
    val ui by viewModel.ui.collectAsState()

    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Eliminar mi cuenta", onVolver)
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AruyTarjeta(tipo = TipoTarjeta.ERROR, modifier = Modifier.fillMaxWidth()) {
                Text("Esto elimina tu cuenta para siempre", style = MaterialTheme.typography.titleMedium)
                Text("No se puede deshacer.", style = MaterialTheme.typography.bodyMedium)
            }
            AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
                Text("Qué se borra", style = MaterialTheme.typography.titleMedium)
                Text("• Tu cuenta: nombre, celular, correo y contraseña.", style = MaterialTheme.typography.bodyMedium)
                Text("• Si eres prestador: tu perfil, tus servicios y sus fotos.", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "• Las opiniones que hiciste y las que recibiste, y tu plan Aruy Pro.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• Tus favoritos, bloqueados, reportes e historial guardados en este teléfono.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            AruyCampo(
                valor = ui.confirmacion,
                onValor = viewModel::cambiarConfirmacion,
                etiqueta = "Para confirmar, escribe ${ValidacionesPerfil.PALABRA_BAJA}",
                error = ui.error,
                teclado = KeyboardOptions(imeAction = ImeAction.Done),
                acciones = KeyboardActions(onDone = { viewModel.eliminarCuenta() })
            )
            BotonPeligro(
                texto = if (ui.procesando) "Eliminando…" else "Eliminar mi cuenta",
                onClick = viewModel::eliminarCuenta,
                ancho = true,
                cargando = ui.procesando
            )
            BotonTexto("Mejor no", onVolver, modifier = Modifier.fillMaxWidth())
        }
    }
}
