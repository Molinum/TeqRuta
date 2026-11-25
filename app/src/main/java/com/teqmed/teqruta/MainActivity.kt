package com.teqmed.teqruta

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.teqmed.teqruta.data.remote.ApiConfig
import com.teqmed.teqruta.ui.components.LocalSincronizacion
import com.teqmed.teqruta.ui.components.SyncControl
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
        // Sin API configurada no hay botón de sincronizar ni llamadas de red
        val sincronizacion = if (ApiConfig.habilitada) SyncControl(app.sync.estado, app.sync::sincronizarAhora) else null
        setContent {
            TeqRutaTheme {
                CompositionLocalProvider(LocalSincronizacion provides sincronizacion) {
                    val sesion: SessionViewModel = viewModel(factory = factory)
                    TeqRutaNavGraph(factory = factory, sesion = sesion)
                }
            }
        }
    }
}
