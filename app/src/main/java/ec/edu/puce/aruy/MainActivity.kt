package ec.edu.puce.aruy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ec.edu.puce.aruy.data.local.PreferenciasUi
import ec.edu.puce.aruy.ui.navigation.AruyApp
import ec.edu.puce.aruy.ui.theme.AruyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        val contenedor = (application as AruyApplication).contenedor

        setContent {
            val preferencias by contenedor.preferencias.preferencias.collectAsState(initial = PreferenciasUi())

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
