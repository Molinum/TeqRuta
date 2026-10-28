package com.teqmed.teqruta.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.ui.theme.TeqRutaTheme

/**
 * Campo de texto con ícono, estado de error y mensaje animado. No valida nada por sí mismo:
 * recibe el [error] ya calculado por el ViewModel.
 */
@Composable
fun ValidatedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    onImeAction: () -> Unit = {},
    isPassword: Boolean = false,
    enabled: Boolean = true
) {
    var mostrarPassword by rememberSaveable { mutableStateOf(false) }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(label) },
            leadingIcon = { Icon(leadingIcon, contentDescription = null) },
            trailingIcon = when {
                isPassword -> {
                    {
                        IconButton(onClick = { mostrarPassword = !mostrarPassword }) {
                            Icon(
                                if (mostrarPassword) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (mostrarPassword) "Ocultar contraseña" else "Mostrar contraseña"
                            )
                        }
                    }
                }
                error != null -> {
                    { Icon(Icons.Outlined.ErrorOutline, contentDescription = "Error") }
                }
                else -> null
            },
            isError = error != null,
            visualTransformation = if (isPassword && !mostrarPassword) {
                PasswordVisualTransformation()
            } else {
                VisualTransformation.None
            },
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            singleLine = true,
            enabled = enabled
        )
        MensajeError(error)
        if (helper != null && error == null) {
            Text(
                text = helper,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, top = 4.dp)
            )
        }
    }
}

/** Mensaje de error bajo un campo, que aparece y desaparece con animación. */
@Composable
internal fun MensajeError(error: String?) {
    // Conserva el último mensaje para que no desaparezca el texto durante la animación de salida
    val ultimoError = remember { mutableStateOf(error) }
    if (error != null) ultimoError.value = error

    AnimatedVisibility(
        visible = error != null,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = ultimoError.value.orEmpty(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ValidatedTextFieldPreview() {
    TeqRutaTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            ValidatedTextField(
                value = "tecnico@teqmed.cl",
                onValueChange = {},
                label = "Correo",
                leadingIcon = Icons.Outlined.Email
            )
            ValidatedTextField(
                value = "tecnico@",
                onValueChange = {},
                label = "Correo",
                leadingIcon = Icons.Outlined.Email,
                error = "Ingresa un correo válido"
            )
            ValidatedTextField(
                value = "",
                onValueChange = {},
                label = "Contraseña",
                leadingIcon = Icons.Outlined.Lock,
                isPassword = true,
                error = "La contraseña es obligatoria"
            )
        }
    }
}
