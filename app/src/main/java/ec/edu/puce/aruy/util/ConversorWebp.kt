package ec.edu.puce.aruy.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

/** La foto no se pudo leer o convertir; [message] se muestra tal cual a la persona. */
class ErrorFoto(message: String) : Exception(message)

/**
 * Foto lista para subir: ya en WebP, con una miniatura para mostrarla y una copia pequeña para que
 * ML Kit la analice.
 */
class ImagenPreparada(
    val webp: ByteArray,
    val ancho: Int,
    val alto: Int,
    val calidad: Int,
    val miniatura: Bitmap,
    val paraEtiquetar: Bitmap
)

/**
 * Lee una foto de la cámara o de la galería (JPG, PNG, WEBP...), corrige su orientación, la reduce a
 * 1280 px y la guarda en WebP de 500 KB como máximo. Corre fuera del hilo principal.
 */
class ConversorWebp(private val contexto: Context) {

    suspend fun convertir(uri: Uri): ImagenPreparada = withContext(Dispatchers.Default) {
        val resolver = contexto.contentResolver

        // Con inJustDecodeBounds, decodeStream solo llena las medidas y devuelve null: no sirve para saber si abrió
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val abierta = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas); true } ?: false
        if (!abierta) throw ErrorFoto("No se pudo abrir la foto.")
        if (medidas.outWidth <= 0 || medidas.outHeight <= 0) throw ErrorFoto("El archivo no es una foto válida.")

        val orientacion = resolver.openInputStream(uri)?.use {
            runCatching { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
                .getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        // Se lee ya reducida: una foto de 12 megapíxeles no cabe cómoda en memoria
        val opciones = BitmapFactory.Options().apply {
            inSampleSize = PlanWebp.muestreo(medidas.outWidth, medidas.outHeight, PlanWebp.LADO_MAX)
        }
        val leida = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: throw ErrorFoto("No se pudo leer la foto.")
        val base = girar(leida, orientacion)

        var escalada = base
        var ladoEscalada = -1
        val elegida = PlanWebp.elegir { lado, calidad ->
            if (lado != ladoEscalada) {
                escalada = reducir(base, lado)
                ladoEscalada = lado
            }
            comprimir(escalada, calidad)
        } ?: throw ErrorFoto("No se pudo reducir la foto a 500 KB. Prueba con otra más pequeña.")

        if (!PlanWebp.esWebp(elegida.bytes)) throw ErrorFoto("Este teléfono no pudo convertir la foto a WebP.")

        ImagenPreparada(
            webp = elegida.bytes,
            ancho = escalada.width,
            alto = escalada.height,
            calidad = elegida.calidad,
            miniatura = reducir(base, MINIATURA),
            paraEtiquetar = reducir(base, PARA_ETIQUETAR)
        )
    }

    /** Borra el archivo temporal que dejó la cámara (carpeta de caché de la app). */
    fun borrarTemporal(uri: Uri) {
        runCatching { contexto.contentResolver.delete(uri, null, null) }
    }

    private fun reducir(origen: Bitmap, ladoMax: Int): Bitmap {
        val destino = PlanWebp.ajustar(origen.width, origen.height, ladoMax)
        if (destino.ancho == origen.width && destino.alto == origen.height) return origen
        return Bitmap.createScaledBitmap(origen, destino.ancho, destino.alto, true)
    }

    private fun comprimir(bitmap: Bitmap, calidad: Int): ByteArray {
        val salida = ByteArrayOutputStream()
        val formato = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP // antes de Android 11 es WebP con pérdida
        }
        bitmap.compress(formato, calidad, salida)
        return salida.toByteArray()
    }

    /** Aplica la orientación EXIF: así la foto sale derecha aunque el celular la haya guardado girada. */
    private fun girar(origen: Bitmap, orientacion: Int): Bitmap {
        val matriz = Matrix()
        when (orientacion) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matriz.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matriz.postRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matriz.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matriz.postRotate(90f); matriz.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_90 -> matriz.postRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> { matriz.postRotate(270f); matriz.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_ROTATE_270 -> matriz.postRotate(270f)
            else -> return origen
        }
        return Bitmap.createBitmap(origen, 0, 0, origen.width, origen.height, matriz, true)
    }

    private companion object {
        const val MINIATURA = 320
        const val PARA_ETIQUETAR = 640
    }
}
