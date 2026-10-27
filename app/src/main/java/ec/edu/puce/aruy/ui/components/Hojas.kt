package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Hoja inferior del prototipo (`AR.sheet`): se cierra arrastrando, tocando fuera o con Atrás. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AruyHoja(
    titulo: String?,
    onCerrar: () -> Unit,
    content: @Composable () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onCerrar,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = MaterialTheme.shapes.extraLarge.copy(
            bottomStart = androidx.compose.foundation.shape.CornerSize(0.dp),
            bottomEnd = androidx.compose.foundation.shape.CornerSize(0.dp)
        )
    ) {
        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (titulo != null) Text(titulo, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

/** Diálogo de confirmación: "¿Eliminar «Instalación de breakers»?" con Cancelar y acción. */
@Composable
fun DialogoConfirmacion(
    titulo: String,
    texto: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    textoCancelar: String = "Cancelar",
    peligroso: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(titulo) },
        text = { Text(texto) },
        confirmButton = {
            if (peligroso) BotonPeligro(textoConfirmar, onConfirmar, soloTexto = true)
            else BotonTexto(textoConfirmar, onConfirmar)
        },
        dismissButton = { BotonTexto(textoCancelar, onCancelar) }
    )
}

/**
 * Muestra un aviso con acción opcional ("Deshacer"). Devuelve true si el usuario tocó la acción.
 */
suspend fun SnackbarHostState.avisar(mensaje: String, accion: String? = null): Boolean =
    showSnackbar(
        message = mensaje,
        actionLabel = accion,
        duration = if (accion != null) SnackbarDuration.Long else SnackbarDuration.Short
    ) == SnackbarResult.ActionPerformed
