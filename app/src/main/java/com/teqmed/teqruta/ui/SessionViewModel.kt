package com.teqmed.teqruta.ui

import androidx.lifecycle.ViewModel
import com.teqmed.teqruta.data.local.entity.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Usuario con sesión iniciada, compartido por todas las pantallas de la actividad.
 * [alCambiarUsuario] avisa a la capa de datos qué jornadas debe pedir al servidor.
 */
class SessionViewModel(private val alCambiarUsuario: (Long?) -> Unit = {}) : ViewModel() {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun iniciarSesion(usuario: Usuario) {
        _usuario.value = usuario
        alCambiarUsuario(usuario.id)
    }

    fun cerrarSesion() {
        _usuario.value = null
        alCambiarUsuario(null)
    }
}
