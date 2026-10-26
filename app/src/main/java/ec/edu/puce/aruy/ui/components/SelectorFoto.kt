package ec.edu.puce.aruy.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

/** Las dos formas de conseguir una foto: la cámara o la galería. */
class AccionesFoto(val tomar: () -> Unit, val elegir: () -> Unit)

/**
 * Prepara la cámara y la galería. La galería usa el selector de fotos del sistema, que no necesita
 * permisos. La cámara abre la app de cámara del teléfono y guarda la foto en un archivo temporal de
 * la caché (por eso la app no pide el permiso CAMERA). [alElegir] recibe la foto y si viene de la cámara.
 */
@Composable
fun rememberAccionesFoto(alElegir: (uri: Uri, desdeCamara: Boolean) -> Unit): AccionesFoto {
    val contexto = LocalContext.current
    val alElegirActual by rememberUpdatedState(alElegir)
    var uriCamara by rememberSaveable { mutableStateOf<String?>(null) }

    val galeria = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) alElegirActual(uri, false)
    }
    val camara = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { tomada ->
        val uri = uriCamara?.let(Uri::parse)
        uriCamara = null
        if (uri != null) {
            if (tomada) alElegirActual(uri, true) else runCatching { contexto.contentResolver.delete(uri, null, null) }
        }
    }

    return remember(contexto) {
        AccionesFoto(
            tomar = {
                val carpeta = File(contexto.cacheDir, "fotos").apply { mkdirs() }
                val archivo = File.createTempFile("foto_", ".jpg", carpeta)
                val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
                uriCamara = uri.toString()
                camara.launch(uri)
            },
            elegir = {
                galeria.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }
        )
    }
}

/** "Agregar foto": tomar una foto o elegirla de la galería. */
@Composable
fun HojaAgregarFoto(acciones: AccionesFoto, onCerrar: () -> Unit) {
    AruyHoja("Agregar foto", onCerrar) {
        Text(
            "Aruy reduce la foto, la convierte a WebP y la analiza en tu teléfono para sugerirte una categoría. Tú decides.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        BotonRelleno("Tomar foto", { onCerrar(); acciones.tomar() }, icono = Icons.Filled.PhotoCamera, grande = true, ancho = true)
        BotonContorno("Elegir de la galería", { onCerrar(); acciones.elegir() }, icono = Icons.Filled.PhotoLibrary, grande = true, ancho = true)
        BotonTexto("Ahora no", onCerrar)
    }
}
