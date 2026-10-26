package ec.edu.puce.aruy.ui.screen.sistema

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.navigation.RolNavegacion

/** Pantalla a la que lleva una ruta que el rol actual no puede abrir (`sistema.js` del prototipo). */
@Composable
fun AccesoDenegadoScreen(rol: RolNavegacion, onVolverAlInicio: () -> Unit) {
    val texto = when (rol) {
        RolNavegacion.PRESTADOR -> "Eres prestador: esta pantalla es para quienes contratan servicios."
        RolNavegacion.CONTRATANTE -> "Eres contratante: esta pantalla es para quienes ofrecen servicios."
        RolNavegacion.INVITADO -> "Crea una cuenta o inicia sesión para entrar aquí."
    }
    EstadoVacio(
        titulo = "Esta pantalla es de otro tipo de cuenta",
        texto = texto,
        icono = Icons.Filled.Lock,
        textoBoton = "Volver a mi inicio",
        onBoton = onVolverAlInicio
    )
}
