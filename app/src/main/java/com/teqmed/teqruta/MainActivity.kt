package com.teqmed.teqruta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teqmed.teqruta.ui.components.ControlActualizacion
import com.teqmed.teqruta.ui.components.LocalActualizacion
import com.teqmed.teqruta.ui.SessionViewModel
import com.teqmed.teqruta.ui.ViewModelFactory
import com.teqmed.teqruta.ui.navigation.TeqRutaNavGraph
import com.teqmed.teqruta.ui.theme.TeqRutaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as TeqRutaApp
        val factory = ViewModelFactory(app)
        val actualizacion = ControlActualizacion(app.datos.estado, app.datos::actualizarTodo)
        setContent {
            TeqRutaTheme {
                CompositionLocalProvider(LocalActualizacion provides actualizacion) {
                    val sesion: SessionViewModel = viewModel(factory = factory)
                    TeqRutaNavGraph(factory = factory, sesion = sesion)
                }
            }
        }
    }
}
