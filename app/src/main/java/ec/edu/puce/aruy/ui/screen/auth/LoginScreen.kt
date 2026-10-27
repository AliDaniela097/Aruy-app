package ec.edu.puce.aruy.ui.screen.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.validation.Validaciones
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onVolver: () -> Unit,
    onCrearCuenta: () -> Unit
) {
    val formulario by viewModel.formulario.collectAsState()
    val aviso by viewModel.aviso.collectAsState()

    var correo by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var verPassword by rememberSaveable { mutableStateOf(false) }
    var errorCorreo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorPassword by rememberSaveable { mutableStateOf<String?>(null) }

    // Los errores del servidor no deben sobrevivir al salir de la pantalla
    DisposableEffect(Unit) { onDispose { viewModel.limpiarError() } }

    fun enviar() {
        errorCorreo = Validaciones.correo(correo)
        errorPassword = if (password.isEmpty()) "Escribe tu contraseña." else null
        if (errorCorreo == null && errorPassword == null) {
            viewModel.avisoMostrado()
            viewModel.iniciarSesion(correo, password) {}
        }
    }

    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Iniciar sesión", onVolver)
        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Entra con tu correo y tu contraseña.", style = MaterialTheme.typography.bodyLarge)

            aviso?.let { mensaje ->
                AruyTarjeta(tipo = TipoTarjeta.ADVERTENCIA, modifier = Modifier.fillMaxWidth()) {
                    Text(mensaje, style = MaterialTheme.typography.bodyMedium)
                }
            }

            AruyCampo(
                valor = correo,
                onValor = { correo = it; errorCorreo = null },
                etiqueta = "Correo",
                placeholder = "nombre@correo.com",
                error = errorCorreo,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
            )
            AruyCampo(
                valor = password,
                onValor = { password = it; errorPassword = null },
                etiqueta = "Contraseña",
                placeholder = "Tu contraseña",
                error = errorPassword,
                teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                acciones = KeyboardActions(onDone = { enviar() }),
                transformacion = if (verPassword) VisualTransformation.None else PasswordVisualTransformation(),
                iconoFinal = {
                    IconButton(onClick = { verPassword = !verPassword }) {
                        Icon(
                            if (verPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (verPassword) "Ocultar contraseña" else "Mostrar contraseña"
                        )
                    }
                }
            )

            formulario.error?.let { mensaje ->
                Text(
                    mensaje,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )
            }

            BotonRelleno(
                texto = if (formulario.cargando) "Entrando…" else "Iniciar sesión",
                onClick = ::enviar,
                grande = true,
                ancho = true,
                cargando = formulario.cargando
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿No tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
                BotonTexto("Crear cuenta", onCrearCuenta)
            }
        }
    }
}
