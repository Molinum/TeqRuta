package com.teqmed.teqruta.ui.jornada

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.data.local.entity.JornadaResumen
import com.teqmed.teqruta.data.local.entity.TrasladoConNombres
import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.ui.components.ConfirmDialog
import com.teqmed.teqruta.ui.components.ErrorCard
import com.teqmed.teqruta.ui.components.EstadoChip
import com.teqmed.teqruta.ui.components.EstadoJornadaChip
import com.teqmed.teqruta.ui.components.FormDialog
import com.teqmed.teqruta.ui.components.FotoMiniatura
import com.teqmed.teqruta.ui.components.TeqRutaTopBar
import com.teqmed.teqruta.ui.components.ValidatedTextField
import com.teqmed.teqruta.ui.tecnico.Dato
import com.teqmed.teqruta.ui.util.formatearFecha
import com.teqmed.teqruta.ui.util.formatearHora
import com.teqmed.teqruta.ui.util.formatearKm
import com.teqmed.teqruta.ui.util.formatearKmDecimal
import com.teqmed.teqruta.ui.util.formatearMotivo
import com.teqmed.teqruta.ui.util.formatearPesos
import kotlin.math.roundToInt

@Composable
fun JornadaDetalleScreen(
    viewModel: JornadaDetalleViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(estado.revisada) {
        if (estado.revisada) onVolver()
    }
    LaunchedEffect(estado.noAutorizado) {
        if (estado.noAutorizado) onVolver()
    }

    Scaffold(
        topBar = { TeqRutaTopBar("Detalle de jornada", onVolver = onVolver) },
        bottomBar = {
            if (estado.puedeRevisar) {
                BarraRevision(
                    procesando = estado.procesando,
                    onRechazar = viewModel::pedirRechazo,
                    onValidar = viewModel::pedirValidar
                )
            }
        }
    ) { padding ->
        val resumen = estado.resumen
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            resumen == null || estado.noAutorizado -> Box(
                Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center
            ) { Text("No se encontró la jornada.") }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { ErrorCard(estado.errorGeneral) }
                item { Cabecera(resumen, estado.esJefe) }
                item { Fotos(resumen) }
                item {
                    Text(
                        "Traslados (${estado.traslados.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                items(estado.traslados, key = { it.traslado.id }) { TrasladoDetalle(it, estado.esJefe) }
            }
        }
    }

    if (estado.confirmandoValidar) {
        ConfirmDialog(
            titulo = "Validar jornada",
            mensaje = "Una vez validada, la jornada no se puede modificar y pasa a los reportes de costo.",
            textoConfirmar = "Validar",
            icono = Icons.Outlined.CheckCircle,
            onConfirmar = viewModel::validar,
            onCancelar = viewModel::cancelarValidar
        )
    }
    if (estado.rechazando) {
        FormDialog(
            titulo = "Rechazar jornada",
            onGuardar = viewModel::rechazar,
            onCancelar = viewModel::cancelarRechazo,
            guardando = estado.procesando,
            textoGuardar = "Rechazar"
        ) {
            Text(
                "El técnico verá este comentario en su historial.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            ValidatedTextField(
                value = estado.comentario,
                onValueChange = viewModel::onComentarioChange,
                label = "Motivo del rechazo",
                leadingIcon = Icons.Outlined.ChatBubbleOutline,
                error = estado.comentarioError,
                helper = "Mínimo 10 caracteres",
                onImeAction = viewModel::rechazar,
                enabled = !estado.procesando
            )
        }
    }
}

@Composable
private fun BarraRevision(procesando: Boolean, onRechazar: () -> Unit, onValidar: () -> Unit) {
    Surface(tonalElevation = 3.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRechazar,
                enabled = !procesando,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                Icon(Icons.Outlined.Block, contentDescription = null)
                Text("Rechazar", modifier = Modifier.padding(start = 8.dp))
            }
            Button(
                onClick = onValidar,
                enabled = !procesando,
                modifier = Modifier.weight(1f).height(52.dp)
            ) {
                if (procesando) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.CheckCircle, contentDescription = null)
                    Text("Validar", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
private fun Cabecera(resumen: JornadaResumen, esJefe: Boolean) {
    val jornada = resumen.jornada
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatearFecha(jornada.inicio), style = MaterialTheme.typography.titleMedium)
                EstadoJornadaChip(jornada.estado)
            }
            Dato(Icons.Outlined.Person, resumen.tecnicoNombre)
            Dato(Icons.Outlined.DirectionsCar, "${resumen.vehiculoDescripcion} · ${resumen.vehiculoPatente}")
            Dato(
                Icons.Outlined.Schedule,
                "${formatearHora(jornada.inicio)} – ${jornada.fin?.let(::formatearHora) ?: "…"}"
            )
            val kmFinal = jornada.kmFinal
            Dato(
                Icons.Outlined.Speed,
                if (kmFinal != null) {
                    "${formatearKm(jornada.kmInicial)} → ${formatearKm(kmFinal)} (${formatearKm(kmFinal - jornada.kmInicial)})"
                } else {
                    "Km inicial ${formatearKm(jornada.kmInicial)}"
                }
            )
            if (jornada.validacionAutomatica) {
                Text(
                    "Validada automáticamente: ningún traslado tuvo desviación.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (esJefe) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Costo total de la jornada", style = MaterialTheme.typography.bodyMedium)
                    Text(formatearPesos(resumen.costoTotal), style = MaterialTheme.typography.titleMedium)
                }
                if (resumen.tieneAlerta) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Outlined.WarningAmber, contentDescription = null)
                            Text(
                                "${resumen.conAlerta} de ${resumen.totalTraslados} traslados con desviación o sin catalogar.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
            if (jornada.estado == EstadoJornada.RECHAZADA && jornada.comentarioJefe != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text("Comentario del jefe", style = MaterialTheme.typography.labelMedium)
                        Text(jornada.comentarioJefe, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun Fotos(resumen: JornadaResumen) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        FotoMiniatura("Odómetro al iniciar", resumen.jornada.fotoInicioUri, Modifier.weight(1f))
        FotoMiniatura("Odómetro al cerrar", resumen.jornada.fotoCierreUri, Modifier.weight(1f))
    }
}

@Composable
private fun TrasladoDetalle(item: TrasladoConNombres, esJefe: Boolean) {
    val t = item.traslado
    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${t.orden}. ${item.origenNombre} → ${item.destinoNombre}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                t.marca?.let { EstadoChip(it) }
            }
            Text(
                "${formatearHora(t.horaSalida)} – ${t.horaLlegada?.let(::formatearHora) ?: "…"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val recorridos = (t.kmFin ?: t.kmInicio) - t.kmInicio
            Linea("Km declarados", formatearKm(recorridos))
            if (esJefe) {
                Linea("Km oficiales", t.kmOficialUsado?.let(::formatearKmDecimal) ?: "Sin tramo oficial")
                t.desviacionPct?.let { Linea("Desviación", "${(it * 100).roundToInt()}%") }
                Linea("Costo", formatearPesos(t.costo ?: 0.0))
            }
            formatearMotivo(t.motivoTipo, t.motivoDetalle)?.let { Linea("Motivo del técnico", it) }
            Linea("GPS de salida", coordenadas(t.latSalida, t.lonSalida))
            Linea("GPS de llegada", coordenadas(t.latLlegada, t.lonLlegada))
            if (t.fotoUri != null) {
                FotoMiniatura("Odómetro en la llegada", t.fotoUri, Modifier.padding(top = 8.dp))
            }
        }
    }
}

private fun coordenadas(lat: Double?, lon: Double?): String =
    if (lat == null || lon == null) "Sin ubicación" else "%.4f, %.4f".format(lat, lon)

@Composable
private fun Linea(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}
