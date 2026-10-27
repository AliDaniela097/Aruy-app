package ec.edu.puce.aruy.ui.screen.catalogo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ec.edu.puce.aruy.domain.catalogo.Contacto
import ec.edu.puce.aruy.domain.catalogo.MotivoReporte
import ec.edu.puce.aruy.ui.components.AruyAvatar
import ec.edu.puce.aruy.ui.components.AruyCampo
import ec.edu.puce.aruy.ui.components.AruyHoja
import ec.edu.puce.aruy.ui.components.AruyTarjeta
import ec.edu.puce.aruy.ui.components.BotonContorno
import ec.edu.puce.aruy.ui.components.BotonRelleno
import ec.edu.puce.aruy.ui.components.BotonTexto
import ec.edu.puce.aruy.ui.components.BotonWhatsApp
import ec.edu.puce.aruy.ui.components.TipoTarjeta

/** "Contactar a {nombre}": mensaje editable y envío por WhatsApp (`flows.js`). */
@Composable
fun HojaContactar(
    nombre: String,
    oficio: String?,
    tituloServicio: String?,
    disponible: Boolean,
    onAbrirWhatsApp: (mensaje: String) -> Unit,
    onCopiar: (mensaje: String) -> Unit,
    onCerrar: () -> Unit
) {
    var mensaje by rememberSaveable { mutableStateOf(Contacto.mensajePorDefecto(nombre, tituloServicio)) }
    AruyHoja("Contactar a $nombre", onCerrar) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AruyAvatar(nombre)
            if (!oficio.isNullOrBlank()) Text(oficio, style = MaterialTheme.typography.bodyLarge)
        }
        if (!disponible) {
            AruyTarjeta(tipo = TipoTarjeta.ADVERTENCIA, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "$nombre no está recibiendo solicitudes ahora. Puedes dejarle tu mensaje y te responderá cuando vuelva.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        AruyCampo(
            valor = mensaje,
            onValor = { mensaje = it },
            etiqueta = "Tu mensaje",
            unaLinea = false,
            lineasMin = 4,
            maxCaracteres = 400
        )
        BotonWhatsApp("Abrir WhatsApp", { onAbrirWhatsApp(mensaje.trim()) }, icono = Icons.AutoMirrored.Filled.Chat, habilitado = mensaje.isNotBlank())
        BotonContorno("Copiar mensaje", { onCopiar(mensaje.trim()) }, icono = Icons.Filled.ContentCopy, ancho = true, habilitado = mensaje.isNotBlank())
        BotonTexto("Ahora no", onCerrar)
    }
}

/** "¿Cómo te fue con {nombre}?": 1 a 5 estrellas y comentario opcional. */
@Composable
fun HojaCalificar(
    nombre: String,
    enviando: Boolean,
    onPublicar: (puntuacion: Int, comentario: String?) -> Unit,
    onCerrar: () -> Unit
) {
    var estrellas by rememberSaveable { mutableStateOf(0) }
    var comentario by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }

    AruyHoja("¿Cómo te fue con $nombre?", onCerrar) {
        Text(
            "Tu opinión ayuda a otras personas a elegir. Solo puedes calificar una vez a cada prestador.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            (1..5).forEach { valor ->
                IconButton(
                    onClick = { estrellas = valor; error = null },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics {
                            role = Role.RadioButton
                            contentDescription = "$valor ${if (valor == 1) "estrella" else "estrellas"}"
                        }
                ) {
                    Icon(
                        if (valor <= estrellas) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = ec.edu.puce.aruy.ui.theme.AruyTheme.colores.estrella,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
        AruyCampo(
            valor = comentario,
            onValor = { comentario = it },
            etiqueta = "Comentario (opcional)",
            placeholder = "Cuenta cómo fue el trabajo: puntualidad, trato, resultado…",
            unaLinea = false,
            lineasMin = 3,
            maxCaracteres = 300
        )
        BotonRelleno(
            texto = if (enviando) "Publicando…" else "Publicar opinión",
            onClick = {
                if (estrellas == 0) error = "Elige cuántas estrellas le das."
                else onPublicar(estrellas, comentario.trim().ifEmpty { null })
            },
            grande = true,
            ancho = true,
            cargando = enviando
        )
    }
}

/** "Reportar {objetivo}": motivo y detalle. Por ahora el reporte se guarda solo en el teléfono. */
@Composable
fun HojaReportar(
    objetivo: String,
    onEnviar: (motivo: MotivoReporte, detalle: String?) -> Unit,
    onCerrar: () -> Unit
) {
    var motivo by rememberSaveable { mutableStateOf(MotivoReporte.FOTOS_AJENAS) }
    var detalle by rememberSaveable { mutableStateOf("") }

    AruyHoja("Reportar $objetivo", onCerrar) {
        Text(
            "Se guarda en este teléfono. Todavía no se envía al equipo de Aruy.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        MotivoReporte.entries.forEach { opcion ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = motivo == opcion, role = Role.RadioButton) { motivo = opcion }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(selected = motivo == opcion, onClick = null)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(opcion.titulo, style = MaterialTheme.typography.titleMedium)
                    opcion.descripcion?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        AruyCampo(
            valor = detalle,
            onValor = { detalle = it },
            etiqueta = "Cuéntanos más (opcional)",
            unaLinea = false,
            lineasMin = 3,
            maxCaracteres = 240
        )
        BotonRelleno("Guardar reporte", { onEnviar(motivo, detalle.trim().ifEmpty { null }) }, grande = true, ancho = true)
    }
}

/** "Más opciones" del detalle: reportar o bloquear. */
@Composable
fun HojaMasOpciones(
    nombre: String,
    onReportar: () -> Unit,
    onBloquear: () -> Unit,
    onCerrar: () -> Unit,
    objetivo: String = "servicio"
) {
    AruyHoja(null, onCerrar) {
        FilaOpcion(Icons.Filled.Flag, "Reportar este $objetivo", "Fotos falsas, estafa o contenido inapropiado", onReportar)
        FilaOpcion(Icons.Filled.Block, "Bloquear a $nombre", "Dejarás de ver sus servicios", onBloquear)
        BotonTexto("Cancelar", onCerrar)
    }
}

@Composable
private fun FilaOpcion(icono: androidx.compose.ui.graphics.vector.ImageVector, titulo: String, texto: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(selected = false, role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(icono, contentDescription = null)
        Column {
            Text(titulo, style = MaterialTheme.typography.titleMedium)
            Text(texto, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
