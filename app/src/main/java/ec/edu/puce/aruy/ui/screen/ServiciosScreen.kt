package ec.edu.puce.aruy.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.domain.model.Servicio
import ec.edu.puce.aruy.ui.viewmodel.ServicioViewModel
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import ec.edu.puce.aruy.domain.model.CategoriaServicio

@Composable
fun ServiciosScreen(
    viewModel: ServicioViewModel
) {
    val estado by viewModel.estadoServicios.collectAsState()

    ServiciosContenido(estado = estado)
}

@Composable
fun ServiciosContenido(
    estado: EstadoApi<List<Servicio>>
) {
    when (estado) {

        is EstadoApi.Inactivo -> {
            EstadoServiciosMensaje(
                "Todavía no se han consultado servicios."
            )
        }

        is EstadoApi.Cargando -> {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        is EstadoApi.Exito -> {

            var busqueda by remember {
                mutableStateOf("")
            }

            var categoriaSeleccionada by remember {
                mutableStateOf<CategoriaServicio?>(null)
            }

            val serviciosFiltrados = estado.datos.filter { servicio ->

                val coincideBusqueda =
                    servicio.titulo.contains(
                        busqueda,
                        ignoreCase = true
                    ) || servicio.descripcion.contains(
                        busqueda,
                        ignoreCase = true
                    )

                val coincideCategoria =
                    categoriaSeleccionada == null ||
                            servicio.categoria == categoriaSeleccionada

                coincideBusqueda && coincideCategoria
            }

            Column(
                modifier = Modifier.fillMaxSize()
            ) {

                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    label = {
                        Text("Buscar servicios")
                    },
                    placeholder = {
                        Text("Ej. Electricista, plomero...")
                    },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )

                if (serviciosFiltrados.isEmpty()) {

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (estado.datos.isEmpty()) {
                                "No hay servicios disponibles."
                            } else {
                                "No se encontraron servicios."
                            }
                        )
                    }

                } else {

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        items(
                            items = serviciosFiltrados,
                            key = { it.id }
                        ) { servicio ->
                            ServicioItem(servicio)
                        }
                    }
                }
            }
        }

        is EstadoApi.Error -> {
            EstadoServiciosMensaje(estado.mensaje)
        }
    }
}

@Composable
private fun EstadoServiciosMensaje(
    mensaje: String
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(mensaje)
    }
}

@Composable
private fun ServicioItem(
    servicio: Servicio
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = servicio.titulo,
                style = MaterialTheme.typography.titleMedium
            )

            Text(
                text = servicio.descripcion
            )

            Text(
                text = "Categoría: ${servicio.categoria}"
            )

            Text(
                text = servicio.precioReferencial?.let {
                    "Precio referencial: $${"%.2f".format(it)}"
                } ?: "Precio a convenir"
            )

            Text(
                text = if (servicio.disponible) {
                    "Disponible"
                } else {
                    "No disponible"
                }
            )
        }
    }
}


