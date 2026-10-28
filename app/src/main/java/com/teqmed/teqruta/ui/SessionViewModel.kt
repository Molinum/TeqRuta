package com.teqmed.teqruta.ui

import androidx.lifecycle.ViewModel
import com.teqmed.teqruta.data.local.entity.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Usuario con sesión iniciada, compartido por todas las pantallas de la actividad. */
class SessionViewModel : ViewModel() {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun iniciarSesion(usuario: Usuario) {
        _usuario.value = usuario
    }

    fun cerrarSesion() {
        _usuario.value = null
    }
}
