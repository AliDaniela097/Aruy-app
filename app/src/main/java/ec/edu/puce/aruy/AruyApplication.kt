package ec.edu.puce.aruy

import android.app.Application

/** Guarda el [AppContainer] para que sobreviva a los giros de pantalla y a recrear la actividad. */
class AruyApplication : Application() {

    val contenedor: AppContainer by lazy { AppContainer(this) }
}
