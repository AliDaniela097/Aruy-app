package ec.edu.puce.aruy.data.remote.dto

import com.google.gson.annotations.SerializedName

data class UsuarioDto(

    @SerializedName("id")
    val id: String,

    @SerializedName("nombre")
    val nombre: String,

    @SerializedName("telefono")
    val telefono: String,

    @SerializedName("rol")
    val rol: String,

    @SerializedName("estado_verificacion")
    val estadoVerificacion: String,

    @SerializedName("foto_perfil")
    val fotoPerfil: String? = null,

    @SerializedName("activo")
    val activo: Boolean = true
)