package ec.edu.puce.aruy.domain.catalogo

import ec.edu.puce.aruy.domain.model.CategoriaServicio
import ec.edu.puce.aruy.domain.model.ServicioConPrestador
import java.text.Normalizer
import java.util.Locale

enum class OrdenCatalogo(val etiqueta: String, val descripcion: String) {
    MEJOR_CALIFICACION("Mejor calificación", "Las mejores opiniones primero"),
    MENOR_PRECIO("Menor precio", "Desde el precio referencial más bajo"),
    ALFABETICO("Orden alfabético", "De la A a la Z por título")
}

/** Filtros de la pantalla Buscar (`buscar.js` del prototipo, sin distancia ni mapa). */
data class FiltrosCatalogo(
    val texto: String = "",
    val categoria: CategoriaServicio? = null,
    val zona: String? = null,
    /** 0 = todas, 4 = "4 o más", 4.5 = "4,5 o más". */
    val calificacionMinima: Double = 0.0,
    val soloDisponibles: Boolean = false,
    val soloVerificados: Boolean = false,
    /** null = sin límite. */
    val precioMaximo: Double? = null,
    val orden: OrdenCatalogo = OrdenCatalogo.MEJOR_CALIFICACION
) {
    /** Cuántos filtros hay activos (sin contar el texto ni el orden), para el contador del botón. */
    val cantidadActivos: Int
        get() = listOf(
            categoria != null,
            zona != null,
            calificacionMinima > 0,
            soloDisponibles,
            soloVerificados,
            precioMaximo != null
        ).count { it }
}

object FiltroCatalogo {

    /** Minúsculas y sin tildes: "Plomería" y "plomeria" son lo mismo al buscar. */
    fun normalizar(texto: String): String =
        Normalizer.normalize(texto.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .trim()

    fun aplicar(
        items: List<ServicioConPrestador>,
        filtros: FiltrosCatalogo,
        prestadoresBloqueados: Set<String> = emptySet()
    ): List<ServicioConPrestador> {
        val palabras = normalizar(filtros.texto).split(Regex("\\s+")).filter { it.isNotEmpty() }
        val zona = filtros.zona?.let(::normalizar)

        val filtrados = items.filter { item ->
            val s = item.servicio
            val p = item.prestador
            when {
                s.prestadorId in prestadoresBloqueados -> false
                filtros.categoria != null && s.categoria != filtros.categoria -> false
                zona != null && (p == null || !normalizar(p.zona).contains(zona)) -> false
                filtros.calificacionMinima > 0 && (p?.promedio ?: 0.0) < filtros.calificacionMinima -> false
                filtros.soloDisponibles && !disponibleAhora(item) -> false
                filtros.soloVerificados && p?.verificado != true -> false
                filtros.precioMaximo != null && s.precioReferencial != null &&
                    s.precioReferencial > filtros.precioMaximo -> false
                palabras.isNotEmpty() && !coincide(item, palabras) -> false
                else -> true
            }
        }
        return ordenar(filtrados, filtros.orden)
    }

    /** "Disponible ahora": el servicio no está pausado y su prestador recibe solicitudes. */
    fun disponibleAhora(item: ServicioConPrestador): Boolean =
        item.servicio.disponible && (item.prestador?.disponible ?: true)

    private fun coincide(item: ServicioConPrestador, palabras: List<String>): Boolean {
        val pajar = normalizar(
            listOf(
                item.servicio.titulo,
                item.servicio.descripcion,
                item.servicio.categoria.etiqueta,
                item.prestador?.nombre.orEmpty(),
                item.prestador?.oficio.orEmpty()
            ).joinToString(" ")
        )
        return palabras.all { palabra ->
            // "plomeros" o "instalaciones" también encuentran "plomero" e "instalacion"
            pajar.contains(palabra) ||
                (palabra.length > 3 && (pajar.contains(palabra.removeSuffix("es")) || pajar.contains(palabra.removeSuffix("s"))))
        }
    }

    fun ordenar(items: List<ServicioConPrestador>, orden: OrdenCatalogo): List<ServicioConPrestador> =
        when (orden) {
            OrdenCatalogo.MEJOR_CALIFICACION -> items.sortedWith(
                compareByDescending<ServicioConPrestador> { it.prestador?.promedio ?: -1.0 }
                    .thenByDescending { it.prestador?.totalCalificaciones ?: 0 }
                    .thenBy { normalizar(it.servicio.titulo) }
            )
            OrdenCatalogo.MENOR_PRECIO -> items.sortedWith(
                compareBy<ServicioConPrestador> { it.servicio.precioReferencial ?: Double.MAX_VALUE }
                    .thenBy { normalizar(it.servicio.titulo) }
            )
            OrdenCatalogo.ALFABETICO -> items.sortedBy { normalizar(it.servicio.titulo) }
        }
}
