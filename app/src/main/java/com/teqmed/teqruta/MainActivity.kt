package com.teqmed.teqruta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.ui.SessionViewModel
import com.teqmed.teqruta.ui.ViewModelFactory
import com.teqmed.teqruta.ui.auth.LoginScreen
import com.teqmed.teqruta.ui.theme.TeqRutaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val factory = ViewModelFactory(application as TeqRutaApp)
        setContent {
            TeqRutaTheme {
                val sesion: SessionViewModel = viewModel(factory = factory)
                val usuario by sesion.usuario.collectAsState()

                AnimatedContent(targetState = usuario, label = "sesion") { actual ->
                    if (actual == null) {
                        LoginScreen(
                            viewModel = viewModel(factory = factory),
                            onLoginExitoso = sesion::iniciarSesion
                        )
                    } else {
                        Bienvenida(actual, onCerrarSesion = sesion::cerrarSesion)
                    }
                }
            }
        }
    }
}

/** Pantalla provisoria hasta implementar la navegación por rol. */
@Composable
private fun Bienvenida(usuario: Usuario, onCerrarSesion: () -> Unit) {
    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
        ) {
            Text("Hola, ${usuario.nombre}", style = MaterialTheme.typography.headlineSmall)
            Text(usuario.rol.etiqueta, style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onCerrarSesion) { Text("Cerrar sesión") }
        }
    }
}
