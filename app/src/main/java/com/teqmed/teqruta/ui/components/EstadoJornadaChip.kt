package com.teqmed.teqruta.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.LockClock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.domain.model.EstadoJornada

/** Etiqueta de color con el estado de una jornada: abierta, cerrada, validada o rechazada. */
@Composable
fun EstadoJornadaChip(estado: EstadoJornada, modifier: Modifier = Modifier) {
    val esquema = MaterialTheme.colorScheme
    val (fondoObjetivo, textoObjetivo) = when (estado) {
        EstadoJornada.ABIERTA -> esquema.primaryContainer to esquema.onPrimaryContainer
        EstadoJornada.CERRADA -> esquema.tertiaryContainer to esquema.onTertiaryContainer
        EstadoJornada.VALIDADA -> esquema.secondaryContainer to esquema.onSecondaryContainer
        EstadoJornada.RECHAZADA -> esquema.errorContainer to esquema.onErrorContainer
    }
    val fondo by animateColorAsState(fondoObjetivo, label = "fondo-chip-jornada")
    val texto by animateColorAsState(textoObjetivo, label = "texto-chip-jornada")
    val icono = when (estado) {
        EstadoJornada.ABIERTA -> Icons.Outlined.LockClock
        EstadoJornada.CERRADA -> Icons.Outlined.HourglassEmpty
        EstadoJornada.VALIDADA -> Icons.Outlined.CheckCircle
        EstadoJornada.RECHAZADA -> Icons.Outlined.Block
    }

    Surface(color = fondo, contentColor = texto, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(estado.etiqueta, style = MaterialTheme.typography.labelMedium)
        }
    }
}
