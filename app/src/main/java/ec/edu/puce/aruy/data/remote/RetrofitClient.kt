package ec.edu.puce.aruy.data.remote

import ec.edu.puce.aruy.BuildConfig
import ec.edu.puce.aruy.data.remote.api.AuthApi
import ec.edu.puce.aruy.data.remote.api.ServicioApi
import ec.edu.puce.aruy.data.remote.api.UsuarioApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP de la app. Recibe de dónde sacar el token JWT y qué hacer si el servidor lo rechaza,
 * para no depender de la sesión directamente.
 */
class RetrofitClient(
    token: () -> String?,
    alExpirar: () -> Unit,
    // Definida en app/build.gradle.kts (buildConfigField BASE_URL, según el flavor emulador/celular)
    baseUrl: String = BuildConfig.BASE_URL
) {

    private val loggingInterceptor =
        HttpLoggingInterceptor().apply {
            // BASIC no imprime cabeceras: el token JWT nunca llega al log
            level = HttpLoggingInterceptor.Level.BASIC
        }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(token, alExpirar))
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)

    val usuarioApi: UsuarioApi = retrofit.create(UsuarioApi::class.java)

    val servicioApi: ServicioApi = retrofit.create(ServicioApi::class.java)
}
