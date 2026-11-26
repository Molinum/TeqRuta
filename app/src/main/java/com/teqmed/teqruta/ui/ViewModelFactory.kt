package com.teqmed.teqruta.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.teqmed.teqruta.TeqRutaApp
import com.teqmed.teqruta.ui.auth.LoginViewModel
import com.teqmed.teqruta.ui.admin.DestinosViewModel
import com.teqmed.teqruta.ui.admin.TarifaViewModel
import com.teqmed.teqruta.ui.admin.TramosViewModel
import com.teqmed.teqruta.ui.admin.UsuariosViewModel
import com.teqmed.teqruta.ui.admin.VehiculosViewModel
import com.teqmed.teqruta.ui.gerencia.GerenciaViewModel
import com.teqmed.teqruta.ui.jefe.JefeInicioViewModel
import com.teqmed.teqruta.ui.jornada.JornadaDetalleViewModel
import com.teqmed.teqruta.ui.tecnico.HistorialViewModel
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
    private val usuarioId: Long? = null,
    private val jornadaId: Long? = null
) : ViewModelProvider.Factory {

    fun conUsuario(id: Long) = ViewModelFactory(app, id, jornadaId)

    fun conJornada(id: Long) = ViewModelFactory(app, usuarioId, id)

    private fun usuario(): Long = requireNotNull(usuarioId) { "Este ViewModel requiere un usuario" }

    private fun jornada(): Long = requireNotNull(jornadaId) { "Este ViewModel requiere una jornada" }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel = when (modelClass) {
            SessionViewModel::class.java -> SessionViewModel { app.datos.usuarioId = it }
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
            HistorialViewModel::class.java -> HistorialViewModel(usuario(), app.jornadaRepository)
            JornadaDetalleViewModel::class.java ->
                JornadaDetalleViewModel(usuario(), jornada(), app.jornadaRepository)
            JefeInicioViewModel::class.java -> JefeInicioViewModel(usuario(), app.jornadaRepository)
            GerenciaViewModel::class.java -> GerenciaViewModel(app.reporteRepository)
            DestinosViewModel::class.java -> DestinosViewModel(app.adminRepository)
            VehiculosViewModel::class.java -> VehiculosViewModel(app.adminRepository)
            TramosViewModel::class.java -> TramosViewModel(app.adminRepository)
            TarifaViewModel::class.java -> TarifaViewModel(app.adminRepository)
            UsuariosViewModel::class.java -> UsuariosViewModel(usuario(), app.adminRepository)
            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        }
        return viewModel as T
    }
}
