package ec.edu.puce.aruy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

/** Campo de texto del prototipo: etiqueta arriba, 56 dp mínimo, error en rojo debajo. */
@Composable
fun AruyCampo(
    valor: String,
    onValor: (String) -> Unit,
    etiqueta: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    ayuda: String? = null,
    error: String? = null,
    unaLinea: Boolean = true,
    lineasMin: Int = 1,
    maxCaracteres: Int? = null,
    teclado: KeyboardOptions = KeyboardOptions.Default,
    acciones: KeyboardActions = KeyboardActions.Default,
    transformacion: VisualTransformation = VisualTransformation.None,
    iconoFinal: (@Composable () -> Unit)? = null,
    habilitado: Boolean = true
) {
    OutlinedTextField(
        value = valor,
        onValueChange = { nuevo -> if (maxCaracteres == null || nuevo.length <= maxCaracteres) onValor(nuevo) },
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
        label = { Text(etiqueta) },
        placeholder = placeholder?.let { { Text(it) } },
        // Sin mensaje no se reserva la línea de apoyo: así los campos no quedan separados de más
        supportingText = (error ?: ayuda)?.let { mensaje -> { Text(mensaje) } },
        isError = error != null,
        singleLine = unaLinea,
        minLines = lineasMin,
        keyboardOptions = teclado,
        keyboardActions = acciones,
        visualTransformation = transformacion,
        trailingIcon = iconoFinal,
        enabled = habilitado,
        shape = MaterialTheme.shapes.medium
    )
}

/** Fila de interruptor con título y descripción, toda la fila es táctil (48 dp mínimo). */
@Composable
fun AruyInterruptor(
    titulo: String,
    activado: Boolean,
    onCambio: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    descripcion: String? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = activado, role = Role.Switch, onValueChange = onCambio)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            if (descripcion != null) {
                Text(
                    descripcion,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(checked = activado, onCheckedChange = null)
    }
}

/** Control segmentado de una sola opción: "Lista | Mapa", "Sistema | Claro | Oscuro". */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AruySegmentado(
    opciones: List<String>,
    seleccionada: Int,
    onSeleccion: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        opciones.forEachIndexed { indice, texto ->
            SegmentedButton(
                selected = indice == seleccionada,
                onClick = { onSeleccion(indice) },
                shape = SegmentedButtonDefaults.itemShape(indice, opciones.size),
                modifier = Modifier.heightIn(min = 48.dp).semantics { }
            ) { Text(texto) }
        }
    }
}
