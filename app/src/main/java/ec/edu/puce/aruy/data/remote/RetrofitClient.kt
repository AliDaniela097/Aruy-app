package ec.edu.puce.aruy.data.remote

import ec.edu.puce.aruy.data.remote.api.UsuarioApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ec.edu.puce.aruy.data.remote.api.ServicioApi

object RetrofitClient {

    // URL temporal: reemplazar cuando exista el backend
    private const val BASE_URL = "https://example.com/"

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val usuarioApi: UsuarioApi =
        retrofit.create(UsuarioApi::class.java)

    val servicioApi: ServicioApi =
        retrofit.create(ServicioApi::class.java)
}