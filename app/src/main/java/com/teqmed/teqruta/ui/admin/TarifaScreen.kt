package com.teqmed.teqruta.ui.admin

import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.teqmed.teqruta.ui.components.FormDialog
import com.teqmed.teqruta.ui.components.ValidatedTextField
import com.teqmed.teqruta.ui.util.formatearFecha
import com.teqmed.teqruta.ui.util.formatearPesos

@Composable
fun TarifaScreen(viewModel: TarifaViewModel, onVolver: () -> Unit) {
    val estado by viewModel.estado.collectAsState()
    val ahora = System.currentTimeMillis()

    MantenedorScaffold("Tarifa de combustible", onVolver, viewModel::nuevo, mensaje = null) {
        items(estado.tarifas, key = { it.id }) { t ->
            val situacion = when {
                t.id == estado.vigenteId -> "Vigente"
                t.vigenteDesde > ahora -> "Programada"
                else -> "Anterior"
            }
            ItemMantenedor(
                titulo = "${formatearPesos(t.precioLitro)} por litro",
                detalle = "$situacion · desde ${formatearFecha(t.vigenteDesde)}",
                onEditar = null,
                modifier = Modifier.animateItem()
            )
        }
    }

    estado.form?.let { f ->
        FormDialog(
            titulo = "Nueva tarifa",
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarForm,
            guardando = f.guardando,
            errorGeneral = f.errorGeneral
        ) {
            ValidatedTextField(f.precio, viewModel::onPrecioChange, "Precio por litro (CLP)", Icons.Outlined.LocalGasStation,
                error = f.errores[CampoTarifa.PRECIO], keyboardType = KeyboardType.Decimal, enabled = !f.guardando)
            ValidatedTextField(f.vigenteDesde, viewModel::onVigenciaChange, "Vigente desde", Icons.Outlined.CalendarMonth,
                error = f.errores[CampoTarifa.VIGENCIA], helper = "dd-MM-aaaa",
                imeAction = ImeAction.Done, onImeAction = viewModel::guardar, enabled = !f.guardando)
        }
    }
}
