package ec.edu.puce.aruy.data.remote

import ec.edu.puce.aruy.BuildConfig
import ec.edu.puce.aruy.data.remote.api.UsuarioApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import ec.edu.puce.aruy.data.remote.api.ServicioApi
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Definida en app/build.gradle.kts (buildConfigField BASE_URL)
    private const val BASE_URL = BuildConfig.BASE_URL

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
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
