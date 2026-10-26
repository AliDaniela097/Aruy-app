package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.catalogo.EtiquetaFoto
import ec.edu.puce.aruy.domain.catalogo.SugerenciaCategoria
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SugerenciaCategoriaTest {

    private fun etiqueta(original: String, confianza: Float) =
        EtiquetaFoto(SugerenciaCategoria.traducir(original), confianza, original)

    @Test
    fun sugiere_la_categoria_de_las_etiquetas_en_ingles() {
        val etiquetas = listOf(etiqueta("Faucet", 0.9f), etiqueta("Bathroom", 0.8f), etiqueta("Wall", 0.75f))
        assertEquals(CategoriaServicio.PLOMERIA, SugerenciaCategoria.sugerir(etiquetas))
    }

    @Test
    fun gana_la_categoria_con_mas_confianza_acumulada() {
        val etiquetas = listOf(etiqueta("Paint", 0.95f), etiqueta("Ceiling", 0.9f), etiqueta("Wall", 0.8f))
        assertEquals(CategoriaServicio.PINTURA, SugerenciaCategoria.sugerir(etiquetas))
    }

    @Test
    fun sin_etiquetas_relacionadas_no_sugiere_nada() {
        assertNull(SugerenciaCategoria.sugerir(emptyList()))
        assertNull(SugerenciaCategoria.sugerir(listOf(etiqueta("Dog", 0.99f), etiqueta("Sky", 0.9f))))
    }

    @Test
    fun traduce_las_etiquetas_conocidas_y_deja_las_demas() {
        assertEquals("Ladrillo", SugerenciaCategoria.traducir("Brick"))
        assertEquals("Cabello", SugerenciaCategoria.traducir(" hair "))
        assertEquals("Skateboard", SugerenciaCategoria.traducir("Skateboard"))
    }
}
