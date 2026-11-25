package com.teqmed.teqruta.ui.admin

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.LocationCity
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.domain.model.TipoDestino
import com.teqmed.teqruta.ui.components.ConfirmDialog
import com.teqmed.teqruta.ui.components.DropdownField
import com.teqmed.teqruta.ui.components.FormDialog
import com.teqmed.teqruta.ui.components.ValidatedTextField

@Composable
fun DestinosScreen(viewModel: DestinosViewModel, onVolver: () -> Unit) {
    val estado by viewModel.estado.collectAsState()

    MantenedorScaffold("Destinos", onVolver, viewModel::nuevo, estado.mensaje) {
        items(estado.destinos, key = { it.id }) { d ->
            ItemMantenedor(
                titulo = d.nombre,
                detalle = "${d.tipo.etiqueta} · ${d.ciudad} · ${d.direccion}",
                activo = d.activo,
                onEditar = { viewModel.editar(d) },
                onCambiarActivo = { viewModel.pedirCambioActivo(d) },
                modifier = Modifier.animateItem()
            )
        }
    }

    estado.form?.let { f ->
        FormDialog(
            titulo = if (f.id == 0L) "Nuevo destino" else "Editar destino",
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarForm,
            guardando = f.guardando,
            errorGeneral = f.errorGeneral
        ) {
            ValidatedTextField(f.nombre, viewModel::onNombreChange, "Nombre", Icons.Outlined.Apartment,
                error = f.errores[CampoDestino.NOMBRE], enabled = !f.guardando)
            DropdownField(
                label = "Tipo", options = TipoDestino.entries, selected = f.tipo,
                onSelected = viewModel::onTipoChange, optionLabel = { it.etiqueta },
                leadingIcon = Icons.Outlined.Home, error = f.errores[CampoDestino.TIPO], enabled = !f.guardando
            )
            ValidatedTextField(f.direccion, viewModel::onDireccionChange, "Dirección", Icons.Outlined.Place,
                error = f.errores[CampoDestino.DIRECCION], enabled = !f.guardando)
            ValidatedTextField(f.ciudad, viewModel::onCiudadChange, "Ciudad", Icons.Outlined.LocationCity,
                error = f.errores[CampoDestino.CIUDAD], enabled = !f.guardando)
            ValidatedTextField(f.lat, viewModel::onLatChange, "Latitud", Icons.Outlined.Explore,
                error = f.errores[CampoDestino.LAT], helper = "Ej. -36.8285",
                keyboardType = KeyboardType.Decimal, enabled = !f.guardando)
            ValidatedTextField(f.lon, viewModel::onLonChange, "Longitud", Icons.Outlined.Public,
                error = f.errores[CampoDestino.LON], helper = "Ej. -73.0605",
                keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done,
                onImeAction = viewModel::guardar, enabled = !f.guardando)
        }
    }

    estado.porCambiar?.let { d ->
        ConfirmDialog(
            titulo = if (d.activo) "Desactivar destino" else "Activar destino",
            mensaje = if (d.activo) {
                "${d.nombre} dejará de aparecer en los formularios de registro. Sus traslados anteriores se conservan."
            } else {
                "${d.nombre} volverá a aparecer en los formularios de registro."
            },
            textoConfirmar = if (d.activo) "Desactivar" else "Activar",
            icono = Icons.Outlined.PowerSettingsNew,
            onConfirmar = viewModel::confirmarCambioActivo,
            onCancelar = viewModel::cancelarCambioActivo
        )
    }
}
