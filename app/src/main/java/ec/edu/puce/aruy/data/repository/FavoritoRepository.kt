package ec.edu.puce.aruy.data.repository

import ec.edu.puce.aruy.data.local.dao.FavoritoDao
import ec.edu.puce.aruy.data.local.entity.FavoritoEntity
import kotlinx.coroutines.flow.Flow

class FavoritoRepository(
    private val favoritoDao: FavoritoDao
) {

    fun obtenerFavoritos(): Flow<List<FavoritoEntity>> {
        return favoritoDao.obtenerFavoritos()
    }

    fun esFavorito(servicioId: String): Flow<Boolean> {
        return favoritoDao.esFavorito(servicioId)
    }

    suspend fun guardarFavorito(servicioId: String) {
        val favorito = FavoritoEntity(
            servicioId = servicioId
        )
        favoritoDao.guardarFavorito(favorito)
    }

    suspend fun eliminarFavorito(servicioId: String) {
        favoritoDao.eliminarFavorito(servicioId)
    }
}