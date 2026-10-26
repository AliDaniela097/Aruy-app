package ec.edu.puce.aruy.domain.catalogo

import ec.edu.puce.aruy.domain.model.CategoriaServicio
import java.util.Locale

/**
 * Etiqueta que ML Kit reconoció en una foto. [texto] es lo que se muestra y se guarda (en español
 * cuando se conoce la traducción); [original] es lo que devolvió ML Kit, en inglés.
 */
data class EtiquetaFoto(val texto: String, val confianza: Float, val original: String = texto)

/**
 * Sugiere la categoría del servicio a partir de las etiquetas que ML Kit pone a la foto.
 * ML Kit solo entrega etiquetas en inglés; aquí se agrupan por categoría y se traducen las más comunes.
 * Es una ayuda, no una regla: la persona siempre puede elegir otra categoría.
 */
object SugerenciaCategoria {

    private val PALABRAS: Map<CategoriaServicio, List<String>> = mapOf(
        CategoriaServicio.PLOMERIA to listOf("plumbing", "pipe", "tap", "faucet", "sink", "bathroom", "toilet", "bathtub", "shower"),
        CategoriaServicio.ELECTRICIDAD to listOf("electric", "wire", "cable", "light", "lamp", "lighting", "switch", "circuit", "bulb", "chandelier"),
        CategoriaServicio.ALBANILERIA to listOf("brick", "wall", "cement", "concrete", "masonry", "construction", "tile", "floor", "roof", "plaster", "stone"),
        CategoriaServicio.PINTURA to listOf("paint", "brush", "roller", "ceiling", "mural"),
        CategoriaServicio.CARPINTERIA to listOf("wood", "furniture", "table", "chair", "cabinet", "carpent", "shelf", "door", "window", "plank", "drawer"),
        CategoriaServicio.BELLEZA to listOf("hair", "nail", "manicure", "pedicure", "beauty", "makeup", "cosmetic", "eyelash", "skin", "face", "lipstick", "salon")
    )

    private val TRADUCCIONES = mapOf(
        "plumbing" to "Plomería", "pipe" to "Tubería", "tap" to "Grifo", "faucet" to "Grifo", "sink" to "Lavamanos",
        "bathroom" to "Baño", "toilet" to "Inodoro", "bathtub" to "Tina", "shower" to "Ducha", "water" to "Agua",
        "wire" to "Cable", "cable" to "Cable", "light" to "Luz", "lamp" to "Lámpara", "lighting" to "Iluminación",
        "switch" to "Interruptor", "circuit" to "Circuito", "electricity" to "Electricidad", "bulb" to "Foco",
        "brick" to "Ladrillo", "wall" to "Pared", "cement" to "Cemento", "concrete" to "Concreto", "masonry" to "Albañilería",
        "construction" to "Construcción", "tile" to "Baldosa", "floor" to "Piso", "roof" to "Techo", "stone" to "Piedra",
        "paint" to "Pintura", "brush" to "Brocha", "ceiling" to "Cielo raso", "mural" to "Mural",
        "wood" to "Madera", "furniture" to "Mueble", "table" to "Mesa", "chair" to "Silla", "cabinet" to "Gabinete",
        "shelf" to "Repisa", "door" to "Puerta", "window" to "Ventana", "drawer" to "Cajón",
        "hair" to "Cabello", "hairstyle" to "Peinado", "nail" to "Uñas", "manicure" to "Manicure", "pedicure" to "Pedicure",
        "beauty" to "Belleza", "makeup" to "Maquillaje", "skin" to "Piel", "face" to "Rostro", "eyelash" to "Pestañas"
    )

    fun traducir(etiqueta: String): String =
        TRADUCCIONES[etiqueta.trim().lowercase(Locale.ROOT)] ?: etiqueta.trim()

    /** La categoría cuyas etiquetas suman más confianza, o null si ninguna etiqueta se relaciona con un oficio. */
    fun sugerir(etiquetas: List<EtiquetaFoto>): CategoriaServicio? {
        val puntajes = PALABRAS.mapValues { (_, palabras) ->
            etiquetas.sumOf { etiqueta ->
                val texto = etiqueta.original.lowercase(Locale.ROOT)
                if (palabras.any { texto.contains(it) }) etiqueta.confianza.toDouble() else 0.0
            }
        }
        return puntajes.filterValues { it > 0.0 }.maxByOrNull { it.value }?.key
    }
}
