package com.teqmed.teqruta.ui.tecnico

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.data.local.entity.TrasladoConNombres
import com.teqmed.teqruta.domain.model.TipoDestino
import com.teqmed.teqruta.ui.components.EstadoChip
import com.teqmed.teqruta.ui.components.PedirPermisoUbicacion
import com.teqmed.teqruta.ui.components.TeqRutaTopBar
import com.teqmed.teqruta.ui.util.formatearHora
import com.teqmed.teqruta.ui.util.formatearKm
import com.teqmed.teqruta.ui.util.formatearPesos

@Composable
fun TecnicoHomeScreen(
    viewModel: TecnicoHomeViewModel,
    subtitulo: String,
    onIniciarJornada: () -> Unit,
    onSalir: () -> Unit,
    onRegistrarLlegada: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    val volviendo by viewModel.volviendo.collectAsState()

    PedirPermisoUbicacion(viewModel.tienePermisoUbicacion())

    Scaffold(
        topBar = { TeqRutaTopBar("Mis traslados", subtitulo, onCerrarSesion = onCerrarSesion) }
    ) { padding ->
        AnimatedContent(
            targetState = estado,
            contentKey = { if (it.cargando) "cargando" else it.jornadaActiva?.jornada?.id ?: "sin-jornada" },
            transitionSpec = { (fadeIn() + scaleIn(initialScale = 0.96f)) togetherWith fadeOut() },
            label = "estado-tecnico",
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { actual ->
            when {
                actual.cargando -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                actual.jornadaActiva == null -> SinJornada(onIniciarJornada)
                else -> JornadaEnCurso(
                    activa = actual.jornadaActiva,
                    volviendo = volviendo,
                    onSalir = onSalir,
                    onVolverASede = viewModel::volverASede,
                    onRegistrarLlegada = onRegistrarLlegada
                )
            }
        }
    }
}

@Composable
private fun SinJornada(onIniciarJornada: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)
    ) {
        Icon(
            Icons.Outlined.EventAvailable,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Text("No tienes una jornada activa", style = MaterialTheme.typography.titleLarge)
        Text(
            "Inicia tu jornada al salir de la sede para registrar tus traslados.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Button(
            onClick = onIniciarJornada,
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 8.dp)
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Text("Iniciar jornada", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun JornadaEnCurso(
    activa: JornadaActiva,
    volviendo: Boolean,
    onSalir: () -> Unit,
    onVolverASede: () -> Unit,
    onRegistrarLlegada: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { ResumenJornada(activa) }
        item { AccionActual(activa, volviendo, onSalir, onVolverASede, onRegistrarLlegada) }
        if (activa.completados.isNotEmpty()) {
            item {
                Text(
                    "Traslados de hoy",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            items(activa.completados.reversed(), key = { it.traslado.id }) { traslado ->
                TrasladoItem(traslado, Modifier.animateItem())
            }
        }
    }
}

@Composable
private fun ResumenJornada(activa: JornadaActiva) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Jornada en curso", style = MaterialTheme.typography.titleMedium)
            Dato(
                Icons.Outlined.DirectionsCar,
                activa.vehiculo?.let { "${it.descripcion} · ${it.patente}" } ?: "Vehículo"
            )
            Dato(Icons.Outlined.Schedule, "Inicio: ${formatearHora(activa.jornada.inicio)}")
            Dato(Icons.Outlined.Speed, "Km inicial: ${formatearKm(activa.jornada.kmInicial)}")
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Total("Traslados", activa.completados.size.toString())
                Total("Km recorridos", formatearKm(activa.kmRecorridos))
                Total("Costo", formatearPesos(activa.costoTotal))
            }
        }
    }
}

@Composable
private fun Total(etiqueta: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, style = MaterialTheme.typography.titleMedium)
        Text(etiqueta, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun AccionActual(
    activa: JornadaActiva,
    volviendo: Boolean,
    onSalir: () -> Unit,
    onVolverASede: () -> Unit,
    onRegistrarLlegada: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val enCurso = activa.enCurso
            if (enCurso != null) {
                val haciaSede = enCurso.destinoTipo == TipoDestino.SEDE
                Dato(Icons.Outlined.LocalShipping, "En camino a ${enCurso.destinoNombre}")
                Text(
                    "Salida ${formatearHora(enCurso.traslado.horaSalida)} · ${formatearKm(enCurso.traslado.kmInicio)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(onClick = onRegistrarLlegada, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(if (haciaSede) Icons.Outlined.Flag else Icons.Outlined.Place, contentDescription = null)
                    Text(
                        if (haciaSede) "Llegué a la sede · Cerrar jornada" else "Registrar llegada",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            } else {
                Dato(Icons.Outlined.Place, "Estás en ${activa.ubicacionActual}")
                Button(onClick = onSalir, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Icon(Icons.Outlined.LocalShipping, contentDescription = null)
                    Text("Salir hacia una clínica", modifier = Modifier.padding(start = 8.dp))
                }
                if (!activa.enSede) {
                    OutlinedButton(
                        onClick = onVolverASede,
                        enabled = !volviendo,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Home, contentDescription = null)
                        Text("Volver a la sede", modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun TrasladoItem(item: TrasladoConNombres, modifier: Modifier = Modifier) {
    val t = item.traslado
    Card(modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${t.orden}. ${item.destinoNombre}",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f)
                )
                t.marca?.let { EstadoChip(it) }
            }
            Text(
                "Desde ${item.origenNombre}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${formatearKm((t.kmFin ?: t.kmInicio) - t.kmInicio)} declarados · ${formatearPesos(t.costo ?: 0.0)}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
internal fun Dato(icono: ImageVector, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icono, contentDescription = null, modifier = Modifier.size(18.dp))
        Text(texto, style = MaterialTheme.typography.bodyMedium)
    }
}
