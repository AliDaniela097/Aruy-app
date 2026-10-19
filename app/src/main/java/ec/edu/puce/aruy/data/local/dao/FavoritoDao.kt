package ec.edu.puce.aruy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ec.edu.puce.aruy.data.local.entity.FavoritoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarFavorito(favorito: FavoritoEntity)

    @Query("""
        SELECT * FROM favoritos
        ORDER BY fechaGuardado DESC
    """)
    fun obtenerFavoritos(): Flow<List<FavoritoEntity>>

    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM favoritos
            WHERE servicioId = :servicioId
        )
    """)
    fun esFavorito(servicioId: String): Flow<Boolean>

    @Query("""
        DELETE FROM favoritos
        WHERE servicioId = :servicioId
    """)
    suspend fun eliminarFavorito(servicioId: String)
}