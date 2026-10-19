package ec.edu.puce.aruy.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.domain.model.Usuario
import ec.edu.puce.aruy.ui.viewmodel.UsuarioViewModel

@Composable
fun UsuarioScreen(
    viewModel: UsuarioViewModel
) {
    val estado by viewModel.estadoUsuario.collectAsState()

    UsuarioContenido(estado = estado)
}

@Composable
fun UsuarioContenido(
    estado: EstadoApi<Usuario>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (estado) {

            is EstadoApi.Inactivo -> {
                Text("Todavía no se ha consultado ningún usuario.")
            }

            is EstadoApi.Cargando -> {
                CircularProgressIndicator()

                Spacer(modifier = Modifier.height(16.dp))

                Text("Cargando usuario...")
            }

            is EstadoApi.Exito -> {
                Text(
                    text = "Usuario encontrado",
                    style = MaterialTheme.typography.headlineSmall
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text("Nombre: ${estado.datos.nombre}")
                Text("Teléfono: ${estado.datos.telefono}")
                Text("Rol: ${estado.datos.rol}")
                Text(
                    "Verificación: ${estado.datos.estadoVerificacion}"
                )
            }

            is EstadoApi.Error -> {
                Text(
                    text = "No se pudo obtener el usuario",
                    color = MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(estado.mensaje)
            }
        }
    }
}