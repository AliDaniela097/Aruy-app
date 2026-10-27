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
 * Darse de baja. El backend no borra la cuenta (la base lo impide si hay servicios u opiniones): la deja
 * inactiva. La pantalla lo dice tal cual, sin prometer que se borren los datos.
 */
@Composable
fun BajaCuentaScreen(viewModel: BajaCuentaViewModel, onVolver: () -> Unit) {
    val ui by viewModel.ui.collectAsState()

    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Dar de baja mi cuenta", onVolver)
        Column(
            Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            AruyTarjeta(tipo = TipoTarjeta.ERROR, modifier = Modifier.fillMaxWidth()) {
                Text("Esto desactiva tu cuenta", style = MaterialTheme.typography.titleMedium)
                Text("No se puede deshacer desde la app.", style = MaterialTheme.typography.bodyMedium)
            }
            AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
                Text("Qué pasa", style = MaterialTheme.typography.titleMedium)
                Text("• No podrás volver a iniciar sesión con esta cuenta.", style = MaterialTheme.typography.bodyMedium)
                Text("• Si eres prestador, tu perfil queda como no disponible.", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "• Tus servicios, fotos y opiniones no se borran del servidor.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "• Tus favoritos y tu historial siguen guardados en este teléfono.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            AruyCampo(
                valor = ui.confirmacion,
                onValor = viewModel::cambiarConfirmacion,
                etiqueta = "Para confirmar, escribe ${ValidacionesPerfil.PALABRA_BAJA}",
                error = ui.error,
                teclado = KeyboardOptions(imeAction = ImeAction.Done),
                acciones = KeyboardActions(onDone = { viewModel.darDeBaja() })
            )
            BotonPeligro(
                texto = if (ui.procesando) "Dando de baja…" else "Dar de baja mi cuenta",
                onClick = viewModel::darDeBaja,
                ancho = true,
                cargando = ui.procesando
            )
            BotonTexto("Mejor no", onVolver, modifier = Modifier.fillMaxWidth())
        }
    }
}
