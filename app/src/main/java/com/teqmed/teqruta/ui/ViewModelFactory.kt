package com.teqmed.teqruta.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.teqmed.teqruta.TeqRutaApp
import com.teqmed.teqruta.ui.auth.LoginViewModel
import com.teqmed.teqruta.ui.tecnico.IniciarJornadaViewModel
import com.teqmed.teqruta.ui.tecnico.RegistrarLlegadaViewModel
import com.teqmed.teqruta.ui.tecnico.RegistrarSalidaViewModel
import com.teqmed.teqruta.ui.tecnico.TecnicoHomeViewModel

/**
 * Crea los ViewModels entregándoles los repositorios que necesitan.
 * Los ViewModels que trabajan con los datos de un usuario usan [conUsuario].
 */
class ViewModelFactory(
    private val app: TeqRutaApp,
    private val usuarioId: Long? = null
) : ViewModelProvider.Factory {

    fun conUsuario(id: Long) = ViewModelFactory(app, id)

    private fun usuario(): Long = requireNotNull(usuarioId) { "Este ViewModel requiere un usuario" }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel = when (modelClass) {
            SessionViewModel::class.java -> SessionViewModel()
            LoginViewModel::class.java -> LoginViewModel(app.authRepository)
            TecnicoHomeViewModel::class.java -> TecnicoHomeViewModel(
                usuario(), app.jornadaRepository, app.catalogoRepository, app.locationProvider
            )
            IniciarJornadaViewModel::class.java -> IniciarJornadaViewModel(
                usuario(), app.jornadaRepository, app.catalogoRepository, app.locationProvider
            )
            RegistrarSalidaViewModel::class.java -> RegistrarSalidaViewModel(
                usuario(), app.jornadaRepository, app.catalogoRepository, app.locationProvider
            )
            RegistrarLlegadaViewModel::class.java ->
                RegistrarLlegadaViewModel(usuario(), app.jornadaRepository, app.locationProvider)
            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        }
        return viewModel as T
    }
}
