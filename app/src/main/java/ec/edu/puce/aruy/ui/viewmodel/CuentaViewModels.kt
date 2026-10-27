package ec.edu.puce.aruy.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ec.edu.puce.aruy.data.local.PreferenciasStore
import ec.edu.puce.aruy.data.local.PreferenciasUi
import ec.edu.puce.aruy.data.remote.util.ApiErrorHandler
import ec.edu.puce.aruy.data.remote.util.MensajesPerfil
import ec.edu.puce.aruy.data.repository.DatosPerfilPrestador
import ec.edu.puce.aruy.data.repository.PerfilRepository
import ec.edu.puce.aruy.data.repository.SuscripcionRepository
import ec.edu.puce.aruy.data.sesion.SesionManager
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Suscripcion
import ec.edu.puce.aruy.domain.model.TipoPlan
import ec.edu.puce.aruy.domain.validation.Validaciones
import ec.edu.puce.aruy.domain.validation.ValidacionesPerfil
import ec.edu.puce.aruy.ui.theme.ModoTema
import ec.edu.puce.aruy.ui.theme.TamanoTexto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// ---------------------------------------------------------------- Editar perfil

data class EditarPerfilUi(
    val cargando: Boolean = true,
    val esPrestador: Boolean = false,
    val email: String? = null,
    val nombre: String = "",
    val celular: String = "",
    val oficio: String = "",
    val zona: String = "",
    val descripcion: String = "",
    val anios: String = "",
    val errorNombre: String? = null,
    val errorCelular: String? = null,
    val errorOficio: String? = null,
    val errorZona: String? = null,
    val errorAnios: String? = null,
    val guardando: Boolean = false,
    val finalizado: Boolean = false
)

class EditarPerfilViewModel(
    private val repositorio: PerfilRepository,
    private val sesion: SesionManager
) : ViewModel() {

    private val _ui = MutableStateFlow(EditarPerfilUi())
    val ui: StateFlow<EditarPerfilUi> = _ui.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        _ui.update { it.copy(cargando = true) }
        viewModelScope.launch {
            try {
                val datos = repositorio.misDatos()
                val prestador = datos.usuario.rol == RolUsuario.PRESTADOR
                val perfil = if (prestador) runCatching { repositorio.miPerfil() }.getOrNull() else null
                _ui.update {
                    it.copy(
                        cargando = false,
                        esPrestador = prestador,
                        email = datos.email,
                        nombre = datos.usuario.nombre,
                        celular = datos.usuario.telefono,
                        oficio = perfil?.oficioPrincipal.orEmpty(),
                        zona = perfil?.zonaCobertura.orEmpty(),
                        descripcion = perfil?.descripcionProfesional.orEmpty(),
                        anios = perfil?.aniosExperiencia?.toString().orEmpty()
                    )
                }
                if (prestador && perfil == null) _mensajes.tryEmit("No pudimos cargar tu perfil de prestador.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false) }
                _mensajes.tryEmit(ApiErrorHandler.obtenerMensaje(e))
            }
        }
    }

    fun cambiarNombre(v: String) = _ui.update { it.copy(nombre = v.take(Validaciones.NOMBRE_MAX), errorNombre = null) }
    fun cambiarCelular(v: String) = _ui.update { it.copy(celular = v.take(20), errorCelular = null) }
    fun cambiarOficio(v: String) = _ui.update { it.copy(oficio = v.take(ValidacionesPerfil.OFICIO_MAX), errorOficio = null) }
    fun cambiarZona(v: String) = _ui.update { it.copy(zona = v.take(ValidacionesPerfil.ZONA_MAX), errorZona = null) }
    fun cambiarDescripcion(v: String) = _ui.update { it.copy(descripcion = v.take(ValidacionesPerfil.DESCRIPCION_MAX)) }
    fun cambiarAnios(v: String) = _ui.update { it.copy(anios = v.take(2), errorAnios = null) }

    fun guardar() {
        val u = _ui.value
        if (u.guardando || u.cargando) return
        val anios = ValidacionesPerfil.anios(u.anios)
        val validado = u.copy(
            errorNombre = Validaciones.nombre(u.nombre),
            errorCelular = Validaciones.celular(u.celular),
            errorOficio = if (u.esPrestador) ValidacionesPerfil.oficio(u.oficio) else null,
            errorZona = if (u.esPrestador) ValidacionesPerfil.zona(u.zona) else null,
            errorAnios = if (u.esPrestador) anios.error else null
        )
        _ui.value = validado
        if (listOf(validado.errorNombre, validado.errorCelular, validado.errorOficio, validado.errorZona, validado.errorAnios).any { it != null }) return

        _ui.update { it.copy(guardando = true) }
        viewModelScope.launch {
            try {
                val usuario = repositorio.actualizarMisDatos(u.nombre, u.celular)
                sesion.actualizarUsuario(usuario)
                if (u.esPrestador) {
                    repositorio.actualizarMiPerfil(DatosPerfilPrestador(u.oficio, u.zona, u.descripcion, anios.valor))
                }
                _ui.update { it.copy(guardando = false, finalizado = true) }
                _mensajes.tryEmit("Cambios guardados.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(guardando = false) }
                _mensajes.tryEmit(MensajesPerfil.alGuardarPerfil(e))
            }
        }
    }
}

// ---------------------------------------------------------------- Dar de baja

data class BajaUi(val confirmacion: String = "", val error: String? = null, val procesando: Boolean = false)

/** Darse de baja: la cuenta queda inactiva en el servidor y se cierra la sesión en este teléfono. */
class BajaCuentaViewModel(
    private val repositorio: PerfilRepository,
    private val sesion: SesionManager
) : ViewModel() {

    private val _ui = MutableStateFlow(BajaUi())
    val ui: StateFlow<BajaUi> = _ui.asStateFlow()

    fun cambiarConfirmacion(v: String) = _ui.update { it.copy(confirmacion = v, error = null) }

    fun darDeBaja() {
        val actual = _ui.value
        if (actual.procesando) return
        if (!ValidacionesPerfil.confirmaBaja(actual.confirmacion)) {
            _ui.update { it.copy(error = "Escribe la palabra ${ValidacionesPerfil.PALABRA_BAJA} para continuar.") }
            return
        }
        _ui.update { it.copy(procesando = true, error = null) }
        viewModelScope.launch {
            try {
                repositorio.darDeBaja()
                // Cerrar la sesión lleva solo a la bienvenida (AruyApp observa el estado)
                sesion.cerrar()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(procesando = false, error = MensajesPerfil.alGuardarPerfil(e)) }
            }
        }
    }
}

// ---------------------------------------------------------------- Aruy Pro

data class ProUi(
    val cargando: Boolean = true,
    val error: String? = null,
    val suscripcion: Suscripcion? = null,
    val renovacionAutomatica: Boolean = false,
    val procesando: Boolean = false
)

class AruyProViewModel(private val repositorio: SuscripcionRepository) : ViewModel() {

    private val _ui = MutableStateFlow(ProUi())
    val ui: StateFlow<ProUi> = _ui.asStateFlow()

    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 4)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    init {
        cargar()
    }

    fun cargar() {
        _ui.update { it.copy(cargando = true, error = null) }
        viewModelScope.launch {
            try {
                _ui.update { it.copy(cargando = false, suscripcion = repositorio.miSuscripcion()) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(cargando = false, error = MensajesPerfil.enSuscripciones(e)) }
            }
        }
    }

    fun cambiarRenovacion(valor: Boolean) = _ui.update { it.copy(renovacionAutomatica = valor) }

    fun suscribirse(plan: TipoPlan) {
        if (_ui.value.procesando) return
        _ui.update { it.copy(procesando = true) }
        viewModelScope.launch {
            try {
                val nueva = repositorio.suscribirse(plan, _ui.value.renovacionAutomatica)
                _ui.update { it.copy(procesando = false, suscripcion = nueva) }
                _mensajes.tryEmit("Aruy Pro activado. Es una suscripción simulada: no se cobró nada.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(procesando = false) }
                _mensajes.tryEmit(MensajesPerfil.enSuscripciones(e))
                // Si ya había una vigente en el servidor, se muestra la real
                if (e is retrofit2.HttpException && e.code() == 409) cargar()
            }
        }
    }

    fun cancelar() {
        val id = _ui.value.suscripcion?.id ?: return
        if (_ui.value.procesando) return
        _ui.update { it.copy(procesando = true) }
        viewModelScope.launch {
            try {
                val cancelada = repositorio.cancelar(id)
                _ui.update { it.copy(procesando = false, suscripcion = cancelada) }
                _mensajes.tryEmit("Cancelaste tu suscripción.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _ui.update { it.copy(procesando = false) }
                _mensajes.tryEmit(MensajesPerfil.enSuscripciones(e))
            }
        }
    }
}

// ---------------------------------------------------------------- Ajustes

/** Tema, tamaño del texto y animaciones: se guardan en el teléfono y se aplican al instante. */
class AjustesViewModel(private val almacen: PreferenciasStore) : ViewModel() {

    val preferencias: StateFlow<PreferenciasUi> =
        almacen.preferencias.stateIn(viewModelScope, SharingStarted.Eagerly, PreferenciasUi())

    fun elegirTema(modo: ModoTema) {
        viewModelScope.launch { almacen.guardarTema(modo) }
    }

    fun elegirTamanoTexto(tamano: TamanoTexto) {
        viewModelScope.launch { almacen.guardarTamanoTexto(tamano) }
    }

    fun reducirAnimaciones(reducir: Boolean) {
        viewModelScope.launch { almacen.guardarReducirAnimaciones(reducir) }
    }
}
