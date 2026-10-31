package ec.edu.puce.aruy.ui.screen.perfil

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import ec.edu.puce.aruy.ui.components.AruyInterruptor
import ec.edu.puce.aruy.ui.components.AruySegmentado
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.BotonPeligro
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.theme.ModoTema
import ec.edu.puce.aruy.ui.theme.TamanoTexto
import ec.edu.puce.aruy.ui.viewmodel.AjustesViewModel
import kotlinx.coroutines.launch

@Composable
private fun Subtitulo(texto: String) {
    Text(texto, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

/** Ajustes: tema, tamaño del texto, animaciones y caché de fotos (`ajustes` en `ajustes.js`). */
@OptIn(ExperimentalCoilApi::class)
@Composable
fun AjustesScreen(viewModel: AjustesViewModel, onVolver: () -> Unit) {
    val preferencias by viewModel.preferencias.collectAsState()
    val contexto = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val alcance = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BarraSuperior("Ajustes", onVolver)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Subtitulo("Tema")
                AruySegmentado(
                    opciones = listOf("Sistema", "Claro", "Oscuro"),
                    seleccionada = preferencias.modoTema.ordinal,
                    onSeleccion = { viewModel.elegirTema(ModoTema.entries[it]) }
                )
                Text(
                    "«Sistema» sigue el modo claro u oscuro de tu teléfono.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Subtitulo("Tamaño del texto")
                AruySegmentado(
                    opciones = listOf("Normal", "Grande", "Muy grande"),
                    seleccionada = preferencias.tamanoTexto.ordinal,
                    onSeleccion = { viewModel.elegirTamanoTexto(TamanoTexto.entries[it]) }
                )
                AruyTarjeta(tipo = TipoTarjeta.CONTORNO, modifier = Modifier.fillMaxWidth()) {
                    Text("Así se verá el texto en Aruy.", style = MaterialTheme.typography.bodyLarge)
                }

                Subtitulo("Movimiento")
                AruyInterruptor(
                    titulo = "Reducir animaciones",
                    activado = preferencias.reducirAnimaciones,
                    onCambio = viewModel::reducirAnimaciones,
                    descripcion = "Quita los brillos y movimientos de las pantallas de carga."
                )

                Subtitulo("Datos guardados")
                BotonContorno(
                    "Borrar fotos en caché",
                    onClick = {
                        contexto.imageLoader.memoryCache?.clear()
                        contexto.imageLoader.diskCache?.clear()
                        alcance.launch { snackbar.avisar("Listo. La próxima vez se descargan de nuevo.") }
                    },
                    ancho = true
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

/**
 * Permisos. Aruy no pide ninguno: la cámara se abre con la app de cámara del teléfono y las fotos se
 * eligen con el selector del sistema. Esta pantalla lo explica y lleva a los ajustes del teléfono.
 */
@Composable
fun PermisosScreen(onVolver: () -> Unit) {
    val contexto = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Permisos", onVolver)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Aruy no necesita permisos especiales.", style = MaterialTheme.typography.bodyLarge)
            TarjetaPermiso("Cámara", "Para la foto de un trabajo se abre la app de cámara de tu teléfono. Aruy no tiene acceso a la cámara por su cuenta.")
            TarjetaPermiso("Galería", "Eliges las fotos con el selector del sistema: Aruy solo ve las que tú escoges, no toda tu galería.")
            TarjetaPermiso("Ubicación", "Aruy no usa tu ubicación.")
            BotonContorno(
                "Abrir ajustes del teléfono",
                onClick = {
                    contexto.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", contexto.packageName, null))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                },
                ancho = true
            )
        }
    }
}

@Composable
private fun TarjetaPermiso(titulo: String, texto: String) {
    AruyTarjeta(tipo = TipoTarjeta.CONTORNO, modifier = Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}

/** Mis datos y privacidad: qué se guarda y dónde. */
@Composable
fun PrivacidadScreen(onVolver: () -> Unit, onPermisos: () -> Unit, onBaja: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Mis datos y privacidad", onVolver)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AruyTarjeta(tipo = TipoTarjeta.ACENTO, modifier = Modifier.fillMaxWidth()) {
                Text("Tu información es tuya", style = MaterialTheme.typography.titleMedium)
                Text("Aquí ves qué guarda Aruy y dónde.", style = MaterialTheme.typography.bodyMedium)
            }
            Dato(
                "Tu cuenta",
                "Tu nombre, celular y correo se guardan en el servidor de Aruy. La contraseña se guarda cifrada y nunca se vuelve a mostrar."
            )
            Dato(
                "En este teléfono",
                "Tus favoritos, personas bloqueadas, historial de contactos, reportes y el borrador de un servicio. " +
                    "La sesión se guarda cifrada y no se copia a respaldos."
            )
            Dato(
                "Tus fotos",
                "Las fotos de tus servicios se reducen y se convierten a WebP en tu teléfono, y se guardan en el almacenamiento de Aruy " +
                    "sin la ubicación ni los datos de la cámara. El análisis de etiquetas ocurre en tu teléfono, sin internet."
            )
            Dato("Tu ubicación", "Aruy no la usa ni la guarda.")
            BotonContorno("Revisar permisos", onPermisos, ancho = true)
            BotonPeligro("Eliminar mi cuenta", onBaja, soloTexto = true)
        }
    }
}

@Composable
private fun Dato(titulo: String, texto: String) {
    AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
        Text(titulo, style = MaterialTheme.typography.titleMedium)
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}
