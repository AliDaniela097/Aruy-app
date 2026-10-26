package ec.edu.puce.aruy.ui.screen.perfil

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.model.EstadoSuscripcion
import ec.edu.puce.aruy.domain.model.Suscripcion
import ec.edu.puce.aruy.domain.pro.PlanesPro
import ec.edu.puce.aruy.ui.components.AruyInterruptor
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BarraSuperior
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.DialogoConfirmacion
import ec.edu.puce.aruy.ui.components.EstadoCargando
import ec.edu.puce.aruy.ui.components.EstadoVacio
import ec.edu.puce.aruy.ui.components.Pastilla
import ec.edu.puce.aruy.ui.components.TipoPastilla
import ec.edu.puce.aruy.ui.components.TipoTarjeta
import ec.edu.puce.aruy.ui.components.avisar
import ec.edu.puce.aruy.ui.viewmodel.AruyProViewModel

/** Aruy Pro: la suscripción simulada del backend (7 o 30 días, sin cobro real). */
@Composable
fun AruyProScreen(viewModel: AruyProViewModel, onVolver: () -> Unit) {
    val ui by viewModel.ui.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var confirmarCancelar by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.mensajes.collect { snackbar.avisar(it) } }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            BarraSuperior("Aruy Pro", onVolver)
            when {
                ui.cargando -> EstadoCargando("Cargando tu suscripción…")
                ui.error != null -> EstadoVacio(
                    titulo = "No pudimos cargar", texto = ui.error.orEmpty(), icono = Icons.Filled.Star,
                    textoBoton = "Reintentar", onBoton = viewModel::cargar
                )
                else -> Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    AruyTarjeta(tipo = TipoTarjeta.ADVERTENCIA, modifier = Modifier.fillMaxWidth()) {
                        Text("Suscripción simulada", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "En esta versión no se cobra nada: sirve para probar cómo funcionará Aruy Pro.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    ui.suscripcion?.let { Actual(it, ui.procesando, onCancelar = { confirmarCancelar = true }) }

                    if (PlanesPro.puedeContratar(ui.suscripcion)) {
                        Text("Elige un plan", style = MaterialTheme.typography.titleLarge)
                        AruyInterruptor(
                            titulo = "Renovación automática",
                            activado = ui.renovacionAutomatica,
                            onCambio = viewModel::cambiarRenovacion,
                            descripcion = "Se guarda tu preferencia; por ahora no se renueva ni se cobra."
                        )
                        PlanesPro.PLANES.forEach { plan ->
                            AruyTarjeta(tipo = TipoTarjeta.CONTORNO, modifier = Modifier.fillMaxWidth()) {
                                Text(plan.titulo, style = MaterialTheme.typography.titleMedium)
                                Text("${plan.dias} días · ${plan.descripcion}", style = MaterialTheme.typography.bodyMedium)
                                BotonRelleno(
                                    texto = "Elegir ${plan.titulo.removePrefix("Aruy Pro ")}",
                                    onClick = { viewModel.suscribirse(plan.tipo) },
                                    modifier = Modifier.padding(top = 8.dp),
                                    ancho = true,
                                    cargando = ui.procesando
                                )
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    if (confirmarCancelar) {
        DialogoConfirmacion(
            titulo = "¿Cancelar tu suscripción?",
            texto = "Dejará de estar activa y podrás elegir un plan otra vez cuando quieras.",
            textoConfirmar = "Cancelar suscripción",
            textoCancelar = "Mantenerla",
            peligroso = true,
            onConfirmar = { confirmarCancelar = false; viewModel.cancelar() },
            onCancelar = { confirmarCancelar = false }
        )
    }
}

@Composable
private fun Actual(suscripcion: Suscripcion, procesando: Boolean, onCancelar: () -> Unit) {
    val plan = PlanesPro.plan(suscripcion.tipoPlan)
    AruyTarjeta(tipo = TipoTarjeta.CONTORNO, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(plan?.titulo ?: "Aruy Pro", style = MaterialTheme.typography.titleMedium)
            Pastilla(
                PlanesPro.etiquetaEstado(suscripcion.estado),
                tipo = when (suscripcion.estado) {
                    EstadoSuscripcion.ACTIVA -> TipoPastilla.OK
                    EstadoSuscripcion.PENDIENTE -> TipoPastilla.ADVERTENCIA
                    else -> TipoPastilla.NEUTRA
                }
            )
        }
        PlanesPro.fecha(suscripcion.fechaInicio)?.let { Text("Desde el $it", style = MaterialTheme.typography.bodyMedium) }
        PlanesPro.fecha(suscripcion.fechaFin)?.let {
            Text(
                if (suscripcion.estado == EstadoSuscripcion.ACTIVA) "Termina el $it" else "Terminó el $it",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        if (PlanesPro.puedeCancelar(suscripcion)) {
            BotonContorno("Cancelar suscripción", onCancelar, modifier = Modifier.padding(top = 8.dp), habilitado = !procesando)
        }
    }
}
