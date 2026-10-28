package com.teqmed.teqruta.ui.auth

import com.teqmed.teqruta.data.local.entity.Usuario

data class LoginFormState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    /** Error que no corresponde a un campo, ej. credenciales incorrectas. */
    val errorGeneral: String? = null,
    val cargando: Boolean = false,
    /** Se completa cuando el login es exitoso; la pantalla navega y lo consume. */
    val usuarioAutenticado: Usuario? = null
)
