package ec.edu.puce.aruy.data

import ec.edu.puce.aruy.data.remote.RetrofitClient
import ec.edu.puce.aruy.data.repository.PerfilRepository
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.model.EstadoSesion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Sesion
import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.ui.viewmodel.BajaCuentaViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EliminarCuentaTest {

    private lateinit var servidor: MockWebServer
    private lateinit var sesion: SesionManager
    private lateinit var almacen: AlmacenEnMemoria
    private val borrados = mutableListOf<String>()

    @Before
    fun antes() = runBlocking {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        servidor = MockWebServer().apply { start() }
        almacen = AlmacenEnMemoria()
        sesion = SesionManager(almacen, CoroutineScope(Dispatchers.Unconfined))
        sesion.iniciar(
            Sesion(
                token = tokenDePrueba(System.currentTimeMillis() / 1000 + 3600),
                usuario = Usuario(id = "u1", nombre = "Camila Andrade", telefono = "0998765432", rol = RolUsuario.PRESTADOR),
                perfilPrestadorId = "p1"
            )
        )
    }

    @After
    fun despues() {
        servidor.shutdown()
        Dispatchers.resetMain()
    }

    private fun viewModel(): BajaCuentaViewModel {
        val cliente = RetrofitClient({ sesion.tokenActual() }, {}, servidor.url("/").toString())
        val perfiles = PerfilRepository(cliente.usuarioApi, cliente.prestadorApi)
        return BajaCuentaViewModel(perfiles, sesion) { borrados += it }
    }

    /** La respuesta de Retrofit llega en otro hilo: espera a que el ViewModel termine. */
    private fun esperar(condicion: () -> Boolean) {
        val limite = System.currentTimeMillis() + 5_000
        while (!condicion() && System.currentTimeMillis() < limite) Thread.sleep(20)
    }

    @Test
    fun eliminar_la_cuenta_borra_los_datos_locales_y_cierra_la_sesion() {
        servidor.enqueue(MockResponse().setResponseCode(204))
        val vm = viewModel()

        vm.cambiarConfirmacion("eliminar")
        vm.eliminarCuenta()

        esperar { sesion.estado.value is EstadoSesion.SinSesion }
        assertEquals("DELETE", servidor.takeRequest().method)
        assertEquals(listOf("u1"), borrados)
        assertTrue(sesion.estado.value is EstadoSesion.SinSesion)
        assertNull(almacen.guardada)
    }

    @Test
    fun si_el_servidor_falla_no_se_borra_nada_en_el_telefono() {
        servidor.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"Error al eliminar tu cuenta"}"""))
        val vm = viewModel()

        vm.cambiarConfirmacion("ELIMINAR")
        vm.eliminarCuenta()

        esperar { vm.ui.value.error != null }
        assertTrue(borrados.isEmpty())
        assertTrue(sesion.estado.value is EstadoSesion.Autenticada)
        assertTrue(vm.ui.value.error != null)
    }

    @Test
    fun sin_escribir_la_palabra_no_llama_al_servidor() {
        val vm = viewModel()

        vm.cambiarConfirmacion("BAJA")
        vm.eliminarCuenta()

        assertEquals(0, servidor.requestCount)
        assertTrue(borrados.isEmpty())
    }
}
