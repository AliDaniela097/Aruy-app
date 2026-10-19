package ec.edu.puce.aruy.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.domain.model.EstadoVerificacion
import ec.edu.puce.aruy.domain.model.RolUsuario
import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.ui.theme.AruyTheme

@Preview(showBackground = true, name = "Estado Inactivo")
@Composable
fun PreviewUsuarioInactivo() {
    AruyTheme {
        UsuarioContenido(
            estado = EstadoApi.Inactivo
        )
    }
}

@Preview(showBackground = true, name = "Estado Cargando")
@Composable
fun PreviewUsuarioCargando() {
    AruyTheme {
        UsuarioContenido(
            estado = EstadoApi.Cargando
        )
    }
}

@Preview(showBackground = true, name = "Estado Exito")
@Composable
fun PreviewUsuarioExito() {
    val usuario = Usuario(
        id = "usuario-prueba",
        nombre = "María López",
        telefono = "0991234567",
        rol = RolUsuario.PRESTADOR,
        estadoVerificacion = EstadoVerificacion.VERIFICADA
    )

    AruyTheme {
        UsuarioContenido(
            estado = EstadoApi.Exito(usuario)
        )
    }
}

@Preview(showBackground = true, name = "Estado Error")
@Composable
fun PreviewUsuarioError() {
    AruyTheme {
        UsuarioContenido(
            estado = EstadoApi.Error(
                "No se pudo conectar con el servidor"
            )
        )
    }
}