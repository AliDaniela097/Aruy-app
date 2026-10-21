package ec.edu.puce.aruy.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.EstadoApi
import ec.edu.puce.aruy.domain.model.Servicio
import ec.edu.puce.aruy.ui.theme.AruyTheme

@Preview(
    showBackground = true,
    name = "Lista de servicios"
)
@Composable
fun PreviewListaServicios() {

    val servicios = listOf(
        Servicio(
            id = "servicio-001",
            prestadorId = "prestador-001",
            titulo = "Instalación eléctrica",
            descripcion = "Instalaciones eléctricas residenciales.",
            categoria = CategoriaServicio.ELECTRICIDAD,
            precioReferencial = 25.0,
            disponible = true
        ),
        Servicio(
            id = "servicio-002",
            prestadorId = "prestador-002",
            titulo = "Reparación de tuberías",
            descripcion = "Reparación de fugas de agua.",
            categoria = CategoriaServicio.PLOMERIA,
            precioReferencial = null,
            disponible = true
        )
    )

    AruyTheme {
        ServiciosContenido(
            estado = EstadoApi.Exito(servicios)
        )
    }
}

@Preview(
    showBackground = true,
    name = "Sin servicios"
)
@Composable
fun PreviewServiciosVacios() {
    AruyTheme {
        ServiciosContenido(
            estado = EstadoApi.Exito(emptyList())
        )
    }
}

@Preview(
    showBackground = true,
    name = "Cargando servicios"
)
@Composable
fun PreviewServiciosCargando() {
    AruyTheme {
        ServiciosContenido(
            estado = EstadoApi.Cargando
        )
    }
}

@Preview(
    showBackground = true,
    name = "Error de servicios"
)
@Composable
fun PreviewServiciosError() {
    AruyTheme {
        ServiciosContenido(
            estado = EstadoApi.Error(
                "No se pudo conectar con el servidor."
            )
        )
    }
}
