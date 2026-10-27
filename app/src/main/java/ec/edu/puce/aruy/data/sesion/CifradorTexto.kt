package ec.edu.puce.aruy.data.sesion

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Cifra el token JWT antes de guardarlo en el teléfono. */
interface CifradorTexto {
    fun cifrar(texto: String): String

    /** null si no se puede descifrar (por ejemplo, si cambió la clave del dispositivo). */
    fun descifrar(cifrado: String): String?
}

/** AES-GCM con una clave que vive en el Android Keystore y nunca sale del dispositivo. */
class CifradorKeystore(private val alias: String = "aruy_sesion") : CifradorTexto {

    private fun clave(): SecretKey {
        val almacen = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (almacen.getKey(alias, null) as? SecretKey)?.let { return it }
        val generador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generador.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generador.generateKey()
    }

    override fun cifrar(texto: String): String {
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply { init(Cipher.ENCRYPT_MODE, clave()) }
        val resultado = cifrador.iv + cifrador.doFinal(texto.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(resultado)
    }

    override fun descifrar(cifrado: String): String? = runCatching {
        val bytes = Base64.getDecoder().decode(cifrado)
        val iv = bytes.copyOfRange(0, TAMANO_IV)
        val cifrador = Cipher.getInstance(TRANSFORMACION).apply {
            init(Cipher.DECRYPT_MODE, clave(), GCMParameterSpec(128, iv))
        }
        String(cifrador.doFinal(bytes, TAMANO_IV, bytes.size - TAMANO_IV), Charsets.UTF_8)
    }.getOrNull()

    private companion object {
        const val TRANSFORMACION = "AES/GCM/NoPadding"
        const val TAMANO_IV = 12
    }
}
