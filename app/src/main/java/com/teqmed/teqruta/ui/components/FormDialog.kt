package com.teqmed.teqruta.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Diálogo con un formulario desplazable, botón de guardar y el error general del envío. */
@Composable
fun FormDialog(
    titulo: String,
    onGuardar: () -> Unit,
    onCancelar: () -> Unit,
    guardando: Boolean = false,
    errorGeneral: String? = null,
    textoGuardar: String = "Guardar",
    contenido: @Composable () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!guardando) onCancelar() },
        title = { Text(titulo) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                contenido()
                ErrorCard(errorGeneral)
            }
        },
        confirmButton = {
            TextButton(onClick = onGuardar, enabled = !guardando) {
                if (guardando) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text(textoGuardar)
            }
        },
        dismissButton = { TextButton(onClick = onCancelar, enabled = !guardando) { Text("Cancelar") } }
    )
}
