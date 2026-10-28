package com.teqmed.teqruta.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/** Diálogo de confirmación para acciones que no se pueden deshacer fácilmente. */
@Composable
fun ConfirmDialog(
    titulo: String,
    mensaje: String,
    textoConfirmar: String,
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
    icono: ImageVector? = null
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        icon = icono?.let { { Icon(it, contentDescription = null) } },
        title = { Text(titulo) },
        text = { Text(mensaje) },
        confirmButton = { TextButton(onClick = onConfirmar) { Text(textoConfirmar) } },
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}
