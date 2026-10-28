package com.teqmed.teqruta.ui.tecnico

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.data.local.entity.Traslado
import com.teqmed.teqruta.ui.components.ErrorCard
import com.teqmed.teqruta.ui.components.EstadoChip
import com.teqmed.teqruta.ui.components.FotoOdometroField
import com.teqmed.teqruta.ui.components.TeqRutaTopBar
import com.teqmed.teqruta.ui.components.ValidatedTextField
import com.teqmed.teqruta.ui.util.formatearHora
import com.teqmed.teqruta.ui.util.formatearKm
import com.teqmed.teqruta.ui.util.formatearKmDecimal
import com.teqmed.teqruta.ui.util.formatearPesos
import kotlin.math.roundToInt

@Composable
fun RegistrarLlegadaScreen(
    viewModel: RegistrarLlegadaViewModel,
    onVolver: () -> Unit
) {
    val estado by viewModel.estado.collectAsState()
    val titulo = if (estado.esCierre) "Cerrar jornada" else "Registrar llegada"

    Scaffold(topBar = { TeqRutaTopBar(titulo, onVolver = onVolver) }) { padding ->
        AnimatedContent(
            targetState = estado.resultado,
            transitionSpec = { (fadeIn() + slideInVertically { it / 4 }) togetherWith fadeOut() },
            label = "llegada-resultado",
            modifier = Modifier.fillMaxSize().padding(padding)
        ) { resultado ->
            if (resultado == null) {
                Formulario(estado, viewModel)
            } else {
                Resultado(resultado, estado.esCierre, estado.conUbicacion, onVolver)
            }
        }
    }
}

@Composable
private fun Formulario(estado: RegistrarLlegadaState, viewModel: RegistrarLlegadaViewModel) {
    val enCurso = estado.enCurso
    Column(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (enCurso == null) {
            Text("No hay un traslado en curso.", style = MaterialTheme.typography.bodyLarge)
            return@Column
        }
        Aviso(
            icono = Icons.Outlined.LocalShipping,
            titulo = "${enCurso.origenNombre} → ${enCurso.destinoNombre}",
            texto = "Salida ${formatearHora(enCurso.traslado.horaSalida)} · ${formatearKm(enCurso.traslado.kmInicio)}",
            fondo = MaterialTheme.colorScheme.secondaryContainer,
            contenido = MaterialTheme.colorScheme.onSecondaryContainer
        )

        ValidatedTextField(
            value = estado.km,
            onValueChange = viewModel::onKmChange,
            label = if (estado.esCierre) "Km final del odómetro" else "Km actual del odómetro",
            leadingIcon = Icons.Outlined.Speed,
            error = estado.kmError,
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
            enabled = !estado.guardando
        )

        FotoOdometroField(
            fotoPath = estado.fotoPath,
            onFotoTomada = viewModel::onFotoTomada,
            obligatoria = estado.esCierre,
            error = estado.fotoError
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
                Icon(if (estado.esCierre) Icons.Outlined.Flag else Icons.Outlined.Place, contentDescription = null)
                Text(
                    if (estado.esCierre) "Cerrar jornada" else "Registrar llegada",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun Resultado(traslado: Traslado, esCierre: Boolean, conUbicacion: Boolean, onVolver: () -> Unit) {
    // Contador animado del costo
    val objetivo = remember { mutableFloatStateOf(0f) }
    LaunchedEffect(traslado.id) { objetivo.floatValue = (traslado.costo ?: 0.0).toFloat() }
    val costoAnimado by animateFloatAsState(objetivo.floatValue, tween(900), label = "costo")

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(64.dp)
        )
        Text(
            if (esCierre) "Jornada cerrada" else "Llegada registrada",
            style = MaterialTheme.typography.titleLarge
        )
        Text(formatearPesos(costoAnimado.toDouble()), style = MaterialTheme.typography.headlineMedium)
        traslado.marca?.let { EstadoChip(it) }

        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                val declarados = (traslado.kmFin ?: traslado.kmInicio) - traslado.kmInicio
                Fila("Km declarados", formatearKm(declarados))
                Fila("Km oficiales", traslado.kmOficialUsado?.let(::formatearKmDecimal) ?: "Sin tramo oficial")
                traslado.desviacionPct?.let { Fila("Desviación", "${(it * 100).roundToInt()}%") }
                HorizontalDivider()
                Fila("Peaje", formatearPesos((traslado.peajeUsado ?: 0).toDouble()))
                Fila("Precio litro", formatearPesos(traslado.precioLitroUsado ?: 0.0))
                Fila("Ubicación GPS", if (conUbicacion) "Registrada" else "Sin ubicación")
            }
        }

        Button(onClick = onVolver, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Volver al inicio")
        }
    }
}

@Composable
private fun Fila(etiqueta: String, valor: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(etiqueta, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valor, style = MaterialTheme.typography.bodyMedium)
    }
}
