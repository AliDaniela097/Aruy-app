package ec.edu.puce.aruy

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ec.edu.puce.aruy.data.local.PreferenciasUi
import ec.edu.puce.aruy.ui.navigation.AruyApp
import ec.edu.puce.aruy.ui.theme.AruyTheme
import ec.edu.puce.aruy.ui.theme.ModoTema

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val contenedor = (application as AruyApplication).contenedor

        setContent {
            val preferencias by contenedor.preferencias.preferencias.collectAsState(initial = PreferenciasUi())

            // Los íconos de las barras del sistema deben verse sobre el tema elegido en Ajustes,
            // no solo sobre el del teléfono (si no, "Oscuro" con el sistema en claro queda ilegible)
            val oscuro = when (preferencias.modoTema) {
                ModoTema.SISTEMA -> isSystemInDarkTheme()
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
            }
            LaunchedEffect(oscuro) {
                val estilo = if (oscuro) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = estilo, navigationBarStyle = estilo)
            }

            AruyTheme(
                modo = preferencias.modoTema,
                tamanoTexto = preferencias.tamanoTexto,
                reducirAnimaciones = preferencias.reducirAnimaciones
            ) {
                AruyApp(contenedor = contenedor)
            }
        }
    }
}
