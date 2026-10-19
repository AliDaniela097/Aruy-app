package ec.edu.puce.aruy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favoritos")
data class FavoritoEntity(
    @PrimaryKey
    val servicioId: String,
    val fechaGuardado: Long = System.currentTimeMillis()
)