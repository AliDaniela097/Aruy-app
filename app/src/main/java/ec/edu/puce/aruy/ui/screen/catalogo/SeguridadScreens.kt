package ec.edu.puce.aruy.ui.screen.catalogo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.catalogo.TiempoRelativo
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.TipoPastilla
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.viewmodel.ActividadLocalViewModel

/** Personas bloqueadas (`bloqueados` en `ajustes.js`): no se ven sus servicios ni su perfil. */
@Composable
fun BloqueadosScreen(actividad: ActividadLocalViewModel, onVolver: () -> Unit) {
    val bloqueados by actividad.bloqueados.collectAsState()
    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Personas bloqueadas", onVolver)
        if (bloqueados.isEmpty()) {
            EstadoVacio(
                titulo = "No has bloqueado a nadie",
                texto = "Si alguien te incomoda, puedes bloquearlo desde su perfil.",
                icono = Icons.Filled.Block
            )
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "No ves sus servicios ni su perfil. Puedes desbloquearlas cuando quieras.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(bloqueados, key = { it.prestadorId }) { bloqueado ->
                    AruyTarjeta(tipo = TipoTarjeta.CONTORNO, modifier = Modifier.fillMaxWidth()) {
                        Text(bloqueado.nombre, style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Bloqueada ${TiempoRelativo.texto(bloqueado.fecha)}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        BotonContorno("Desbloquear", { actividad.desbloquear(bloqueado.prestadorId) })
                    }
                }
            }
        }
    }
}

/** Mis reportes. Por ahora solo se guardan en el teléfono: el backend todavía no recibe reportes. */
@Composable
fun MisReportesScreen(actividad: ActividadLocalViewModel, onVolver: () -> Unit, onVerNormas: () -> Unit) {
    val reportes by actividad.reportes.collectAsState()
    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Mis reportes", onVolver)
        if (reportes.isEmpty()) {
            EstadoVacio(
                titulo = "No has hecho reportes",
                texto = "Si ves algo que no cumple las normas, puedes reportarlo desde el perfil o el servicio.",
                icono = Icons.Filled.Flag,
                textoBoton = "Ver las normas",
                onBoton = onVerNormas
            )
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    AruyTarjeta(tipo = TipoTarjeta.ADVERTENCIA, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            "Estos reportes se guardan solo en este teléfono. Todavía no se envían al equipo de Aruy.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                items(reportes, key = { it.id }) { reporte ->
                    AruyTarjeta(tipo = TipoTarjeta.CONTORNO, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(reporte.objetivo, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Pastilla("Guardado", tipo = TipoPastilla.NEUTRA)
                        }
                        Text(reporte.motivo, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(top = 4.dp))
                        reporte.detalle?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                        Text(
                            TiempoRelativo.texto(reporte.fecha),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

private val NORMAS = listOf(
    "Usa fotos propias" to "Sube solo fotos de trabajos que hiciste tú.",
    "Di la verdad" to "Describe tu servicio y tus precios tal como son.",
    "Trato respetuoso" to "Habla con respeto, antes, durante y después del trabajo.",
    "Sin pagos por adelantado sospechosos" to "Desconfía si te piden dinero o datos sensibles antes de conocerse.",
    "Opiniones honestas" to "Cuenta cómo te fue de verdad; no escribas opiniones falsas."
)

@Composable
fun NormasScreen(onVolver: () -> Unit, onVerReportes: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        BarraSuperior("Normas de la comunidad", onVolver)
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Aruy funciona con confianza. Estas son las reglas para cuidarla.",
                style = MaterialTheme.typography.bodyLarge
            )
            NORMAS.forEach { (titulo, texto) ->
                AruyTarjeta(modifier = Modifier.fillMaxWidth()) {
                    Text(titulo, style = MaterialTheme.typography.titleMedium)
                    Text(texto, style = MaterialTheme.typography.bodyMedium)
                }
            }
            BotonContorno("Ver mis reportes", onVerReportes, ancho = true)
        }
    }
}
