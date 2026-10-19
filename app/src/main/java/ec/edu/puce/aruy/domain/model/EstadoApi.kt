package ec.edu.puce.aruy.domain.model

sealed class EstadoApi<out T> {

    data object Inactivo : EstadoApi<Nothing>()

    data object Cargando : EstadoApi<Nothing>()

    data class Exito<T>(
        val datos: T
    ) : EstadoApi<T>()

    data class Error(
        val mensaje: String
    ) : EstadoApi<Nothing>()
}