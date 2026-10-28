package com.teqmed.teqruta.ui.tecnico

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.teqmed.teqruta.ui.components.TeqRutaTopBar
import com.teqmed.teqruta.ui.util.formatearHora
import com.teqmed.teqruta.ui.util.formatearKm

@Composable
fun TecnicoHomeScreen(
    viewModel: TecnicoHomeViewModel,
    subtitulo: String,
    onIniciarJornada: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

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
                else -> JornadaEnCurso(actual.jornadaActiva)
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
private fun JornadaEnCurso(activa: JornadaActiva) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Jornada en curso", style = MaterialTheme.typography.titleMedium)
                Dato(
                    Icons.Outlined.DirectionsCar,
                    activa.vehiculo?.let { "${it.descripcion} · ${it.patente}" } ?: "Vehículo"
                )
                Dato(Icons.Outlined.Schedule, "Inicio: ${formatearHora(activa.jornada.inicio)}")
                Dato(Icons.Outlined.Speed, "Km inicial: ${formatearKm(activa.jornada.kmInicial)}")
            }
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
