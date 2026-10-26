package ec.edu.puce.aruy.data.sesion

import com.google.gson.JsonParser
import java.util.Base64

/** Lee la fecha de vencimiento (`exp`) del token JWT, sin validar la firma (eso lo hace el servidor). */
object Jwt {

    /** Segundos desde 1970 en que vence el token, o null si no se puede leer. */
    fun vencimientoSegundos(token: String): Long? = runCatching {
        val carga = token.split(".")[1]
        val json = String(Base64.getUrlDecoder().decode(carga), Charsets.UTF_8)
        JsonParser.parseString(json).asJsonObject.get("exp").asLong
    }.getOrNull()

    fun estaVencido(token: String, ahoraMs: Long = System.currentTimeMillis()): Boolean {
        val exp = vencimientoSegundos(token) ?: return true
        return exp * 1000 <= ahoraMs
    }
}
