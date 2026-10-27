package ec.edu.puce.aruy.ui.screen.auth

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.data.repository.DatosRegistro
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.validation.Validaciones
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.AruyChip
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.viewmodel.AuthViewModel

private val ZONAS = listOf("Norte", "Centro", "Sur", "Valles")

/** Registro en 2 pasos (`auth.js`): 1) cómo usarás Aruy, 2) tus datos. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RegistroScreen(
    viewModel: AuthViewModel,
    onVolver: () -> Unit,
    onIniciarSesion: () -> Unit
) {
    val formulario by viewModel.formulario.collectAsState()

    var paso by rememberSaveable { mutableStateOf(1) }
    var rol by rememberSaveable { mutableStateOf(RolUsuario.CONTRATANTE) }
    var nombre by rememberSaveable { mutableStateOf("") }
    var correo by rememberSaveable { mutableStateOf("") }
    var celular by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var verPassword by rememberSaveable { mutableStateOf(false) }
    var oficio by rememberSaveable { mutableStateOf<CategoriaServicio?>(null) }
    var zona by rememberSaveable { mutableStateOf(ZONAS.first()) }
    var acepta by rememberSaveable { mutableStateOf(false) }

    var errorNombre by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCorreo by rememberSaveable { mutableStateOf<String?>(null) }
    var errorCelular by rememberSaveable { mutableStateOf<String?>(null) }
    var errorPassword by rememberSaveable { mutableStateOf<String?>(null) }
    var errorOficio by rememberSaveable { mutableStateOf<String?>(null) }
    var errorAcepta by rememberSaveable { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) { onDispose { viewModel.limpiarError() } }

    // Atrás en el paso 2 vuelve al paso 1, no sale del registro
    BackHandler(enabled = paso == 2) { paso = 1 }

    fun enviar() {
        errorNombre = Validaciones.nombre(nombre)
        errorCorreo = Validaciones.correo(correo)
        errorCelular = Validaciones.celular(celular)
        errorPassword = Validaciones.password(password)
        errorOficio = if (rol == RolUsuario.PRESTADOR && oficio == null) "Elige qué servicio ofreces." else null
        errorAcepta = if (!acepta) "Para crear tu cuenta acepta las normas." else null
        val hayError = listOf(errorNombre, errorCorreo, errorCelular, errorPassword, errorOficio, errorAcepta)
            .any { it != null }
        if (!hayError) {
            viewModel.registrar(
                DatosRegistro(
                    nombre = nombre,
                    correo = correo,
                    celular = celular,
                    password = password,
                    rol = rol,
                    oficio = oficio?.etiqueta,
                    zona = zona
                )
            ) {}
        }
    }

    Column(Modifier.fillMaxSize()) {
        BarraSuperior(
            titulo = if (paso == 1) "Crear cuenta" else if (rol == RolUsuario.CONTRATANTE) "Crea tu cuenta" else "Crea tu perfil",
            onVolver = { if (paso == 2) paso = 1 else onVolver() }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Indicador(paso)

            if (paso == 1) {
                Text("¿Cómo usarás Aruy?", style = MaterialTheme.typography.headlineMedium)
                TarjetaRol(
                    titulo = "Quiero contratar servicios",
                    texto = "Busca por categoría y zona, mira trabajos reales y escribe por WhatsApp.",
                    seleccionada = rol == RolUsuario.CONTRATANTE,
                    onClick = { rol = RolUsuario.CONTRATANTE }
                )
                TarjetaRol(
                    titulo = "Quiero ofrecer mis servicios",
                    texto = "Publica lo que haces, sube fotos de tus trabajos y recibe clientes cerca de ti.",
                    seleccionada = rol == RolUsuario.PRESTADOR,
                    onClick = { rol = RolUsuario.PRESTADOR }
                )
                BotonRelleno("Continuar", { paso = 2 }, grande = true, ancho = true)
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("¿Ya tienes cuenta?", style = MaterialTheme.typography.bodyMedium)
                    BotonTexto("Iniciar sesión", onIniciarSesion)
                }
            } else {
                AruyCampo(
                    valor = nombre,
                    onValor = { nombre = it; errorNombre = null },
                    etiqueta = "Nombre completo",
                    placeholder = "Ejemplo: Lucía Cevallos",
                    error = errorNombre,
                    maxCaracteres = Validaciones.NOMBRE_MAX,
                    teclado = KeyboardOptions(imeAction = ImeAction.Next)
                )
                AruyCampo(
                    valor = correo,
                    onValor = { correo = it; errorCorreo = null },
                    etiqueta = "Correo",
                    placeholder = "nombre@correo.com",
                    error = errorCorreo ?: formulario.errorCorreo,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next)
                )
                AruyCampo(
                    valor = celular,
                    onValor = { celular = it; errorCelular = null },
                    etiqueta = "Celular",
                    placeholder = "0998765432",
                    ayuda = "Las personas te contactan por este número en WhatsApp.",
                    error = errorCelular,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next)
                )
                AruyCampo(
                    valor = password,
                    onValor = { password = it; errorPassword = null },
                    etiqueta = "Contraseña",
                    placeholder = "Mínimo 8 caracteres",
                    error = errorPassword,
                    teclado = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
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
                MedidorPassword(password)

                if (rol == RolUsuario.PRESTADOR) {
                    Text("¿Qué servicio ofreces?", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoriaServicio.entries.forEach { categoria ->
                            AruyChip(
                                texto = categoria.etiqueta,
                                seleccionado = oficio == categoria,
                                onClick = { oficio = categoria; errorOficio = null }
                            )
                        }
                    }
                    errorOficio?.let {
                        Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                    }

                    Text("Tu zona en Quito", style = MaterialTheme.typography.titleMedium)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ZONAS.forEach { opcion ->
                            AruyChip(opcion, seleccionado = zona == opcion, onClick = { zona = opcion })
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(selected = acepta, role = Role.Checkbox, onClick = { acepta = !acepta; errorAcepta = null }),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = acepta, onCheckedChange = null, modifier = Modifier.padding(12.dp))
                    Text(
                        "Acepto las normas de la comunidad y el uso de mis datos según la política de privacidad.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                errorAcepta?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }

                formulario.error?.let { mensaje ->
                    Text(
                        mensaje,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                    )
                }

                BotonRelleno(
                    texto = if (formulario.cargando) "Creando tu cuenta…" else "Crear cuenta",
                    onClick = ::enviar,
                    grande = true,
                    ancho = true,
                    cargando = formulario.cargando
                )
            }
        }
    }
}

/** "Paso N de 2": dos barras de 6 dp. */
@Composable
private fun Indicador(paso: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Paso $paso de 2" },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        repeat(2) { indice ->
            Box(
                Modifier
                    .weight(1f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(
                        if (indice < paso) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceContainerHigh
                    )
            )
        }
    }
}

@Composable
private fun TarjetaRol(titulo: String, texto: String, seleccionada: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = seleccionada, role = Role.RadioButton, onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) MaterialTheme.colorScheme.secondaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = BorderStroke(
            if (seleccionada) 2.dp else 1.5.dp,
            if (seleccionada) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            RadioButton(selected = seleccionada, onClick = null)
            Column(Modifier.padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Medidor de 4 barras con el texto de [Validaciones.textoFuerzaPassword]. */
@Composable
private fun MedidorPassword(password: String) {
    val puntaje = Validaciones.fuerzaPassword(password)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(4) { indice ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            if (password.isNotEmpty() && indice < puntaje) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                )
            }
        }
        Text(
            Validaciones.textoFuerzaPassword(if (password.isEmpty()) 0 else puntaje),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
