package ec.edu.puce.aruy.data

import com.google.gson.JsonParser
import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.repository.AuthRepository
import ec.edu.puce.aruy.data.repository.DatosRegistro
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.ui.viewmodel.AuthViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException

class AuthRepositoryTest {

    private lateinit var servidor: MockWebServer
    private lateinit var sesionManager: SesionManager
    private lateinit var repositorio: AuthRepository
    private lateinit var cliente: RetrofitClient

    private val token = tokenDePrueba(System.currentTimeMillis() / 1000 + 3600)

    private val respuestaOk = """
        {"token":"$token",
         "usuario":{"id":"u1","nombre":"Camila Andrade","telefono":"0998765432","rol":"PRESTADOR",
                    "estado_verificacion":"NO_SOLICITADA","foto_perfil":null,"activo":true},
         "perfil_prestador_id":"p1"}
    """.trimIndent()

    @Before
    fun preparar() {
        servidor = MockWebServer().apply { start() }
        sesionManager = SesionManager(AlmacenEnMemoria(), CoroutineScope(Dispatchers.Unconfined))
        cliente = RetrofitClient(
            token = sesionManager::tokenActual,
            alExpirar = sesionManager::expirar,
            baseUrl = servidor.url("/").toString()
        )
        repositorio = AuthRepository(cliente.authApi, sesionManager)
    }

    @After
    fun terminar() = servidor.shutdown()

    @Test
    fun login_guarda_la_sesion_y_envia_el_correo_en_minusculas() = runBlocking {
        servidor.enqueue(MockResponse().setBody(respuestaOk))

        val sesion = repositorio.iniciarSesion("  Camila@Correo.COM ", "Aruy2026x")

        val peticion = servidor.takeRequest()
        assertEquals("/auth/login", peticion.path)
        val cuerpo = JsonParser.parseString(peticion.body.readUtf8()).asJsonObject
        assertEquals("camila@correo.com", cuerpo.get("email").asString)
        assertEquals("Aruy2026x", cuerpo.get("password").asString)

        assertEquals("p1", sesion.perfilPrestadorId)
        assertEquals(RolUsuario.PRESTADOR, sesion.usuario.rol)
        assertTrue(sesionManager.estado.value is EstadoSesion.Autenticada)
        assertEquals(token, sesionManager.tokenActual())
    }

    @Test
    fun registro_de_prestador_envia_oficio_y_zona() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(201).setBody(respuestaOk))

        repositorio.registrar(
            DatosRegistro(
                nombre = " Camila Andrade ",
                correo = "Camila@Correo.com",
                celular = "099 876-5432",
                password = "Aruy2026x",
                rol = RolUsuario.PRESTADOR,
                oficio = "Plomería",
                zona = "Norte"
            )
        )

        val cuerpo = JsonParser.parseString(servidor.takeRequest().body.readUtf8()).asJsonObject
        assertEquals("Camila Andrade", cuerpo.get("nombre").asString)
        assertEquals("0998765432", cuerpo.get("telefono").asString)
        assertEquals("PRESTADOR", cuerpo.get("rol").asString)
        assertEquals("Plomería", cuerpo.get("oficio_principal").asString)
        assertEquals("Norte", cuerpo.get("zona_cobertura").asString)
    }

    @Test
    fun registro_de_contratante_no_envia_datos_de_prestador() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(201).setBody(respuestaOk))

        repositorio.registrar(
            DatosRegistro(
                nombre = "Lucía Cevallos",
                correo = "lucia@correo.com",
                celular = "0998765432",
                password = "Aruy2026x",
                rol = RolUsuario.CONTRATANTE,
                oficio = "Plomería",
                zona = "Norte"
            )
        )

        val cuerpo = JsonParser.parseString(servidor.takeRequest().body.readUtf8()).asJsonObject
        assertEquals("CONTRATANTE", cuerpo.get("rol").asString)
        assertFalse(cuerpo.has("oficio_principal"))
        assertFalse(cuerpo.has("zona_cobertura"))
    }

    @Test
    fun login_con_clave_incorrecta_da_mensaje_claro_y_no_cierra_nada() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Correo o contrasena incorrectos"}"""))

        try {
            repositorio.iniciarSesion("camila@correo.com", "mala")
            fail("Debía fallar con 401")
        } catch (e: HttpException) {
            val estado = AuthViewModel.mensajeDeLogin(e)
            assertEquals("El correo o la contraseña no son correctos.", estado.error)
        }
        assertEquals(EstadoSesion.Cargando, sesionManager.estado.value)
        assertNull(sesionManager.aviso.value)
    }

    @Test
    fun registro_con_correo_repetido_marca_el_campo_del_correo() = runBlocking {
        servidor.enqueue(MockResponse().setResponseCode(409).setBody("""{"error":"Ese correo ya esta registrado"}"""))

        try {
            repositorio.registrar(
                DatosRegistro("Lucía Cevallos", "lucia@correo.com", "0998765432", "Aruy2026x", RolUsuario.CONTRATANTE)
            )
            fail("Debía fallar con 409")
        } catch (e: HttpException) {
            val estado = AuthViewModel.mensajeDeRegistro(e)
            assertNotNull(estado.errorCorreo)
            assertNull(estado.error)
        }
    }

    @Test
    fun las_peticiones_con_sesion_llevan_el_token_bearer() = runBlocking {
        servidor.enqueue(MockResponse().setBody(respuestaOk))
        servidor.enqueue(MockResponse().setBody("[]"))
        repositorio.iniciarSesion("camila@correo.com", "Aruy2026x")
        servidor.takeRequest() // el login no lleva token

        cliente.servicioApi.obtenerServicios()

        assertEquals("Bearer $token", servidor.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun un_401_con_token_cierra_la_sesion() = runBlocking {
        servidor.enqueue(MockResponse().setBody(respuestaOk))
        servidor.enqueue(MockResponse().setResponseCode(401).setBody("""{"error":"Token invalido"}"""))
        repositorio.iniciarSesion("camila@correo.com", "Aruy2026x")

        try {
            cliente.servicioApi.obtenerServicios()
            fail("Debía fallar con 401")
        } catch (_: HttpException) {
        }

        assertEquals(EstadoSesion.SinSesion, sesionManager.estado.value)
        assertEquals(SesionManager.MENSAJE_VENCIDA, sesionManager.aviso.value)
        assertNull(sesionManager.tokenActual())
    }
}
