package com.teqmed.teqruta.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.ui.theme.TeqRutaTheme

/**
 * Selector desplegable con el mismo estilo de error que [ValidatedTextField].
 * [optionDetail] agrega una segunda línea opcional a cada opción (ej. patente o ciudad).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    optionDetail: ((T) -> String)? = null,
    error: String? = null,
    enabled: Boolean = true
) {
    var expandido by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(
            expanded = expandido,
            onExpandedChange = { if (enabled) expandido = it }
        ) {
            OutlinedTextField(
                value = selected?.let(optionLabel).orEmpty(),
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled)
                    .fillMaxWidth(),
                label = { Text(label) },
                leadingIcon = { Icon(leadingIcon, contentDescription = null) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
                isError = error != null,
                singleLine = true,
                enabled = enabled
            )
            ExposedDropdownMenu(
                expanded = expandido,
                onDismissRequest = { expandido = false }
            ) {
                options.forEach { opcion ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(optionLabel(opcion))
                                optionDetail?.let {
                                    Text(
                                        it(opcion),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        onClick = {
                            onSelected(opcion)
                            expandido = false
                        }
                    )
                }
            }
        }
        MensajeError(error)
    }
}

@Preview(showBackground = true)
@Composable
private fun DropdownFieldPreview() {
    TeqRutaTheme {
        DropdownField(
            label = "Vehículo",
            options = listOf("Toyota Hilux 2.4", "Nissan NP300"),
            selected = null,
            onSelected = {},
            optionLabel = { it },
            leadingIcon = Icons.Outlined.DirectionsCar,
            error = "Selecciona un vehículo",
            modifier = Modifier.padding(16.dp)
        )
    }
}
