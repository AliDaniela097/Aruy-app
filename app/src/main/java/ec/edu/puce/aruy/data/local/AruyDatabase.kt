package ec.edu.puce.aruy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import ec.edu.puce.aruy.data.local.dao.BloqueadoDao
import ec.edu.puce.aruy.data.local.dao.ContactoDao
import ec.edu.puce.aruy.data.local.dao.FavoritoDao
import ec.edu.puce.aruy.data.local.dao.ReporteDao
import ec.edu.puce.aruy.data.local.dao.VistoDao
import ec.edu.puce.aruy.data.local.entity.BloqueadoEntity
import ec.edu.puce.aruy.data.local.entity.ContactoEntity
import ec.edu.puce.aruy.data.local.entity.FavoritoEntity
import ec.edu.puce.aruy.data.local.entity.ReporteEntity
import ec.edu.puce.aruy.data.local.entity.VistoEntity

@Database(
    entities = [
        FavoritoEntity::class,
        BloqueadoEntity::class,
        ContactoEntity::class,
        ReporteEntity::class,
        VistoEntity::class
    ],
    version = 2,
    exportSchema = true
)
abstract class AruyDatabase : RoomDatabase() {

    abstract fun favoritoDao(): FavoritoDao
    abstract fun bloqueadoDao(): BloqueadoDao
    abstract fun contactoDao(): ContactoDao
    abstract fun reporteDao(): ReporteDao
    abstract fun vistoDao(): VistoDao
}
