package com.teqmed.teqruta.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

/**
 * Pide el permiso de ubicación la primera vez que se muestra, si aún no está concedido.
 * Si el técnico lo rechaza, la app sigue funcionando y los traslados quedan sin ubicación.
 */
@Composable
fun PedirPermisoUbicacion(tienePermiso: Boolean) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }
    LaunchedEffect(tienePermiso) {
        if (!tienePermiso) {
            launcher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }
}
