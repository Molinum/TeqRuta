package com.teqmed.teqruta.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.data.repository.AdminRepository
import com.teqmed.teqruta.domain.auth.PasswordHasher
import com.teqmed.teqruta.domain.model.Rol
import com.teqmed.teqruta.domain.validation.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CampoUsuario { NOMBRE, EMAIL, PASSWORD, ROL, JEFE }

data class UsuarioForm(
    val id: Long = 0,
    val nombre: String = "",
    val email: String = "",
    val password: String = "",
    val rol: Rol? = null,
    val jefeId: Long? = null,
    val activo: Boolean = true,
    /** El administrador que edita su propia cuenta no puede cambiarse el rol. */
    val esCuentaPropia: Boolean = false,
    val errores: Map<CampoUsuario, String> = emptyMap(),
    val errorGeneral: String? = null,
    val guardando: Boolean = false
)

data class UsuariosState(
    val usuarios: List<Usuario> = emptyList(),
    val form: UsuarioForm? = null,
    val porCambiar: Usuario? = null,
    val mensaje: String? = null
) {
    val jefes get() = usuarios.filter { it.rol == Rol.JEFE && it.activo }
    fun nombreDe(id: Long?) = usuarios.firstOrNull { it.id == id }?.nombre
}

/** Mantenedor de usuarios: rol y, para los técnicos, el jefe a cargo (RF-ADM-06). */
class UsuariosViewModel(
    private val adminId: Long,
    private val adminRepository: AdminRepository
) : ViewModel() {

    private val form = MutableStateFlow<UsuarioForm?>(null)
    private val porCambiar = MutableStateFlow<Usuario?>(null)
    private val mensaje = MutableStateFlow<String?>(null)

    val estado: StateFlow<UsuariosState> = combine(
        adminRepository.observarUsuarios(), form, porCambiar, mensaje, ::UsuariosState
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), UsuariosState())

    fun nuevo() {
        mensaje.value = null
        form.value = UsuarioForm()
    }

    fun editar(u: Usuario) {
        mensaje.value = null
        form.value = UsuarioForm(
            id = u.id, nombre = u.nombre, email = u.email, rol = u.rol, jefeId = u.jefeId,
            activo = u.activo, esCuentaPropia = u.id == adminId
        )
    }

    fun cerrarForm() { form.value = null }

    private fun cambiar(campo: CampoUsuario, cambio: (UsuarioForm) -> UsuarioForm) =
        form.update { f -> f?.let { cambio(it).copy(errores = it.errores - campo) } }

    fun onNombreChange(v: String) = cambiar(CampoUsuario.NOMBRE) { it.copy(nombre = v) }
    fun onEmailChange(v: String) = cambiar(CampoUsuario.EMAIL) { it.copy(email = v) }
    fun onPasswordChange(v: String) = cambiar(CampoUsuario.PASSWORD) { it.copy(password = v) }
    fun onRolChange(v: Rol) = cambiar(CampoUsuario.ROL) { it.copy(rol = v, jefeId = if (v == Rol.TECNICO) it.jefeId else null) }
    fun onJefeChange(v: Usuario) = cambiar(CampoUsuario.JEFE) { it.copy(jefeId = v.id) }

    fun guardar() {
        val f = form.value ?: return
        if (f.guardando) return
        val existentes = estado.value.usuarios
        val errores = buildMap {
            agregar(CampoUsuario.NOMBRE, Validators.requerido(f.nombre, "Nombre"))
            agregar(
                CampoUsuario.EMAIL,
                Validators.primerError(
                    Validators.correo(f.email),
                    Validators.sinDuplicado(
                        f.email, existentes.filter { it.id != f.id }.map { it.email }, "Ya existe un usuario con ese correo"
                    )
                )
            )
            // Al editar, la contraseña vacía conserva la actual
            if (f.id == 0L || f.password.isNotEmpty()) {
                agregar(CampoUsuario.PASSWORD, Validators.longitudMinima(f.password, "La contraseña", 6))
            }
            agregar(CampoUsuario.ROL, Validators.seleccion(f.rol, "un rol"))
            if (f.rol == Rol.TECNICO) agregar(CampoUsuario.JEFE, Validators.seleccion(f.jefeId, "un jefe"))
        }
        if (errores.isNotEmpty()) {
            form.update { it?.copy(errores = errores) }
            return
        }
        val rol = f.rol ?: return
        viewModelScope.launch {
            form.update { it?.copy(guardando = true, errorGeneral = null) }
            val anterior = existentes.firstOrNull { it.id == f.id }
            adminRepository.guardarUsuario(
                Usuario(
                    id = f.id,
                    nombre = f.nombre.trim(),
                    email = f.email.trim().lowercase(),
                    passwordHash = if (f.password.isNotEmpty()) PasswordHasher.hash(f.password)
                    else anterior?.passwordHash.orEmpty(),
                    rol = rol,
                    jefeId = if (rol == Rol.TECNICO) f.jefeId else null,
                    activo = f.activo
                )
            )
                .onSuccess { form.value = null }
                .onFailure { e -> form.update { it?.copy(guardando = false, errorGeneral = e.message) } }
        }
    }

    fun pedirCambioActivo(u: Usuario) {
        mensaje.value = null
        porCambiar.value = u
    }

    fun cancelarCambioActivo() { porCambiar.value = null }

    fun confirmarCambioActivo() {
        val u = porCambiar.value ?: return
        porCambiar.value = null
        if (u.id == adminId && u.activo) {
            mensaje.value = "No puedes desactivar tu propia cuenta"
            return
        }
        viewModelScope.launch {
            adminRepository.guardarUsuario(u.copy(activo = !u.activo)).onFailure { mensaje.value = it.message }
        }
    }
}
