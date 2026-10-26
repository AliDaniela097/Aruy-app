package ec.edu.puce.aruy.data

import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.repository.CatalogoRepository
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CatalogoRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: CatalogoRepository

    private val servicios = """
        [{"id":"s1","prestador_id":"p1","titulo":"Reparación de fugas","descripcion":"Arreglo fugas",
          "categoria":"PLOMERIA","precio_referencial":20,"disponible":true},
         {"id":"s2","prestador_id":"p2","titulo":"Pintura","descripcion":"Pinto paredes",
          "categoria":"PINTURA","precio_referencial":null,"disponible":true}]
    """.trimIndent()

    private val perfiles = """
        [{"id":"p1","usuario_id":"u1","nombre":"Luis Quishpe","telefono":"0998765432",
          "estado_verificacion":"VERIFICADA","oficio_principal":"Plomero","zona_cobertura":"Sur",
          "disponible":true,"calificacion_promedio":4.8,"total_calificaciones":12}]
    """.trimIndent()

    private fun preparar(perfilesDisponibles: Boolean) {
        servidor = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
                    "/servicios" -> MockResponse().setBody(servicios)
                    "/perfiles" ->
                        if (perfilesDisponibles) MockResponse().setBody(perfiles)
                        else MockResponse().setResponseCode(404).setBody("""{"error":"Ruta no encontrada"}""")
                    "/servicios/s1/fotos" -> MockResponse().setBody(
                        """[{"id":"f2","referencia_imagen":"s1/b.webp","url":"https://x/b.webp","orden":1,"etiquetas":[]},
                            {"id":"f1","referencia_imagen":"s1/a.webp","url":"https://x/a.webp","orden":0,"etiquetas":["Tuberia"]}]"""
                    )
                    "/calificaciones?perfil_prestador_id=p1" -> MockResponse().setBody(
                        """{"perfil_prestador_id":"p1","promedio":4.5,"total":2,
                            "calificaciones":[{"id":"c1","puntuacion":5,"comentario":"Excelente","fecha_creacion":"2026-10-01","contratante":"Lucía"}]}"""
                    )
                    else -> MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        val cliente = RetrofitClient({ null }, {}, servidor.url("/").toString())
        repositorio = CatalogoRepository(cliente.servicioApi, cliente.perfilApi, cliente.calificacionApi)
    }

    @After
    fun despues() = servidor.shutdown()

    @Test
    fun une_cada_servicio_con_los_datos_de_su_prestador() = runBlocking {
        preparar(perfilesDisponibles = true)

        val catalogo = repositorio.cargarCatalogo()

        assertEquals(2, catalogo.size)
        val primero = catalogo.first { it.id == "s1" }
        assertEquals("Luis Quishpe", primero.nombrePrestador)
        assertEquals(4.8, primero.prestador?.promedio ?: 0.0, 0.0)
        assertEquals(true, primero.prestador?.verificado)
        assertNull(catalogo.first { it.id == "s2" }.prestador)
        assertEquals("Prestador", catalogo.first { it.id == "s2" }.nombrePrestador)
    }

    @Test
    fun si_el_servidor_no_tiene_perfiles_el_catalogo_sigue_funcionando() = runBlocking {
        preparar(perfilesDisponibles = false)

        val catalogo = repositorio.cargarCatalogo()

        assertEquals(2, catalogo.size)
        assertNull(catalogo[0].prestador)
        assertNull(repositorio.prestador("p1"))
    }

    @Test
    fun las_fotos_llegan_ordenadas_y_con_su_direccion_publica() = runBlocking {
        preparar(perfilesDisponibles = true)

        val fotos = repositorio.fotos("s1")

        assertEquals(listOf("f1", "f2"), fotos.map { it.id })
        assertEquals("https://x/a.webp", fotos.first().url)
        assertEquals(listOf("Tuberia"), fotos.first().etiquetasIA)
    }

    @Test
    fun las_opiniones_traen_promedio_total_y_autor() = runBlocking {
        preparar(perfilesDisponibles = true)

        val resumen = repositorio.opiniones("p1")

        assertEquals(4.5, resumen.promedio ?: 0.0, 0.0)
        assertEquals(2, resumen.total)
        assertNotNull(resumen.opiniones.firstOrNull())
        assertEquals("Lucía", resumen.opiniones.first().autor)
    }
}
