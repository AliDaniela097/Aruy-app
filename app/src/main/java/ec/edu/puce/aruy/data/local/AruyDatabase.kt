package ec.edu.puce.aruy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ec.edu.puce.aruy.data.local.dao.FavoritoDao
import ec.edu.puce.aruy.data.local.entity.FavoritoEntity

@Database(
    entities = [FavoritoEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AruyDatabase : RoomDatabase() {

    abstract fun favoritoDao(): FavoritoDao
}