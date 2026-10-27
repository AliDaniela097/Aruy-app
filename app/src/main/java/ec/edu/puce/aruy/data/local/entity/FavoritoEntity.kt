package ec.edu.puce.aruy.data.local.entity

import androidx.room.Entity

/** Servicio guardado como favorito por una persona (cada cuenta tiene los suyos en este teléfono). */
@Entity(tableName = "favoritos", primaryKeys = ["usuarioId", "servicioId"])
data class FavoritoEntity(
    val usuarioId: String,
    val servicioId: String,
    val fechaGuardado: Long = System.currentTimeMillis()
)

/** Prestador bloqueado: sus servicios dejan de verse en el catálogo. */
@Entity(tableName = "bloqueados", primaryKeys = ["usuarioId", "prestadorId"])
data class BloqueadoEntity(
    val usuarioId: String,
    val prestadorId: String,
    val nombre: String,
    val fecha: Long = System.currentTimeMillis()
)

/** Historial de prestadores contactados por WhatsApp desde la app. */
@Entity(tableName = "contactos")
data class ContactoEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: String,
    val prestadorId: String,
    val servicioId: String?,
    val nombre: String,
    val mensaje: String,
    val fecha: Long = System.currentTimeMillis()
)

/** Reporte hecho por la persona. Por ahora solo se guarda en el teléfono: el backend no tiene reportes. */
@Entity(tableName = "reportes")
data class ReporteEntity(
    @androidx.room.PrimaryKey(autoGenerate = true) val id: Long = 0,
    val usuarioId: String,
    /** "SERVICIO" o "PERFIL". */
    val tipo: String,
    val objetivoId: String,
    val objetivo: String,
    val motivo: String,
    val detalle: String?,
    val fecha: Long = System.currentTimeMillis()
)

/** "Vistos recientemente". */
@Entity(tableName = "vistos", primaryKeys = ["usuarioId", "servicioId"])
data class VistoEntity(
    val usuarioId: String,
    val servicioId: String,
    val fecha: Long = System.currentTimeMillis()
)
