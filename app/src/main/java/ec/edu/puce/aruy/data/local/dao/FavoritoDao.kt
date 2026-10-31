package ec.edu.puce.aruy.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ec.edu.puce.aruy.data.local.entity.BloqueadoEntity
import ec.edu.puce.aruy.data.local.entity.ContactoEntity
import ec.edu.puce.aruy.data.local.entity.FavoritoEntity
import ec.edu.puce.aruy.data.local.entity.ReporteEntity
import ec.edu.puce.aruy.data.local.entity.VistoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoritoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun guardarFavorito(favorito: FavoritoEntity)

    @Query("SELECT * FROM favoritos WHERE usuarioId = :usuarioId ORDER BY fechaGuardado DESC")
    fun obtenerFavoritos(usuarioId: String): Flow<List<FavoritoEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favoritos WHERE usuarioId = :usuarioId AND servicioId = :servicioId)")
    fun esFavorito(usuarioId: String, servicioId: String): Flow<Boolean>

    @Query("DELETE FROM favoritos WHERE usuarioId = :usuarioId AND servicioId = :servicioId")
    suspend fun eliminarFavorito(usuarioId: String, servicioId: String)

    @Query("DELETE FROM favoritos WHERE usuarioId = :usuarioId")
    suspend fun borrarTodo(usuarioId: String)
}

@Dao
interface BloqueadoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun bloquear(bloqueado: BloqueadoEntity)

    @Query("DELETE FROM bloqueados WHERE usuarioId = :usuarioId AND prestadorId = :prestadorId")
    suspend fun desbloquear(usuarioId: String, prestadorId: String)

    @Query("SELECT * FROM bloqueados WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    fun observar(usuarioId: String): Flow<List<BloqueadoEntity>>

    @Query("DELETE FROM bloqueados WHERE usuarioId = :usuarioId")
    suspend fun borrarTodo(usuarioId: String)
}

@Dao
interface ContactoDao {

    @Insert
    suspend fun insertar(contacto: ContactoEntity)

    @Query("SELECT * FROM contactos WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    fun observar(usuarioId: String): Flow<List<ContactoEntity>>

    @Query("DELETE FROM contactos WHERE usuarioId = :usuarioId")
    suspend fun borrarTodo(usuarioId: String)
}

@Dao
interface ReporteDao {

    @Insert
    suspend fun insertar(reporte: ReporteEntity)

    @Query("SELECT * FROM reportes WHERE usuarioId = :usuarioId ORDER BY fecha DESC")
    fun observar(usuarioId: String): Flow<List<ReporteEntity>>

    @Query("DELETE FROM reportes WHERE usuarioId = :usuarioId")
    suspend fun borrarTodo(usuarioId: String)
}

@Dao
interface VistoDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun registrar(visto: VistoEntity)

    @Query("SELECT * FROM vistos WHERE usuarioId = :usuarioId ORDER BY fecha DESC LIMIT :limite")
    fun observar(usuarioId: String, limite: Int): Flow<List<VistoEntity>>

    @Query("DELETE FROM vistos WHERE usuarioId = :usuarioId")
    suspend fun borrarTodo(usuarioId: String)
}
