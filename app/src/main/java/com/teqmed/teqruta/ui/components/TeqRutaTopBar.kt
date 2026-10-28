package com.teqmed.teqruta.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

/**
 * Barra superior común. Muestra el botón de volver si se entrega [onVolver] y el de cerrar
 * sesión (con confirmación) si se entrega [onCerrarSesion].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeqRutaTopBar(
    titulo: String,
    subtitulo: String? = null,
    onVolver: (() -> Unit)? = null,
    onCerrarSesion: (() -> Unit)? = null
) {
    var confirmarSalida by rememberSaveable { mutableStateOf(false) }

    TopAppBar(
        title = {
            Column {
                Text(titulo, style = MaterialTheme.typography.titleLarge)
                subtitulo?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        navigationIcon = {
            onVolver?.let {
                IconButton(onClick = it) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Volver")
                }
            }
        },
        actions = {
            if (onCerrarSesion != null) {
                IconButton(onClick = { confirmarSalida = true }) {
                    Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "Cerrar sesión")
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )

    if (confirmarSalida && onCerrarSesion != null) {
        ConfirmDialog(
            titulo = "Cerrar sesión",
            mensaje = "¿Quieres cerrar tu sesión?",
            textoConfirmar = "Cerrar sesión",
            icono = Icons.AutoMirrored.Outlined.Logout,
            onConfirmar = {
                confirmarSalida = false
                onCerrarSesion()
            },
            onCancelar = { confirmarSalida = false }
        )
    }
}
