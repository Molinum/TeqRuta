package com.teqmed.teqruta.ui.gerencia

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.domain.reporte.AgrupadoPor
import com.teqmed.teqruta.domain.reporte.FilaReporte
import com.teqmed.teqruta.domain.reporte.LineaCosto
import com.teqmed.teqruta.ui.components.TeqRutaTopBar
import com.teqmed.teqruta.ui.util.formatearFecha
import com.teqmed.teqruta.ui.util.formatearKm
import com.teqmed.teqruta.ui.util.formatearPesos
import kotlin.math.roundToInt

@Composable
fun GerenciaScreen(
    viewModel: GerenciaViewModel,
    subtitulo: String,
    onCerrarSesion: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    Scaffold(
        topBar = { TeqRutaTopBar("Costos de traslado", subtitulo, onCerrarSesion = onCerrarSesion) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SelectorMes(estado, viewModel::mesAnterior, viewModel::mesSiguiente) }
            item { SelectorAgrupacion(estado.agrupadoPor, viewModel::onAgrupadoChange) }
            item { Totales(estado) }
            when {
                estado.cargando -> item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                estado.filas.isEmpty() -> item { SinDatos() }
                else -> items(estado.filas, key = { it.clave }) { fila ->
                    FilaItem(
                        fila = fila,
                        agrupadoPor = estado.agrupadoPor,
                        abierta = estado.filaAbierta == fila.clave,
                        onClick = { viewModel.onFilaClick(fila.clave) }
                    )
                }
            }
            item {
                Text(
                    "Considera solo jornadas validadas. El regreso a la sede se atribuye al último cliente visitado.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SelectorMes(estado: GerenciaState, onAnterior: () -> Unit, onSiguiente: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAnterior) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, contentDescription = "Mes anterior")
        }
        Text(
            estado.mes.etiqueta.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.titleLarge
        )
        IconButton(onClick = onSiguiente, enabled = estado.puedeAvanzar) {
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = "Mes siguiente")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorAgrupacion(actual: AgrupadoPor, onChange: (AgrupadoPor) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        AgrupadoPor.entries.forEachIndexed { indice, opcion ->
            SegmentedButton(
                selected = actual == opcion,
                onClick = { onChange(opcion) },
                shape = SegmentedButtonDefaults.itemShape(indice, AgrupadoPor.entries.size)
            ) { Text(opcion.etiqueta) }
        }
    }
}

@Composable
private fun Totales(estado: GerenciaState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Costo total del mes", style = MaterialTheme.typography.labelLarge)
            Text(formatearPesos(estado.costoTotal), style = MaterialTheme.typography.headlineMedium)
            Text(
                "${estado.traslados} traslados · ${formatearKm(estado.km)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
            )
        }
    }
}

@Composable
private fun SinDatos() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Outlined.Insights,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp)
        )
        Text("Sin jornadas validadas en este mes", style = MaterialTheme.typography.titleMedium)
        Text(
            "Prueba con otro mes o espera a que el jefe valide las jornadas.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun FilaItem(fila: FilaReporte, agrupadoPor: AgrupadoPor, abierta: Boolean, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.clickable(onClick = onClick).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    fila.nombre,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(formatearPesos(fila.costo), style = MaterialTheme.typography.titleMedium)
                Icon(if (abierta) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null)
            }
            Text(
                "${fila.traslados} ${if (fila.traslados == 1) "traslado" else "traslados"} · ${formatearKm(fila.km)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (agrupadoPor == AgrupadoPor.TECNICO) {
                // RF-GER-06: cantidad y porcentaje de traslados con desviación
                Text(
                    "${fila.conDesviacion} con desviación (${(fila.porcentajeDesviacion * 100).roundToInt()}%)",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (fila.conDesviacion > 0) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            AnimatedVisibility(
                visible = abierta,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    HorizontalDivider()
                    fila.lineas.forEach { LineaItem(it, agrupadoPor) }
                }
            }
        }
    }
}

@Composable
private fun LineaItem(linea: LineaCosto, agrupadoPor: AgrupadoPor) {
    // Se muestra la otra dimensión: el técnico si se agrupa por cliente, el cliente en los demás casos
    val quien = if (agrupadoPor == AgrupadoPor.CLIENTE) linea.tecnicoNombre else linea.clienteNombre
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Column(Modifier.weight(1f).padding(end = 8.dp)) {
            Text(quien, style = MaterialTheme.typography.bodyMedium)
            Text(
                "${formatearFecha(linea.fecha)} · ${formatearKm(linea.kmDeclarados)}" +
                    if (linea.conDesviacion) " · desviación" else "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(formatearPesos(linea.costo), style = MaterialTheme.typography.bodyMedium)
    }
}
