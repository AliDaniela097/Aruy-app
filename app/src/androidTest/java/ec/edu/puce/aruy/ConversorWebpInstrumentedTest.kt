package ec.edu.puce.aruy

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import ec.edu.puce.aruy.util.ConversorWebp
import ec.edu.puce.aruy.util.ErrorFoto
import ec.edu.puce.aruy.util.PlanWebp
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.Random
import kotlin.math.max

/**
 * Prueba en un teléfono o emulador real lo que no se puede probar en la JVM: que una foto de
 * cámara (JPG grande, con orientación EXIF) o un PNG salga como WebP de máximo 1280 px y 500 KB.
 */
@RunWith(AndroidJUnit4::class)
class ConversorWebpInstrumentedTest {

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext
    private val conversor = ConversorWebp(contexto)
    private val archivos = mutableListOf<File>()

    @Before
    fun preparar() {
        File(contexto.cacheDir, "pruebas").mkdirs()
    }

    @After
    fun limpiar() {
        archivos.forEach { it.delete() }
    }

    /** Degradado con un poco de ruido: parecido a una foto, sin ser un color plano. */
    private fun imagen(ancho: Int, alto: Int, ruido: Boolean = false): Bitmap {
        val azar = Random(7)
        val pixeles = IntArray(ancho * alto) { i ->
            val x = i % ancho
            val y = i / ancho
            val base = Color.rgb(x * 255 / ancho, y * 255 / alto, (x + y) * 255 / (ancho + alto))
            if (ruido) {
                val r = azar.nextInt(60) - 30
                Color.rgb(
                    (Color.red(base) + r).coerceIn(0, 255),
                    (Color.green(base) + r).coerceIn(0, 255),
                    (Color.blue(base) + r).coerceIn(0, 255)
                )
            } else {
                base
            }
        }
        return Bitmap.createBitmap(pixeles, ancho, alto, Bitmap.Config.ARGB_8888)
    }

    private fun guardar(bitmap: Bitmap, nombre: String, formato: Bitmap.CompressFormat, calidad: Int = 95): File {
        val archivo = File(contexto.cacheDir, "pruebas/$nombre").also { archivos += it }
        FileOutputStream(archivo).use { bitmap.compress(formato, calidad, it) }
        return archivo
    }

    private fun decodificar(bytes: ByteArray): Bitmap {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        assertNotNull("El WebP resultante no se puede abrir", bitmap)
        return bitmap
    }

    @Test
    fun una_foto_de_camara_grande_sale_en_webp_de_1280_px_y_500_kb() = runBlocking {
        val archivo = guardar(imagen(4000, 3000, ruido = true), "camara.jpg", Bitmap.CompressFormat.JPEG)

        val resultado = conversor.convertir(Uri.fromFile(archivo))

        assertTrue("No es WebP", PlanWebp.esWebp(resultado.webp))
        assertTrue("Pesa ${resultado.webp.size} bytes", resultado.webp.size <= PlanWebp.MAX_BYTES)
        assertTrue(max(resultado.ancho, resultado.alto) <= PlanWebp.LADO_MAX)
        val abierta = decodificar(resultado.webp)
        assertEquals(resultado.ancho, abierta.width)
        assertEquals(resultado.alto, abierta.height)
        // 4:3 se conserva
        assertEquals(4.0 / 3.0, resultado.ancho.toDouble() / resultado.alto, 0.02)
    }

    @Test
    fun un_png_pequeno_no_se_agranda() = runBlocking {
        val archivo = guardar(imagen(640, 480), "pequena.png", Bitmap.CompressFormat.PNG)

        val resultado = conversor.convertir(Uri.fromFile(archivo))

        assertTrue(PlanWebp.esWebp(resultado.webp))
        assertEquals(640, resultado.ancho)
        assertEquals(480, resultado.alto)
        assertEquals(80, resultado.calidad)
    }

    @Test
    fun la_orientacion_exif_se_corrige_antes_de_convertir() = runBlocking {
        // Una foto vertical guardada "de lado": 400 x 200 con la etiqueta "girar 90°"
        val archivo = guardar(imagen(400, 200), "girada.jpg", Bitmap.CompressFormat.JPEG)
        ExifInterface(archivo.absolutePath).apply {
            setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
            saveAttributes()
        }

        val resultado = conversor.convertir(Uri.fromFile(archivo))

        assertEquals(200, resultado.ancho)
        assertEquals(400, resultado.alto)
        val abierta = decodificar(resultado.webp)
        assertEquals(200, abierta.width)
        assertEquals(400, abierta.height)
    }

    @Test
    fun la_miniatura_y_la_copia_para_la_ia_son_pequenas() = runBlocking {
        val archivo = guardar(imagen(3000, 2000), "grande.jpg", Bitmap.CompressFormat.JPEG)

        val resultado = conversor.convertir(Uri.fromFile(archivo))

        assertTrue(max(resultado.miniatura.width, resultado.miniatura.height) <= 320)
        assertTrue(max(resultado.paraEtiquetar.width, resultado.paraEtiquetar.height) <= 640)
    }

    @Test
    fun un_archivo_que_no_es_imagen_da_un_error_claro() {
        val archivo = File(contexto.cacheDir, "pruebas/texto.jpg").also { archivos += it }
        archivo.writeText("esto no es una foto")

        val error = runCatching { runBlocking { conversor.convertir(Uri.fromFile(archivo)) } }.exceptionOrNull()

        assertTrue("Debía lanzar ErrorFoto y lanzó $error", error is ErrorFoto)
    }
}
