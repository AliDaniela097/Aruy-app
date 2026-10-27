package ec.edu.puce.aruy

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import ec.edu.puce.aruy.util.EtiquetadorFotos
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** ML Kit Image Labeling trae su modelo dentro de la app: debe responder en el teléfono, sin internet. */
@RunWith(AndroidJUnit4::class)
class EtiquetadorFotosInstrumentedTest {

    @Test
    fun el_modelo_incluido_responde_sin_colgarse_y_respeta_el_umbral() = runBlocking {
        val bitmap = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {
            drawColor(Color.rgb(200, 190, 170))
            drawRect(40f, 40f, 280f, 200f, Paint().apply { color = Color.rgb(120, 80, 40) })
        }

        val etiquetas = withTimeout(60_000) { EtiquetadorFotos().etiquetar(bitmap) }

        // Una imagen sintética puede no parecerse a nada conocido: lo importante es que responda
        assertTrue(etiquetas.size <= 8)
        assertTrue(etiquetas.all { it.confianza >= 0.7f })
    }
}
