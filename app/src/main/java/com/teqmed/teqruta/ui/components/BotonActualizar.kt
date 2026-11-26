package com.teqmed.teqruta.ui.components

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import com.teqmed.teqruta.data.remote.mensajeDeError
import com.teqmed.teqruta.data.sync.SyncEstado
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Acceso a la sincronización desde la interfaz. null si la API no está configurada. */
class SyncControl(
    val estado: StateFlow<SyncEstado>,
    val sincronizar: suspend () -> Result<Unit>
)

val LocalSincronizacion = staticCompositionLocalOf<SyncControl?> { null }

/**
 * Botón de la barra superior: sincroniza al mostrarse la pantalla y al tocarlo. Gira mientras
 * sincroniza y muestra una nube tachada si el último intento falló (por ejemplo sin conexión).
 */
@Composable
fun BotonSincronizar() {
    val control = LocalSincronizacion.current ?: return
    val estado by control.estado.collectAsState()
    val alcance = rememberCoroutineScope()
    val contexto = LocalContext.current

    LaunchedEffect(Unit) { control.sincronizar() }

    val giro by rememberInfiniteTransition(label = "sync").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "giro"
    )
    val sincronizando = estado is SyncEstado.Sincronizando
    val conError = estado is SyncEstado.Error

    IconButton(onClick = {
        alcance.launch {
            control.sincronizar()
                .onSuccess { Toast.makeText(contexto, "Sincronizado", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(contexto, mensajeDeError(it), Toast.LENGTH_LONG).show() }
        }
    }) {
        Icon(
            if (conError) Icons.Outlined.CloudOff else Icons.Outlined.Sync,
            contentDescription = "Sincronizar con el servidor",
            modifier = if (sincronizando) Modifier.rotate(giro) else Modifier
        )
    }
}
