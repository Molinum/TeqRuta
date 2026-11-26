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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalContext
import com.teqmed.teqruta.data.remote.EstadoConexion
import com.teqmed.teqruta.data.remote.mensajeDeError
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** Acceso desde la interfaz al estado de la conexión con la API y a la acción de actualizar. */
class ControlActualizacion(
    val estado: StateFlow<EstadoConexion>,
    val actualizar: suspend () -> Result<Unit>
)

val LocalActualizacion = staticCompositionLocalOf<ControlActualizacion?> { null }

/**
 * Botón de la barra superior: vuelve a pedir los datos al servidor. Gira mientras espera la
 * respuesta y muestra una nube tachada si la última consulta falló (por ejemplo sin conexión).
 */
@Composable
fun BotonActualizar() {
    val control = LocalActualizacion.current ?: return
    val estado by control.estado.collectAsState()
    val alcance = rememberCoroutineScope()
    val contexto = LocalContext.current

    val giro by rememberInfiniteTransition(label = "actualizar").animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing), RepeatMode.Restart),
        label = "giro"
    )
    val cargando = estado is EstadoConexion.Cargando
    val conError = estado is EstadoConexion.Error

    IconButton(onClick = {
        alcance.launch {
            control.actualizar()
                .onSuccess { Toast.makeText(contexto, "Datos actualizados", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(contexto, mensajeDeError(it), Toast.LENGTH_LONG).show() }
        }
    }) {
        Icon(
            if (conError) Icons.Outlined.CloudOff else Icons.Outlined.Refresh,
            contentDescription = "Actualizar datos del servidor",
            modifier = if (cargando) Modifier.rotate(giro) else Modifier
        )
    }
}
