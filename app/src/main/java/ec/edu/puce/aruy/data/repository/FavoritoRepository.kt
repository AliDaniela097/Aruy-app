package ec.edu.puce.aruy.data.repository

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
import kotlinx.coroutines.flow.Flow

class FavoritoRepository(
    private val favoritoDao: FavoritoDao
) {

    fun obtenerFavoritos(usuarioId: String): Flow<List<FavoritoEntity>> =
        favoritoDao.obtenerFavoritos(usuarioId)

    fun esFavorito(usuarioId: String, servicioId: String): Flow<Boolean> =
        favoritoDao.esFavorito(usuarioId, servicioId)

    suspend fun guardarFavorito(usuarioId: String, servicioId: String) {
        favoritoDao.guardarFavorito(FavoritoEntity(usuarioId, servicioId))
    }

    suspend fun eliminarFavorito(usuarioId: String, servicioId: String) {
        favoritoDao.eliminarFavorito(usuarioId, servicioId)
    }

    suspend fun borrarTodos(usuarioId: String) = favoritoDao.borrarTodo(usuarioId)
}

/**
 * Lo que la app guarda solo en el teléfono porque el backend no lo ofrece:
 * prestadores bloqueados, historial de contactos, reportes y servicios vistos.
 */
class ActividadLocalRepository(
    private val bloqueadoDao: BloqueadoDao,
    private val contactoDao: ContactoDao,
    private val reporteDao: ReporteDao,
    private val vistoDao: VistoDao
) {

    fun bloqueados(usuarioId: String): Flow<List<BloqueadoEntity>> = bloqueadoDao.observar(usuarioId)

    suspend fun bloquear(usuarioId: String, prestadorId: String, nombre: String) =
        bloqueadoDao.bloquear(BloqueadoEntity(usuarioId, prestadorId, nombre))

    suspend fun desbloquear(usuarioId: String, prestadorId: String) =
        bloqueadoDao.desbloquear(usuarioId, prestadorId)

    fun contactos(usuarioId: String): Flow<List<ContactoEntity>> = contactoDao.observar(usuarioId)

    suspend fun registrarContacto(contacto: ContactoEntity) = contactoDao.insertar(contacto)

    suspend fun borrarHistorialDeContactos(usuarioId: String) = contactoDao.borrarTodo(usuarioId)

    fun reportes(usuarioId: String): Flow<List<ReporteEntity>> = reporteDao.observar(usuarioId)

    suspend fun reportar(reporte: ReporteEntity) = reporteDao.insertar(reporte)

    fun vistos(usuarioId: String, limite: Int = 3): Flow<List<VistoEntity>> = vistoDao.observar(usuarioId, limite)

    suspend fun registrarVisto(usuarioId: String, servicioId: String) =
        vistoDao.registrar(VistoEntity(usuarioId, servicioId))

    /** Al eliminar la cuenta no queda nada suyo en el teléfono. */
    suspend fun borrarTodo(usuarioId: String) {
        bloqueadoDao.borrarTodo(usuarioId)
        contactoDao.borrarTodo(usuarioId)
        reporteDao.borrarTodo(usuarioId)
        vistoDao.borrarTodo(usuarioId)
    }
}
