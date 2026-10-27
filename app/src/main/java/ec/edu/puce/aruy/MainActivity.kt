package ec.edu.puce.aruy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import ec.edu.puce.aruy.data.local.PreferenciasStore
import ec.edu.puce.aruy.data.local.PreferenciasUi
import ec.edu.puce.aruy.ui.navigation.AruyApp
import ec.edu.puce.aruy.ui.navigation.RolNavegacion
import ec.edu.puce.aruy.ui.theme.AruyTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            val contenedor = remember { AppContainer(applicationContext) }
            val almacen = remember { PreferenciasStore(applicationContext) }
            val preferencias by almacen.preferencias.collectAsState(initial = PreferenciasUi())

            AruyTheme(
                modo = preferencias.modoTema,
                tamanoTexto = preferencias.tamanoTexto,
                reducirAnimaciones = preferencias.reducirAnimaciones
            ) {
                // El rol real llega con el inicio de sesión (fase 2)
                AruyApp(contenedor = contenedor, rol = RolNavegacion.CONTRATANTE)
            }
        }
    }
}
