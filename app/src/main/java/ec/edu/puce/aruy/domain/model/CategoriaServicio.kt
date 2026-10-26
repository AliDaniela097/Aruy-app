package ec.edu.puce.aruy.domain.model

/** Las 7 categorías que valida el backend (`CATEGORIAS` en validar.js). */
enum class CategoriaServicio(val etiqueta: String) {
    PLOMERIA("Plomería"),
    ELECTRICIDAD("Electricidad"),
    ALBANILERIA("Albañilería"),
    PINTURA("Pintura"),
    CARPINTERIA("Carpintería"),
    BELLEZA("Belleza"),
    OTROS("Otros")
}
