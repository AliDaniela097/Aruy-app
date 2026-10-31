package ec.edu.puce.aruy.ui.screen.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.BuildConfig
import ec.edu.puce.aruy.domain.model.EstadoVerificacion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.ui.components.AruyAvatar
import ec.edu.puce.aruy.ui.components.AruyInterruptor
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.FilaAjuste
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.TipoPastilla
import ec.edu.puce.aruy.ui.components.TituloGrupo

/** Perfil de la persona con sesión: cuenta, privacidad y cierre de sesión (`perfil.js`). */
@Composable
fun PerfilScreen(
    usuario: Usuario,
    recibeSolicitudes: Boolean,
    onCambiarRecepcion: (Boolean) -> Unit,
    alAbrir: () -> Unit,
    onEditar: () -> Unit,
    onAjustes: () -> Unit,
    onPro: () -> Unit,
    onPermisos: () -> Unit,
    onPrivacidad: () -> Unit,
    onBloqueados: () -> Unit,
    onReportes: () -> Unit,
    onNormas: () -> Unit,
    onBaja: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }
    val prestador = usuario.rol == RolUsuario.PRESTADOR

    LaunchedEffect(Unit) { alAbrir() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AruyAvatar(usuario.nombre, tamano = 96.dp)
            Text(usuario.nombre, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
            EtiquetasPerfil(prestador, usuario)
        }

        if (prestador) {
            AruyTarjeta(modifier = Modifier.padding(horizontal = 16.dp)) {
                AruyInterruptor(
                    titulo = if (recibeSolicitudes) "Recibiendo solicitudes" else "No recibes solicitudes",
                    activado = recibeSolicitudes,
                    onCambio = onCambiarRecepcion,
                    descripcion = if (recibeSolicitudes) "Apareces como «Disponible ahora» en las búsquedas."
                    else "Los clientes verán que no estás disponible por ahora."
                )
            }
        }

        TituloGrupo("Cuenta")
        FilaAjuste(Icons.Filled.Edit, "Editar perfil", onEditar, texto = if (prestador) "Nombre, celular, oficio y descripción" else "Nombre y celular")
        if (prestador) FilaAjuste(Icons.Filled.Star, "Aruy Pro", onPro, texto = "Suscripción simulada: sin cobro real")
        FilaAjuste(Icons.Filled.Settings, "Ajustes", onAjustes, texto = "Tema, tamaño de texto y animaciones")

        TituloGrupo("Privacidad y seguridad")
        FilaAjuste(Icons.Filled.Lock, "Permisos", onPermisos, texto = "Cámara y galería")
        FilaAjuste(Icons.Filled.PrivacyTip, "Mis datos y privacidad", onPrivacidad)
        if (!prestador) FilaAjuste(Icons.Filled.Block, "Personas bloqueadas", onBloqueados)
        FilaAjuste(Icons.Filled.Flag, "Mis reportes", onReportes)
        FilaAjuste(Icons.Filled.Gavel, "Normas de la comunidad", onNormas)

        TituloGrupo("Sesión")
        FilaAjuste(Icons.AutoMirrored.Filled.Logout, "Cerrar sesión", { confirmarSalida = true })
        FilaAjuste(Icons.Filled.DeleteForever, "Eliminar mi cuenta", onBaja, texto = "Borra tu cuenta y tus datos", peligro = true)

        Text(
            "Aruy ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(24.dp)
        )
    }

    if (confirmarSalida) {
        DialogoConfirmacion(
            titulo = "¿Cerrar sesión?",
            texto = "Tus favoritos y tu historial se quedan guardados en este teléfono.",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = { confirmarSalida = false; onCerrarSesion() },
            onCancelar = { confirmarSalida = false }
        )
    }
}

@Composable
private fun EtiquetasPerfil(prestador: Boolean, usuario: Usuario) {
    androidx.compose.foundation.layout.Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Pastilla(if (prestador) "Prestador" else "Contratante", tipo = TipoPastilla.INFO)
        if (usuario.estadoVerificacion == EstadoVerificacion.VERIFICADA) Pastilla("Verificada", tipo = TipoPastilla.OK)
    }
}
