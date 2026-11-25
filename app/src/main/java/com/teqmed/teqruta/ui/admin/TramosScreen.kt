package com.teqmed.teqruta.ui.admin

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.ui.components.DropdownField
import com.teqmed.teqruta.ui.components.FormDialog
import com.teqmed.teqruta.ui.components.ValidatedTextField
import com.teqmed.teqruta.ui.util.formatearFecha
import com.teqmed.teqruta.ui.util.formatearKmDecimal
import com.teqmed.teqruta.ui.util.formatearPesos

@Composable
fun TramosScreen(viewModel: TramosViewModel, onVolver: () -> Unit) {
    val estado by viewModel.estado.collectAsState()

    MantenedorScaffold("Tramos oficiales", onVolver, viewModel::nuevo, mensaje = null) {
        items(estado.tramos, key = { it.tramo.id }) { t ->
            ItemMantenedor(
                titulo = "${t.origenNombre} → ${t.destinoNombre}",
                detalle = "${formatearKmDecimal(t.tramo.kmOficial)} · Peaje ${formatearPesos(t.tramo.peaje.toDouble())} · " +
                    "desde ${formatearFecha(t.tramo.vigenteDesde)}",
                onEditar = { viewModel.editar(t) },
                modifier = Modifier.animateItem()
            )
        }
    }

    estado.form?.let { f ->
        val edita = f.anterior != null
        FormDialog(
            titulo = if (edita) "Nueva versión del tramo" else "Nuevo tramo",
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarForm,
            guardando = f.guardando,
            errorGeneral = f.errorGeneral
        ) {
            DropdownField(
                label = "Origen", options = estado.destinos, selected = f.origen,
                onSelected = viewModel::onOrigenChange, optionLabel = { it.nombre }, optionDetail = { it.ciudad },
                leadingIcon = Icons.Outlined.Place, error = f.errores[CampoTramo.ORIGEN],
                enabled = !edita && !f.guardando
            )
            DropdownField(
                label = "Destino", options = estado.destinos, selected = f.destino,
                onSelected = viewModel::onDestinoChange, optionLabel = { it.nombre }, optionDetail = { it.ciudad },
                leadingIcon = Icons.Outlined.Flag, error = f.errores[CampoTramo.DESTINO],
                enabled = !edita && !f.guardando
            )
            ValidatedTextField(f.km, viewModel::onKmChange, "Km oficiales", Icons.Outlined.Route,
                error = f.errores[CampoTramo.KM], keyboardType = KeyboardType.Decimal, enabled = !f.guardando)
            ValidatedTextField(f.peaje, viewModel::onPeajeChange, "Peaje (CLP)", Icons.Outlined.Payments,
                error = f.errores[CampoTramo.PEAJE], keyboardType = KeyboardType.Number, enabled = !f.guardando)
            ValidatedTextField(f.vigenteDesde, viewModel::onVigenciaChange, "Vigente desde", Icons.Outlined.CalendarMonth,
                error = f.errores[CampoTramo.VIGENCIA], helper = "dd-MM-aaaa",
                imeAction = ImeAction.Done, onImeAction = viewModel::guardar, enabled = !f.guardando)
            if (f.historial.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Versiones", style = MaterialTheme.typography.labelLarge)
                    f.historial.forEach { v ->
                        val hasta = v.tramo.vigenteHasta?.let { "hasta ${formatearFecha(it)}" } ?: "vigente"
                        Text(
                            "${formatearFecha(v.tramo.vigenteDesde)} ($hasta): " +
                                "${formatearKmDecimal(v.tramo.kmOficial)}, peaje ${formatearPesos(v.tramo.peaje.toDouble())}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
