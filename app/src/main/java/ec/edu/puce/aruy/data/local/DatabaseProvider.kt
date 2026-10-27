package ec.edu.puce.aruy.data.local

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: AruyDatabase? = null

    fun getDatabase(context: Context): AruyDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                AruyDatabase::class.java,
                "aruy_local_db"
            )
                // La versión 1 solo tenía favoritos de prueba sin cuenta; al pasar a la versión 2
                // (datos por cuenta) se empieza de cero. Desde la primera versión publicada se
                // deben escribir migraciones reales.
                .fallbackToDestructiveMigration()
                .build()

            INSTANCE = instance
            instance
        }
    }
}
