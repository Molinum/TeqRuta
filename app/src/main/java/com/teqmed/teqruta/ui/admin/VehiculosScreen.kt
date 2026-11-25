package com.teqmed.teqruta.ui.admin

import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.teqmed.teqruta.domain.model.TipoVehiculo
import com.teqmed.teqruta.ui.components.ConfirmDialog
import com.teqmed.teqruta.ui.components.DropdownField
import com.teqmed.teqruta.ui.components.FormDialog
import com.teqmed.teqruta.ui.components.ValidatedTextField

@Composable
fun VehiculosScreen(viewModel: VehiculosViewModel, onVolver: () -> Unit) {
    val estado by viewModel.estado.collectAsState()

    MantenedorScaffold("Vehículos", onVolver, viewModel::nuevo, estado.mensaje) {
        items(estado.vehiculos, key = { it.id }) { v ->
            ItemMantenedor(
                titulo = "${v.descripcion} · ${v.patente}",
                detalle = "${v.tipo.etiqueta} · ${"%.1f".format(v.rendimientoKmL)} km/L",
                activo = v.activo,
                onEditar = { viewModel.editar(v) },
                onCambiarActivo = { viewModel.pedirCambioActivo(v) },
                modifier = Modifier.animateItem()
            )
        }
    }

    estado.form?.let { f ->
        FormDialog(
            titulo = if (f.id == 0L) "Nuevo vehículo" else "Editar vehículo",
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarForm,
            guardando = f.guardando,
            errorGeneral = f.errorGeneral
        ) {
            ValidatedTextField(f.patente, viewModel::onPatenteChange, "Patente", Icons.Outlined.Badge,
                error = f.errores[CampoVehiculo.PATENTE], helper = "AB1234 o ABCD12", enabled = !f.guardando)
            ValidatedTextField(f.descripcion, viewModel::onDescripcionChange, "Descripción", Icons.Outlined.DirectionsCar,
                error = f.errores[CampoVehiculo.DESCRIPCION], enabled = !f.guardando)
            DropdownField(
                label = "Tipo", options = TipoVehiculo.entries, selected = f.tipo,
                onSelected = viewModel::onTipoChange, optionLabel = { it.etiqueta },
                leadingIcon = Icons.Outlined.Category, error = f.errores[CampoVehiculo.TIPO], enabled = !f.guardando
            )
            ValidatedTextField(f.rendimiento, viewModel::onRendimientoChange, "Rendimiento (km/L)", Icons.Outlined.LocalGasStation,
                error = f.errores[CampoVehiculo.RENDIMIENTO], keyboardType = KeyboardType.Decimal,
                imeAction = ImeAction.Done, onImeAction = viewModel::guardar, enabled = !f.guardando)
        }
    }

    estado.porCambiar?.let { v ->
        ConfirmDialog(
            titulo = if (v.activo) "Desactivar vehículo" else "Activar vehículo",
            mensaje = if (v.activo) {
                "${v.patente} dejará de aparecer al iniciar una jornada. Sus jornadas anteriores se conservan."
            } else {
                "${v.patente} volverá a aparecer al iniciar una jornada."
            },
            textoConfirmar = if (v.activo) "Desactivar" else "Activar",
            icono = Icons.Outlined.PowerSettingsNew,
            onConfirmar = viewModel::confirmarCambioActivo,
            onCancelar = viewModel::cancelarCambioActivo
        )
    }
}
