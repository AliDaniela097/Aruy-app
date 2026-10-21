package ec.edu.puce.aruy.ui.navigation

sealed class PantallaAruy(
    val ruta: String,
    val titulo: String
) {

    data object Servicios : PantallaAruy(
        ruta = "servicios",
        titulo = "Servicios"
    )

    data object Favoritos : PantallaAruy(
        ruta = "favoritos",
        titulo = "Favoritos"
    )

    data object Usuario : PantallaAruy(
        ruta = "usuario",
        titulo = "Usuario"
    )
}
