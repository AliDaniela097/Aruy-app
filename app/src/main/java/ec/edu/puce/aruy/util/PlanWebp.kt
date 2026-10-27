package ec.edu.puce.aruy.util

import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Reglas para convertir las fotos del portafolio a WebP antes de enviarlas. Son las mismas que aplica
 * el servidor (`arruy-backend/imagen.js`): lado largo de 1280 px como máximo, calidad desde 80 y
 * máximo 500 KB. Esta parte no depende de Android, por eso se puede probar sin teléfono.
 */
object PlanWebp {

    const val LADO_MAX = 1280
    const val MAX_BYTES = 500 * 1024

    /** Se empieza en 80 y solo se baja si la foto no cabe en 500 KB. */
    val CALIDADES = listOf(80, 75, 70, 60, 50)

    /** Si ni con la calidad más baja cabe, se reduce el lado largo. */
    val LADOS = listOf(LADO_MAX, 1024, 800)

    data class Dimensiones(val ancho: Int, val alto: Int)

    data class Resultado(val bytes: ByteArray, val lado: Int, val calidad: Int)

    /** Cabe dentro de [ladoMax] x [ladoMax] sin deformarse y sin agrandarse. */
    fun ajustar(ancho: Int, alto: Int, ladoMax: Int): Dimensiones {
        val mayor = max(ancho, alto)
        if (mayor <= ladoMax) return Dimensiones(ancho, alto)
        val factor = ladoMax.toDouble() / mayor
        return Dimensiones(
            (ancho * factor).roundToInt().coerceAtLeast(1),
            (alto * factor).roundToInt().coerceAtLeast(1)
        )
    }

    /**
     * `inSampleSize` para leer la foto sin cargar los 12 megapíxeles del celular en memoria:
     * la mayor potencia de 2 que deja el lado largo todavía en [ladoObjetivo] o más.
     */
    fun muestreo(ancho: Int, alto: Int, ladoObjetivo: Int): Int {
        var muestreo = 1
        val mayor = max(ancho, alto)
        while (mayor / (muestreo * 2) >= ladoObjetivo) muestreo *= 2
        return muestreo
    }

    /**
     * Prueba cada lado y calidad, en ese orden, y devuelve la primera combinación que cabe en
     * [MAX_BYTES]; null si ninguna cabe (el servidor también respondería 422).
     */
    fun elegir(compresor: (lado: Int, calidad: Int) -> ByteArray): Resultado? {
        for (lado in LADOS) {
            for (calidad in CALIDADES) {
                val bytes = compresor(lado, calidad)
                if (bytes.size <= MAX_BYTES) return Resultado(bytes, lado, calidad)
            }
        }
        return null
    }

    /** Un archivo WebP empieza con "RIFF", 4 bytes de tamaño y "WEBP". */
    fun esWebp(bytes: ByteArray): Boolean =
        bytes.size > 12 &&
            String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP"
}
