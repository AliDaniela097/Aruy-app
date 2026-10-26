package ec.edu.puce.aruy.data

import com.google.gson.JsonParser
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.remote.util.MensajesPrestador
import ec.edu.puce.aruy.data.repository.DatosServicio
import ec.edu.puce.aruy.data.repository.PrestadorRepository
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PrestadorRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var repositorio: PrestadorRepository

    private val servicioJson = """
        {"id":"s1","prestador_id":"p1","titulo":"Instalación","descripcion":"Instalo breakers",
         "categoria":"ELECTRICIDAD","precio_referencial":15,"disponible":true}
    """.trimIndent()

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val cliente = RetrofitClient({ "token" }, {}, servidor.url("/").toString())
        repositorio = PrestadorRepository(cliente.prestadorApi, cliente.servicioApi)
    }

    @After
    fun terminar() = servidor.shutdown()

    @Test
    fun la_foto_viaja_como_cuerpo_binario_image_webp_con_sus_etiquetas() = runBlocking {
        servidor.enqueue(
            MockResponse().setResponseCode(201).setBody(
                """{"id":"f1","referencia_imagen":"s1/abc.webp","url":"https://x/abc.webp","orden":0,
                    "etiquetas":["Cable"],"formato":"webp","ancho":1280,"alto":960,"bytes":90000,"calidad":80}"""
            )
        )
        val webp = "RIFF".toByteArray() + byteArrayOf(4, 0, 0, 0) + "WEBPVP8 ".toByteArray()

        val foto = repositorio.subirFoto("s1", webp, listOf("Cable", "Pared, vieja"), "  Tablero nuevo ")

        val peticion = servidor.takeRequest()
        assertEquals("POST", peticion.method)
        assertEquals("/servicios/s1/fotos", peticion.requestUrl!!.encodedPath)
        assertEquals("image/webp", peticion.getHeader("Content-Type"))
        assertEquals("Bearer token", peticion.getHeader("Authorization"))
        assertEquals("Cable,Pared  vieja", peticion.requestUrl!!.queryParameter("etiquetas"))
        assertEquals("Tablero nuevo", peticion.requestUrl!!.queryParameter("descripcion"))
        assertArrayEquals(webp, peticion.body.readByteArray())

        assertEquals("f1", foto.id)
        assertEquals("s1/abc.webp", foto.referenciaImagen)
        assertEquals("https://x/abc.webp", foto.url)
    }

    @Test
    fun sin_etiquetas_ni_descripcion_no_se_envian_esos_parametros() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(201).setBody("""{"id":"f1","referencia_imagen":"s1/a.webp"}"""))

        repositorio.subirFoto("s1", ByteArray(20))

        val url = servidor.takeRequest().requestUrl!!
        assertNull(url.queryParameter("etiquetas"))
        assertNull(url.queryParameter("descripcion"))
    }

    @Test
    fun las_etiquetas_se_limpian_antes_de_enviarse() {
        val muchas = (1..30).map { "Etiqueta $it" } + listOf("  ", "Etiqueta 1")
        val unidas = PrestadorRepository.unirEtiquetas(muchas)!!.split(",")
        assertEquals(20, unidas.size)
        assertEquals("Etiqueta 1", unidas.first())
        assertTrue(PrestadorRepository.unirEtiquetas(listOf("x".repeat(80)))!!.length == 50)
        assertNull(PrestadorRepository.unirEtiquetas(emptyList()))
    }

    @Test
    fun crear_servicio_no_lleva_fotos() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(201).setBody(servicioJson))

        val servicio = repositorio.crear(DatosServicio(" Instalación ", "Instalo breakers", CategoriaServicio.ELECTRICIDAD, 15.0, true))

        val cuerpo = JsonParser.parseString(servidor.takeRequest().body.readUtf8()).asJsonObject
        assertEquals("Instalación", cuerpo.get("titulo").asString)
        assertEquals("ELECTRICIDAD", cuerpo.get("categoria").asString)
        assertEquals(15.0, cuerpo.get("precio_referencial").asDouble, 0.0)
        assertTrue(!cuerpo.has("fotos"))
        assertEquals("s1", servicio.id)
    }

    @Test
    fun editar_sin_precio_envia_null_para_borrarlo() = runBlocking {
        servidor.enqueue(MockResponse().setBody(servicioJson))

        repositorio.editar("s1", DatosServicio("Instalación", "Instalo breakers", CategoriaServicio.ELECTRICIDAD, null, false))

        val peticion = servidor.takeRequest()
        assertEquals("PATCH", peticion.method)
        val cuerpo = JsonParser.parseString(peticion.body.readUtf8()).asJsonObject
        assertTrue(cuerpo.has("precio_referencial") && cuerpo.get("precio_referencial").isJsonNull)
        assertEquals(false, cuerpo.get("disponible").asBoolean)
    }

    @Test
    fun pausar_un_servicio_solo_envia_disponible() = runBlocking {
        servidor.enqueue(MockResponse().setBody(servicioJson))

        repositorio.cambiarDisponibilidadDelServicio("s1", false)

        val cuerpo = JsonParser.parseString(servidor.takeRequest().body.readUtf8()).asJsonObject
        assertEquals(setOf("disponible"), cuerpo.keySet())
    }

    @Test
    fun eliminar_acepta_la_respuesta_204_sin_cuerpo() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(204))
        servidor.enqueue(MockResponse().setResponseCode(204))

        repositorio.eliminar("s1")
        repositorio.eliminarFoto("f1")

        assertEquals("DELETE /servicios/s1", servidor.takeRequest().let { "${it.method} ${it.path}" })
        assertEquals("DELETE /fotos/f1", servidor.takeRequest().let { "${it.method} ${it.path}" })
    }

    @Test
    fun cada_error_de_subida_tiene_su_mensaje() {
        fun mensaje(codigo: Int) = MensajesPrestador.alSubirFoto(
            HttpException(Response.error<Any>(codigo, "{}".toResponseBody("application/json".toMediaType())))
        )
        assertTrue(mensaje(409).contains("10 fotos"))
        assertTrue(mensaje(422).contains("500 KB"))
        assertTrue(mensaje(415).contains("JPG, PNG o WEBP"))
        assertTrue(mensaje(403).contains("no es tuyo"))
        assertTrue(mensaje(503).contains("más tarde"))
    }
}
