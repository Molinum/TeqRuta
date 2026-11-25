package com.teqmed.teqruta.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.domain.validation.ValidationResult
import com.teqmed.teqruta.domain.validation.mensajeError
import com.teqmed.teqruta.ui.components.ErrorCard
import com.teqmed.teqruta.ui.components.TeqRutaTopBar

/** Agrega el mensaje de [resultado] al mapa de errores del formulario si el campo no es válido. */
internal fun <K> MutableMap<K, String>.agregar(campo: K, resultado: ValidationResult) {
    resultado.mensajeError?.let { put(campo, it) }
}

/** Pantalla de un mantenedor: barra con volver, lista y botón flotante para crear. */
@Composable
internal fun MantenedorScaffold(
    titulo: String,
    onVolver: () -> Unit,
    onNuevo: (() -> Unit)?,
    mensaje: String?,
    lista: LazyListScope.() -> Unit
) {
    Scaffold(
        topBar = { TeqRutaTopBar(titulo, onVolver = onVolver) },
        floatingActionButton = {
            if (onNuevo != null) {
                FloatingActionButton(onClick = onNuevo) { Icon(Icons.Outlined.Add, contentDescription = "Nuevo") }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { ErrorCard(mensaje) }
            lista()
        }
    }
}

/** Fila de un mantenedor con su título, detalle y los botones de editar y activar/desactivar. */
@Composable
internal fun ItemMantenedor(
    titulo: String,
    detalle: String,
    activo: Boolean = true,
    onEditar: (() -> Unit)?,
    onCambiarActivo: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colores = if (activo) CardDefaults.elevatedCardColors()
    else CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ElevatedCard(modifier = modifier.fillMaxWidth(), colors = colores) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    if (activo) titulo else "$titulo (inactivo)",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (activo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (onEditar != null) {
                IconButton(onClick = onEditar) { Icon(Icons.Outlined.Edit, contentDescription = "Editar") }
            }
            if (onCambiarActivo != null) {
                IconButton(onClick = onCambiarActivo) {
                    Icon(
                        Icons.Outlined.PowerSettingsNew,
                        contentDescription = if (activo) "Desactivar" else "Activar",
                        tint = if (activo) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
