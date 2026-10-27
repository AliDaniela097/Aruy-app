package ec.edu.puce.aruy.util

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import ec.edu.puce.aruy.domain.catalogo.EtiquetaFoto
import ec.edu.puce.aruy.domain.catalogo.SugerenciaCategoria
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Etiquetas de IA con ML Kit Image Labeling. El modelo viene dentro de la app y la foto se analiza en
 * el teléfono, sin internet. Si falla, la foto se puede subir igual, solo que sin etiquetas.
 */
class EtiquetadorFotos {

    private val cliente = ImageLabeling.getClient(
        ImageLabelerOptions.Builder().setConfidenceThreshold(UMBRAL_CONFIANZA).build()
    )

    suspend fun etiquetar(bitmap: Bitmap): List<EtiquetaFoto> = suspendCancellableCoroutine { continuacion ->
        cliente.process(InputImage.fromBitmap(bitmap, 0))
            .addOnSuccessListener { resultado ->
                continuacion.resume(
                    resultado.sortedByDescending { it.confidence }
                        .take(MAX_ETIQUETAS)
                        .map { EtiquetaFoto(SugerenciaCategoria.traducir(it.text), it.confidence, it.text) }
                )
            }
            .addOnFailureListener { continuacion.resume(emptyList()) }
    }

    private companion object {
        /** Solo etiquetas en las que ML Kit confía al menos 70 %, como en el prototipo. */
        const val UMBRAL_CONFIANZA = 0.7f
        const val MAX_ETIQUETAS = 8
    }
}
