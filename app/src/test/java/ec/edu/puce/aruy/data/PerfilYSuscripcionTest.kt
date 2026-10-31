package ec.edu.puce.aruy.data

import com.google.gson.JsonParser
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.remote.util.MensajesPerfil
import ec.edu.puce.aruy.data.repository.DatosPerfilPrestador
import ec.edu.puce.aruy.data.repository.PerfilRepository
import ec.edu.puce.aruy.data.repository.SuscripcionRepository
import ec.edu.puce.aruy.domain.model.EstadoSuscripcion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.TipoPlan
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class PerfilYSuscripcionTest {

    private lateinit var servidor: MockWebServer
    private lateinit var perfiles: PerfilRepository
    private lateinit var suscripciones: SuscripcionRepository

    private val usuarioJson = """
        {"id":"u1","nombre":"Camila Andrade","telefono":"0998765432","rol":"PRESTADOR",
         "estado_verificacion":"VERIFICADA","foto_perfil":null,"activo":true,"email":"camila@correo.com"}
    """.trimIndent()

    private val suscripcionJson = """
        {"id":"x1","prestador_id":"p1","tipo_plan":"ARUY_PRO_SEMANAL","estado":"ACTIVA",
         "fecha_inicio":"2026-10-25T12:00:00.000Z","fecha_fin":"2026-11-01T12:00:00.000Z",
         "renovacion_automatica":false,"simulada":true}
    """.trimIndent()

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        val cliente = RetrofitClient({ "token" }, {}, servidor.url("/").toString())
        perfiles = PerfilRepository(cliente.usuarioApi, cliente.prestadorApi)
        suscripciones = SuscripcionRepository(cliente.suscripcionApi)
    }

    @After
    fun terminar() = servidor.shutdown()

    private fun httpError(codigo: Int, cuerpo: String = "{}") =
        HttpException(Response.error<Any>(codigo, cuerpo.toResponseBody("application/json".toMediaType())))

    @Test
    fun mis_datos_incluyen_el_correo() = runBlocking {
        servidor.enqueue(MockResponse().setBody(usuarioJson))

        val datos = perfiles.misDatos()

        assertEquals("/usuarios/yo", servidor.takeRequest().path)
        assertEquals("camila@correo.com", datos.email)
        assertEquals(RolUsuario.PRESTADOR, datos.usuario.rol)
    }

    @Test
    fun editar_mis_datos_envia_nombre_y_telefono_limpios() = runBlocking {
        servidor.enqueue(MockResponse().setBody(usuarioJson))

        perfiles.actualizarMisDatos("  Camila Andrade ", "099 876-5432")

        val peticion = servidor.takeRequest()
        assertEquals("PATCH", peticion.method)
        val cuerpo = JsonParser.parseString(peticion.body.readUtf8()).asJsonObject
        assertEquals("Camila Andrade", cuerpo.get("nombre").asString)
        assertEquals("0998765432", cuerpo.get("telefono").asString)
    }

    @Test
    fun editar_el_perfil_vacio_borra_la_descripcion_y_los_anios() = runBlocking {
        servidor.enqueue(
            MockResponse().setBody(
                """{"id":"p1","usuario_id":"u1","oficio_principal":"Plomero","zona_cobertura":"Sur","disponible":true}"""
            )
        )

        val perfil = perfiles.actualizarMiPerfil(DatosPerfilPrestador(" Plomero ", "Sur", "  ", null))

        val peticion = servidor.takeRequest()
        assertEquals("/perfiles/yo", peticion.path)
        val cuerpo = JsonParser.parseString(peticion.body.readUtf8()).asJsonObject
        assertEquals("Plomero", cuerpo.get("oficio_principal").asString)
        assertTrue(cuerpo.get("descripcion_profesional").isJsonNull)
        assertTrue(cuerpo.get("anios_experiencia").isJsonNull)
        assertEquals("Plomero", perfil.oficioPrincipal)
    }

    @Test
    fun eliminar_la_cuenta_acepta_la_respuesta_204() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(204))

        perfiles.eliminarCuenta()

        val peticion = servidor.takeRequest()
        assertEquals("DELETE", peticion.method)
        assertEquals("/usuarios/yo", peticion.path)
    }

    @Test
    fun sin_suscripciones_el_404_es_simplemente_ninguna() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"Todavia no tienes suscripciones"}"""))

        assertNull(suscripciones.miSuscripcion())
    }

    @Test
    fun otros_errores_de_mi_suscripcion_no_se_ocultan() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        val error = runCatching { suscripciones.miSuscripcion() }.exceptionOrNull()

        assertTrue(error is HttpException && error.code() == 500)
    }

    @Test
    fun suscribirse_envia_el_plan_y_la_renovacion() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(201).setBody(suscripcionJson))

        val suscripcion = suscripciones.suscribirse(TipoPlan.ARUY_PRO_SEMANAL, renovacionAutomatica = true)

        val cuerpo = JsonParser.parseString(servidor.takeRequest().body.readUtf8()).asJsonObject
        assertEquals("ARUY_PRO_SEMANAL", cuerpo.get("tipo_plan").asString)
        assertTrue(cuerpo.get("renovacion_automatica").asBoolean)
        assertEquals(EstadoSuscripcion.ACTIVA, suscripcion.estado)
        assertEquals(TipoPlan.ARUY_PRO_SEMANAL, suscripcion.tipoPlan)
    }

    @Test
    fun no_se_puede_contratar_el_plan_gratuito() {
        val error = runCatching { runBlocking { suscripciones.suscribirse(TipoPlan.GRATUITO, false) } }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals(0, servidor.requestCount)
    }

    @Test
    fun cancelar_usa_la_ruta_del_id() = runBlocking {
        servidor.enqueue(MockResponse().setBody(suscripcionJson.replace("ACTIVA", "CANCELADA")))

        val cancelada = suscripciones.cancelar("x1")

        val peticion = servidor.takeRequest()
        assertEquals("PATCH", peticion.method)
        assertEquals("/suscripciones/x1/cancelar", peticion.path)
        assertEquals(EstadoSuscripcion.CANCELADA, cancelada.estado)
    }

    @Test
    fun los_errores_tienen_mensajes_claros() {
        assertTrue(MensajesPerfil.alGuardarPerfil(httpError(409)).contains("celular"))
        assertTrue(MensajesPerfil.enSuscripciones(httpError(409)).contains("pendiente o activa"))
        assertTrue(MensajesPerfil.enSuscripciones(httpError(403)).contains("prestadores"))
        assertFalse(MensajesPerfil.alGuardarPerfil(httpError(400, """{"error":"nombre invalido"}""")).isEmpty())
    }
}
