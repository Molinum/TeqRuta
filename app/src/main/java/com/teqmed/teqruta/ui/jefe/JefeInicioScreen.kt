package com.teqmed.teqruta.ui.jefe

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.data.local.entity.JornadaResumen
import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.ui.components.EstadoJornadaChip
import com.teqmed.teqruta.ui.components.TeqRutaTopBar
import com.teqmed.teqruta.ui.tecnico.Dato
import com.teqmed.teqruta.ui.util.formatearFecha
import com.teqmed.teqruta.ui.util.formatearKm
import com.teqmed.teqruta.ui.util.formatearPesos

@Composable
fun JefeInicioScreen(
    viewModel: JefeInicioViewModel,
    subtitulo: String,
    onAbrirJornada: (Long) -> Unit,
    onCerrarSesion: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    Scaffold(
        topBar = { TeqRutaTopBar("Validación de jornadas", subtitulo, onCerrarSesion = onCerrarSesion) }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(FiltroJornadas.entries) { filtro ->
                    val etiqueta =
                        if (filtro == FiltroJornadas.POR_VALIDAR) "${filtro.etiqueta} (${estado.porValidar})"
                        else filtro.etiqueta
                    FilterChip(
                        selected = estado.filtro == filtro,
                        onClick = { viewModel.onFiltroChange(filtro) },
                        label = { Text(etiqueta) }
                    )
                }
            }
            when {
                estado.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                estado.jornadas.isEmpty() -> SinJornadas(estado.filtro)
                else -> LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(estado.jornadas, key = { it.jornada.id }) { resumen ->
                        JornadaItem(resumen, Modifier.animateItem()) { onAbrirJornada(resumen.jornada.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SinJornadas(filtro: FiltroJornadas) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(
            Icons.AutoMirrored.Outlined.FactCheck,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            if (filtro == FiltroJornadas.POR_VALIDAR) "No hay jornadas por validar" else "No hay jornadas en este filtro",
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Text(
            "Las jornadas que cierren los técnicos de tu equipo aparecerán aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun JornadaItem(resumen: JornadaResumen, modifier: Modifier, onClick: () -> Unit) {
    val jornada = resumen.jornada
    val destacada = resumen.tieneAlerta && jornada.estado == EstadoJornada.CERRADA
    val colores =
        // Color opaco: con transparencia la sombra de la tarjeta se ve a través y ensucia el borde
        if (destacada) CardDefaults.elevatedCardColors(
            containerColor = lerp(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.colorScheme.tertiaryContainer, 0.45f)
        )
        else CardDefaults.elevatedCardColors()
    ElevatedCard(modifier = modifier.fillMaxWidth().clickable(onClick = onClick), colors = colores) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
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
            val recorridos = (jornada.kmFinal ?: jornada.kmInicial) - jornada.kmInicial
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${resumen.totalTraslados} traslados · ${formatearKm(recorridos)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(formatearPesos(resumen.costoTotal), style = MaterialTheme.typography.titleSmall)
            }
            if (jornada.validacionAutomatica) {
                Text(
                    "Validada automáticamente",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (resumen.tieneAlerta) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        "${resumen.conAlerta} con desviación o sin catalogar",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
    }
}
