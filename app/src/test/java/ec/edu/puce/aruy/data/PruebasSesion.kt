package ec.edu.puce.aruy.data

import ec.edu.puce.aruy.data.sesion.AlmacenSesion
import ec.edu.puce.aruy.domain.model.Sesion
import java.util.Base64

/** Almacén en memoria para probar sin Android. */
class AlmacenEnMemoria(var guardada: Sesion? = null) : AlmacenSesion {
    override suspend fun leer(): Sesion? = guardada
    override suspend fun guardar(sesion: Sesion) { guardada = sesion }
    override suspend fun borrar() { guardada = null }
}

/** Token con la forma de un JWT (la firma no importa: la app no la valida). */
fun tokenDePrueba(vencimientoSegundos: Long): String {
    val codificador = Base64.getUrlEncoder().withoutPadding()
    val cabecera = codificador.encodeToString("""{"alg":"HS256","typ":"JWT"}""".toByteArray())
    val carga = codificador.encodeToString(
        """{"rol":"PRESTADOR","sub":"u1","iat":1,"exp":$vencimientoSegundos}""".toByteArray()
    )
    return "$cabecera.$carga.firma"
}
