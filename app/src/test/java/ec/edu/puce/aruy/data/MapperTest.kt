package ec.edu.puce.aruy.data

import com.google.gson.Gson
import ec.edu.puce.aruy.data.mapper.toDomain
import ec.edu.puce.aruy.data.remote.dto.CalificacionesDto
import ec.edu.puce.aruy.data.remote.dto.FotoDto
import ec.edu.puce.aruy.data.remote.dto.PerfilPublicoDto
import ec.edu.puce.aruy.data.remote.dto.ServicioDto
import ec.edu.puce.aruy.data.remote.dto.UsuarioDto
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.EstadoVerificacion
import ec.edu.puce.aruy.domain.model.RolUsuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Los DTO reciben el JSON del backend; estos casos son los que más fácil rompen la app si cambia algo. */
class MapperTest {

    private val gson = Gson()

    @Test
    fun una_categoria_desconocida_cae_en_otros() {
        val servicio = ServicioDto("s1", "p1", "Servicio", "Descripción", "ASTROLOGIA", null, true).toDomain()
        assertEquals(CategoriaServicio.OTROS, servicio.categoria)
        assertNull(servicio.precioReferencial)
    }

    @Test
    fun el_servicio_se_lee_del_json_del_backend() {
        val json = """{"id":"s1","prestador_id":"p1","titulo":"Fugas","descripcion":"Arreglo","categoria":"PLOMERIA","precio_referencial":12.5,"disponible":false}"""
        val servicio = gson.fromJson(json, ServicioDto::class.java).toDomain()
        assertEquals("p1", servicio.prestadorId)
        assertEquals(CategoriaServicio.PLOMERIA, servicio.categoria)
        assertEquals(12.5, servicio.precioReferencial!!, 0.0)
        assertFalse(servicio.disponible)
    }

    @Test
    fun un_perfil_con_datos_faltantes_no_rompe_la_app() {
        val perfil = gson.fromJson("""{"id":"p1"}""", PerfilPublicoDto::class.java).toDomain()
        assertEquals("Prestador", perfil.nombre)
        assertNull(perfil.telefono)
        assertFalse(perfil.verificado)
        assertTrue(perfil.disponible)
        assertEquals(0, perfil.totalCalificaciones)
        assertNull(perfil.promedio)
    }

    @Test
    fun un_telefono_en_blanco_se_trata_como_ausente() {
        val perfil = PerfilPublicoDto(id = "p1", nombre = "  ", telefono = " ").toDomain()
        assertEquals("Prestador", perfil.nombre)
        assertNull(perfil.telefono)
    }

    @Test
    fun solo_VERIFICADA_cuenta_como_verificado() {
        assertTrue(PerfilPublicoDto(id = "p", estadoVerificacion = "VERIFICADA").toDomain().verificado)
        assertFalse(PerfilPublicoDto(id = "p", estadoVerificacion = "PENDIENTE").toDomain().verificado)
    }

    @Test
    fun las_fotos_traen_ruta_y_direccion_publica() {
        val json = """{"id":"f1","referencia_imagen":"s1/a.webp","url":"https://x/a.webp","orden":2,"etiquetas":["Cable"]}"""
        val foto = gson.fromJson(json, FotoDto::class.java).toDomain("s1")
        assertEquals("s1", foto.servicioId)
        assertEquals("s1/a.webp", foto.referenciaImagen)
        assertEquals("https://x/a.webp", foto.url)
        assertEquals(listOf("Cable"), foto.etiquetasIA)
        assertEquals(2, foto.orden)
    }

    @Test
    fun una_foto_sin_etiquetas_ni_orden_usa_valores_por_defecto() {
        val foto = gson.fromJson("""{"id":"f1","referencia_imagen":"x.webp"}""", FotoDto::class.java).toDomain("s1")
        assertTrue(foto.etiquetasIA.isEmpty())
        assertEquals(0, foto.orden)
        assertNull(foto.url)
    }

    @Test
    fun una_opinion_sin_autor_se_muestra_con_un_nombre_generico() {
        val json = """{"promedio":4.5,"total":1,"calificaciones":[{"id":"c1","puntuacion":4,"comentario":"  ","fecha_creacion":"2026-10-01"}]}"""
        val resumen = gson.fromJson(json, CalificacionesDto::class.java).toDomain()
        assertEquals(4.5, resumen.promedio!!, 0.0)
        assertEquals("Persona de Aruy", resumen.opiniones.first().autor)
        assertNull(resumen.opiniones.first().comentario)
    }

    @Test
    fun sin_calificaciones_el_total_es_cero() {
        val resumen = gson.fromJson("""{"promedio":null,"total":0,"calificaciones":[]}""", CalificacionesDto::class.java).toDomain()
        assertNull(resumen.promedio)
        assertEquals(0, resumen.total)
        assertTrue(resumen.opiniones.isEmpty())
    }

    @Test
    fun un_rol_o_estado_desconocido_no_rompe_el_usuario() {
        val usuario = UsuarioDto("u1", "Ana Mora", "0998765432", "ADMIN", "OTRO_ESTADO").toDomain()
        assertEquals(RolUsuario.CONTRATANTE, usuario.rol)
        assertEquals(EstadoVerificacion.NO_SOLICITADA, usuario.estadoVerificacion)
        assertTrue(usuario.activo)
    }
}
