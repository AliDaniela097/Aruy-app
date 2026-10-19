package ec.edu.puce.aruy.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.ui.viewmodel.FavoritoViewModel

@Composable
fun FavoritosScreen(
    viewModel: FavoritoViewModel
) {
    val favoritos by viewModel.favoritos.collectAsState()

    val servicioPrueba = "servicio-prueba-001"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Mis favoritos",
            style = MaterialTheme.typography.headlineMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                viewModel.guardarFavorito(servicioPrueba)
            }
        ) {
            Text("Agregar favorito de prueba")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Total de favoritos: ${favoritos.size}")

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(
                items = favoritos,
                key = { it.servicioId }
            ) { favorito ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = favorito.servicioId,
                        modifier = Modifier.weight(1f)
                    )

                    TextButton(
                        onClick = {
                            viewModel.eliminarFavorito(
                                favorito.servicioId
                            )
                        }
                    ) {
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}