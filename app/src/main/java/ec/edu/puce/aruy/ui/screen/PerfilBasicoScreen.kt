package ec.edu.puce.aruy.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.model.EstadoVerificacion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.ui.components.AruyAvatar
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.BotonSuave
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.TipoPastilla

/**
 * Perfil mínimo: quién tiene la sesión, accesos a seguridad y cierre de sesión.
 * La fase 5 lo reemplaza por el perfil completo (editar, ajustes, Aruy Pro...).
 */
@Composable
fun PerfilBasicoScreen(
    usuario: Usuario,
    onBloqueados: () -> Unit,
    onReportes: () -> Unit,
    onNormas: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    var confirmar by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        AruyAvatar(usuario.nombre, tamano = 96.dp)
        Text(usuario.nombre, style = MaterialTheme.typography.headlineMedium)
        Pastilla(
            if (usuario.rol == RolUsuario.PRESTADOR) "Prestador" else "Contratante",
            tipo = TipoPastilla.INFO
        )
        if (usuario.estadoVerificacion == EstadoVerificacion.VERIFICADA) {
            Pastilla("Verificada", tipo = TipoPastilla.OK)
        }
        if (usuario.rol == RolUsuario.CONTRATANTE) {
            BotonSuave("Personas bloqueadas", onBloqueados, ancho = true)
        }
        BotonSuave("Mis reportes", onReportes, ancho = true)
        BotonSuave("Normas de la comunidad", onNormas, ancho = true)
        BotonContorno("Cerrar sesión", { confirmar = true }, ancho = true)
    }

    if (confirmar) {
        DialogoConfirmacion(
            titulo = "¿Cerrar sesión?",
            texto = "Tus favoritos y tu historial se quedan guardados en este teléfono.",
            textoConfirmar = "Cerrar sesión",
            onConfirmar = { confirmar = false; onCerrarSesion() },
            onCancelar = { confirmar = false }
        )
    }
}
