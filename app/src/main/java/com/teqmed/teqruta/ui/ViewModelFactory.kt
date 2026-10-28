package com.teqmed.teqruta.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.teqmed.teqruta.TeqRutaApp
import com.teqmed.teqruta.ui.auth.LoginViewModel

/** Crea los ViewModels entregándoles los repositorios que necesitan. */
class ViewModelFactory(private val app: TeqRutaApp) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val viewModel: ViewModel = when (modelClass) {
            SessionViewModel::class.java -> SessionViewModel()
            LoginViewModel::class.java -> LoginViewModel(app.authRepository)
            else -> throw IllegalArgumentException("ViewModel desconocido: ${modelClass.name}")
        }
        return viewModel as T
    }
}
