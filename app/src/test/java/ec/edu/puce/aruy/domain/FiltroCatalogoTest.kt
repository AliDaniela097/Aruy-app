package ec.edu.puce.aruy.domain

import ec.edu.puce.aruy.domain.catalogo.FiltroCatalogo
import ec.edu.puce.aruy.domain.catalogo.FiltrosCatalogo
import ec.edu.puce.aruy.domain.catalogo.OrdenCatalogo
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.PrestadorPublico
import ec.edu.puce.aruy.domain.model.Servicio
import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FiltroCatalogoTest {

    private fun prestador(
        id: String,
        nombre: String = "Luis Quishpe",
        zona: String = "Sur",
        promedio: Double? = null,
        total: Int = 0,
        verificado: Boolean = false,
        disponible: Boolean = true
    ) = PrestadorPublico(
        id = id, usuarioId = "u$id", nombre = nombre, telefono = "0998765432", fotoPerfil = null,
        verificado = verificado, oficio = "Plomero", descripcion = null, zona = zona,
        aniosExperiencia = null, disponible = disponible, promedio = promedio, totalCalificaciones = total
    )

    private fun item(
        id: String,
        titulo: String,
        categoria: CategoriaServicio = CategoriaServicio.PLOMERIA,
        precio: Double? = null,
        disponible: Boolean = true,
        prestador: PrestadorPublico? = prestador("p-$id")
    ) = ServicioConPrestador(
        Servicio(id, "p-$id", titulo, "Descripción de $titulo", categoria, precio, disponible),
        prestador
    )

    private val catalogo = listOf(
        item("1", "Reparación de fugas", precio = 20.0, prestador = prestador("p-1", promedio = 4.8, total = 40, verificado = true)),
        item("2", "Instalación eléctrica", CategoriaServicio.ELECTRICIDAD, precio = 35.0, prestador = prestador("p-2", "Ana Mora", "Norte", 4.2, 10)),
        item("3", "Manicure y pedicure", CategoriaServicio.BELLEZA, precio = 12.0, disponible = false, prestador = prestador("p-3", "Camila Andrade", "Centro", 4.9, 5)),
        item("4", "Pintura de paredes", CategoriaServicio.PINTURA, prestador = null)
    )

    private fun ids(r: List<ServicioConPrestador>) = r.map { it.id }

    @Test
    fun la_busqueda_ignora_tildes_y_mayusculas() {
        val r = FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(texto = "REPARACION"))
        assertEquals(listOf("1"), ids(r))
    }

    @Test
    fun la_busqueda_encuentra_plurales_y_por_nombre_del_prestador() {
        assertEquals(listOf("2"), ids(FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(texto = "instalaciones electricas"))))
        assertEquals(listOf("3"), ids(FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(texto = "camila"))))
    }

    @Test
    fun filtra_por_categoria_zona_y_calificacion() {
        assertEquals(listOf("2"), ids(FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(categoria = CategoriaServicio.ELECTRICIDAD))))
        assertEquals(listOf("3"), ids(FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(zona = "centro"))))
        assertEquals(listOf("3", "1"), ids(FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(calificacionMinima = 4.5))))
    }

    @Test
    fun solo_disponibles_y_solo_verificados() {
        assertFalse("3" in ids(FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(soloDisponibles = true))))
        assertEquals(listOf("1"), ids(FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(soloVerificados = true))))
    }

    @Test
    fun un_prestador_no_disponible_no_cuenta_como_disponible_ahora() {
        val pausado = item("5", "Servicio", prestador = prestador("p-5", disponible = false))
        assertFalse(FiltroCatalogo.disponibleAhora(pausado))
        assertTrue(FiltroCatalogo.disponibleAhora(item("6", "Servicio")))
    }

    @Test
    fun precio_maximo_deja_pasar_los_servicios_sin_precio() {
        val r = FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(precioMaximo = 25.0))
        assertEquals(setOf("1", "3", "4"), ids(r).toSet())
    }

    @Test
    fun oculta_los_servicios_de_prestadores_bloqueados() {
        val r = FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(), prestadoresBloqueados = setOf("p-1"))
        assertFalse("1" in ids(r))
    }

    @Test
    fun ordena_por_calificacion_precio_y_alfabeto() {
        assertEquals(listOf("3", "1", "2", "4"), ids(FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(orden = OrdenCatalogo.MEJOR_CALIFICACION))))
        assertEquals(listOf("3", "1", "2", "4"), ids(FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(orden = OrdenCatalogo.MENOR_PRECIO))))
        assertEquals(listOf("2", "3", "4", "1"), ids(FiltroCatalogo.aplicar(catalogo, FiltrosCatalogo(orden = OrdenCatalogo.ALFABETICO))))
    }

    @Test
    fun cuenta_los_filtros_activos_sin_contar_texto_ni_orden() {
        assertEquals(0, FiltrosCatalogo(texto = "x", orden = OrdenCatalogo.MENOR_PRECIO).cantidadActivos)
        assertEquals(3, FiltrosCatalogo(categoria = CategoriaServicio.OTROS, soloDisponibles = true, precioMaximo = 30.0).cantidadActivos)
    }
}
