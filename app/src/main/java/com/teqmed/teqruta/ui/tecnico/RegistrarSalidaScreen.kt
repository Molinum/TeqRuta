package com.teqmed.teqruta.ui.tecnico

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalHospital
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.ui.components.DropdownField
import com.teqmed.teqruta.ui.components.ErrorCard
import com.teqmed.teqruta.ui.components.TeqRutaTopBar
import com.teqmed.teqruta.ui.util.formatearKmDecimal
import com.teqmed.teqruta.ui.util.formatearPesos

@Composable
fun RegistrarSalidaScreen(
    viewModel: RegistrarSalidaViewModel,
    onVolver: () -> Unit,
    onSalidaRegistrada: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onSalidaRegistrada()
    }

    Scaffold(topBar = { TeqRutaTopBar("Salir hacia…", onVolver = onVolver) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Aviso(
                icono = Icons.Outlined.Place,
                titulo = "Origen",
                texto = estado.origen?.nombre ?: "…",
                fondo = MaterialTheme.colorScheme.secondaryContainer,
                contenido = MaterialTheme.colorScheme.onSecondaryContainer
            )

            DropdownField(
                label = "Destino",
                options = estado.destinos,
                selected = estado.destino,
                onSelected = viewModel::onDestinoSeleccionado,
                optionLabel = { it.nombre },
                optionDetail = { it.ciudad },
                leadingIcon = Icons.Outlined.LocalHospital,
                error = estado.destinoError,
                enabled = !estado.guardando
            )

            val tramo = estado.tramo
            AnimatedVisibility(
                visible = estado.tramoConsultado,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                if (tramo != null) {
                    Aviso(
                        icono = Icons.Outlined.Info,
                        titulo = "Tramo oficial",
                        texto = "${formatearKmDecimal(tramo.kmOficial)} · Peaje ${formatearPesos(tramo.peaje.toDouble())}",
                        fondo = MaterialTheme.colorScheme.primaryContainer,
                        contenido = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else {
                    Aviso(
                        icono = Icons.Outlined.WarningAmber,
                        titulo = "Tramo no catalogado",
                        texto = "No existe un tramo oficial para este trayecto. Puedes continuar; " +
                            "el traslado quedará marcado para revisión del jefe.",
                        fondo = MaterialTheme.colorScheme.tertiaryContainer,
                        contenido = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            ErrorCard(estado.errorGeneral)

            Button(
                onClick = viewModel::onSubmit,
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.LocalShipping, contentDescription = null)
                    Text("Registrar salida", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}

@Composable
internal fun Aviso(icono: ImageVector, titulo: String, texto: String, fondo: Color, contenido: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = fondo, contentColor = contenido)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icono, contentDescription = null)
            Column {
                Text(titulo, style = MaterialTheme.typography.labelMedium)
                Text(texto, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
