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
            ).build()

            INSTANCE = instance
            instance
        }
    }
}