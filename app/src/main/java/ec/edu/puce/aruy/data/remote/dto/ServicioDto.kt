package ec.edu.puce.aruy.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ServicioDto(

    @SerializedName("id")
    val id: String,

    @SerializedName("prestador_id")
    val prestadorId: String,

    @SerializedName("titulo")
    val titulo: String,

    @SerializedName("descripcion")
    val descripcion: String,

    @SerializedName("categoria")
    val categoria: String,

    @SerializedName("precio_referencial")
    val precioReferencial: Double? = null,

    @SerializedName("disponible")
    val disponible: Boolean = true
)