package com.teqmed.teqruta.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.domain.model.MarcaTraslado

/** Etiqueta de color según la marca del traslado: OK, desviación o no catalogado. */
@Composable
fun EstadoChip(marca: MarcaTraslado, modifier: Modifier = Modifier) {
    val esquema = MaterialTheme.colorScheme
    val (fondoObjetivo, textoObjetivo) = when (marca) {
        MarcaTraslado.OK -> esquema.secondaryContainer to esquema.onSecondaryContainer
        MarcaTraslado.DESVIACION -> esquema.errorContainer to esquema.onErrorContainer
        MarcaTraslado.NO_CATALOGADO -> esquema.tertiaryContainer to esquema.onTertiaryContainer
    }
    val fondo by animateColorAsState(fondoObjetivo, label = "fondo-chip")
    val texto by animateColorAsState(textoObjetivo, label = "texto-chip")
    val icono = when (marca) {
        MarcaTraslado.OK -> Icons.Outlined.CheckCircle
        MarcaTraslado.DESVIACION -> Icons.Outlined.WarningAmber
        MarcaTraslado.NO_CATALOGADO -> Icons.AutoMirrored.Outlined.HelpOutline
    }

    Surface(color = fondo, contentColor = texto, shape = RoundedCornerShape(50), modifier = modifier) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icono, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(marca.etiqueta, style = MaterialTheme.typography.labelMedium)
        }
    }
}
