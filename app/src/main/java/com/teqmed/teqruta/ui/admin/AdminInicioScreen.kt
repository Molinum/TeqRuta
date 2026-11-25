package com.teqmed.teqruta.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocalGasStation
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.ui.components.TeqRutaTopBar

@Composable
fun AdminInicioScreen(
    subtitulo: String,
    onDestinos: () -> Unit,
    onVehiculos: () -> Unit,
    onTramos: () -> Unit,
    onTarifa: () -> Unit,
    onUsuarios: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    Scaffold(topBar = { TeqRutaTopBar("Administración", subtitulo, onCerrarSesion = onCerrarSesion) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Opcion(Icons.Outlined.Place, "Destinos", "Sede, clínicas y hospitales", onDestinos)
            Opcion(Icons.Outlined.DirectionsCar, "Vehículos", "Patente, tipo y rendimiento", onVehiculos)
            Opcion(Icons.Outlined.Route, "Tramos", "Distancias oficiales y peajes, con sus versiones", onTramos)
            Opcion(Icons.Outlined.LocalGasStation, "Tarifa de combustible", "Precio por litro e historial", onTarifa)
            Opcion(Icons.Outlined.Groups, "Usuarios", "Cuentas, roles y jefe a cargo", onUsuarios)
        }
    }
}

@Composable
private fun Opcion(icono: ImageVector, titulo: String, descripcion: String, onClick: () -> Unit) {
    ElevatedCard(Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icono, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(descripcion, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
