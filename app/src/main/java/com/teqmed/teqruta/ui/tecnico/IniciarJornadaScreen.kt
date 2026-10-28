package com.teqmed.teqruta.ui.tecnico

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.ui.components.DropdownField
import com.teqmed.teqruta.ui.components.ErrorCard
import com.teqmed.teqruta.ui.components.TeqRutaTopBar
import com.teqmed.teqruta.ui.components.ValidatedTextField
import com.teqmed.teqruta.ui.util.formatearKm

@Composable
fun IniciarJornadaScreen(
    viewModel: IniciarJornadaViewModel,
    onVolver: () -> Unit,
    onJornadaIniciada: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onJornadaIniciada()
    }

    Scaffold(
        topBar = { TeqRutaTopBar("Iniciar jornada", onVolver = onVolver) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Outlined.Home, contentDescription = null)
                    Column {
                        Text("Origen", style = MaterialTheme.typography.labelMedium)
                        Text(estado.sedeNombre, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            DropdownField(
                label = "Vehículo",
                options = estado.vehiculos,
                selected = estado.vehiculo,
                onSelected = viewModel::onVehiculoSeleccionado,
                optionLabel = { it.descripcion },
                optionDetail = { "${it.patente} · ${it.tipo.etiqueta}" },
                leadingIcon = Icons.Outlined.DirectionsCar,
                error = estado.vehiculoError,
                enabled = !estado.guardando
            )

            ValidatedTextField(
                value = estado.kmInicial,
                onValueChange = viewModel::onKmInicialChange,
                label = "Km inicial del odómetro",
                leadingIcon = Icons.Outlined.Speed,
                error = estado.kmError,
                helper = when {
                    estado.vehiculo == null -> null
                    estado.ultimoKm != null -> "Último km registrado: ${formatearKm(estado.ultimoKm!!)}"
                    else -> "Sin registros previos para este vehículo"
                },
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
                onImeAction = viewModel::onSubmit,
                enabled = !estado.guardando
            )

            ErrorCard(estado.errorGeneral)

            Button(
                onClick = viewModel::onSubmit,
                enabled = !estado.guardando,
                modifier = Modifier.fillMaxWidth().height(52.dp)
            ) {
                if (estado.guardando) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                    Text("Iniciar jornada", modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
    }
}
