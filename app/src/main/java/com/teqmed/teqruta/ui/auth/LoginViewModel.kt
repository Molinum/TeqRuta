package com.teqmed.teqruta.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.repository.AuthRepository
import com.teqmed.teqruta.data.repository.ResultadoLogin
import com.teqmed.teqruta.domain.validation.Validators
import com.teqmed.teqruta.domain.validation.esValido
import com.teqmed.teqruta.domain.validation.mensajeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _estado = MutableStateFlow(LoginFormState())
    val estado: StateFlow<LoginFormState> = _estado.asStateFlow()

    fun onEmailChange(valor: String) {
        _estado.update { it.copy(email = valor, emailError = null, errorGeneral = null) }
    }

    fun onPasswordChange(valor: String) {
        _estado.update { it.copy(password = valor, passwordError = null, errorGeneral = null) }
    }

    fun onSubmit() {
        val actual = _estado.value
        if (actual.cargando) return

        val emailResultado = Validators.correo(actual.email)
        val passwordResultado = Validators.requerido(actual.password, "Contraseña")
        _estado.update {
            it.copy(
                emailError = emailResultado.mensajeError,
                passwordError = passwordResultado.mensajeError
            )
        }
        if (!emailResultado.esValido || !passwordResultado.esValido) return

        viewModelScope.launch {
            _estado.update { it.copy(cargando = true, errorGeneral = null) }
            when (val resultado = authRepository.login(actual.email, actual.password)) {
                is ResultadoLogin.Exito -> _estado.update {
                    it.copy(cargando = false, usuarioAutenticado = resultado.usuario)
                }
                ResultadoLogin.CredencialesInvalidas -> _estado.update {
                    it.copy(cargando = false, errorGeneral = "Correo o contraseña incorrectos")
                }
                ResultadoLogin.UsuarioInactivo -> _estado.update {
                    it.copy(
                        cargando = false,
                        errorGeneral = "Tu usuario está desactivado. Contacta al administrador."
                    )
                }
            }
        }
    }

    /** Limpia el formulario después de navegar, para que no quede la contraseña en memoria. */
    fun onLoginConsumido() {
        _estado.value = LoginFormState()
    }
}
