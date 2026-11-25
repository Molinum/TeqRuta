package com.teqmed.teqruta.ui

import androidx.lifecycle.ViewModel
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.data.repository.Sincronizador
import com.teqmed.teqruta.data.repository.SinSincronizacion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Usuario con sesión iniciada, compartido por todas las pantallas de la actividad. */
class SessionViewModel(private val sync: Sincronizador = SinSincronizacion) : ViewModel() {

    private val _usuario = MutableStateFlow<Usuario?>(null)
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    fun iniciarSesion(usuario: Usuario) {
        _usuario.value = usuario
        sync.usuarioId = usuario.id
    }

    fun cerrarSesion() {
        _usuario.value = null
        sync.usuarioId = null
    }
}
