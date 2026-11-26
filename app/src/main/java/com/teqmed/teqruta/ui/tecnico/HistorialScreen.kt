package com.teqmed.teqruta.ui.tecnico

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
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventNote
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.data.local.entity.JornadaResumen
import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.ui.components.EstadoJornadaChip
import com.teqmed.teqruta.ui.components.TeqRutaTopBar
import com.teqmed.teqruta.ui.util.formatearFecha
import com.teqmed.teqruta.ui.util.formatearKm

@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel,
    onVolver: () -> Unit,
    onAbrirJornada: (Long) -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    Scaffold(topBar = { TeqRutaTopBar("Historial de jornadas", onVolver = onVolver) }) { padding ->
        when {
            estado.cargando -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            estado.jornadas.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
            ) {
                Icon(
                    Icons.Outlined.EventNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
                Text("Aún no tienes jornadas cerradas", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Cuando cierres una jornada aparecerá aquí con su estado de revisión.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(estado.jornadas, key = { it.jornada.id }) { JornadaItem(it) { onAbrirJornada(it.jornada.id) } }
            }
        }
    }
}

@Composable
private fun JornadaItem(resumen: JornadaResumen, onClick: () -> Unit) {
    val jornada = resumen.jornada
    ElevatedCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatearFecha(jornada.inicio), style = MaterialTheme.typography.titleMedium)
                EstadoJornadaChip(jornada.estado)
            }
            Dato(Icons.Outlined.DirectionsCar, "${resumen.vehiculoDescripcion} · ${resumen.vehiculoPatente}")
            val recorridos = (jornada.kmFinal ?: jornada.kmInicial) - jornada.kmInicial
            Text(
                "${resumen.totalTraslados} traslados · ${formatearKm(recorridos)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (jornada.estado == EstadoJornada.RECHAZADA && jornada.comentarioJefe != null) {
                Text(
                    "Comentario del jefe: ${jornada.comentarioJefe}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
