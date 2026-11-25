package com.teqmed.teqruta.ui.admin

import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.teqmed.teqruta.domain.model.Rol
import com.teqmed.teqruta.ui.components.ConfirmDialog
import com.teqmed.teqruta.ui.components.DropdownField
import com.teqmed.teqruta.ui.components.FormDialog
import com.teqmed.teqruta.ui.components.ValidatedTextField

@Composable
fun UsuariosScreen(viewModel: UsuariosViewModel, onVolver: () -> Unit) {
    val estado by viewModel.estado.collectAsState()

    MantenedorScaffold("Usuarios", onVolver, viewModel::nuevo, estado.mensaje) {
        items(estado.usuarios, key = { it.id }) { u ->
            val jefe = estado.nombreDe(u.jefeId)?.let { " · Jefe: $it" }.orEmpty()
            ItemMantenedor(
                titulo = u.nombre,
                detalle = "${u.email} · ${u.rol.etiqueta}$jefe",
                activo = u.activo,
                onEditar = { viewModel.editar(u) },
                onCambiarActivo = { viewModel.pedirCambioActivo(u) },
                modifier = Modifier.animateItem()
            )
        }
    }

    estado.form?.let { f ->
        val edita = f.id != 0L
        FormDialog(
            titulo = if (edita) "Editar usuario" else "Nuevo usuario",
            onGuardar = viewModel::guardar,
            onCancelar = viewModel::cerrarForm,
            guardando = f.guardando,
            errorGeneral = f.errorGeneral
        ) {
            ValidatedTextField(f.nombre, viewModel::onNombreChange, "Nombre", Icons.Outlined.Person,
                error = f.errores[CampoUsuario.NOMBRE], enabled = !f.guardando)
            ValidatedTextField(f.email, viewModel::onEmailChange, "Correo", Icons.Outlined.Email,
                error = f.errores[CampoUsuario.EMAIL], keyboardType = KeyboardType.Email, enabled = !f.guardando)
            ValidatedTextField(f.password, viewModel::onPasswordChange, "Contraseña", Icons.Outlined.Lock,
                error = f.errores[CampoUsuario.PASSWORD], isPassword = true,
                helper = if (edita) "Déjala vacía para mantener la actual" else "Mínimo 6 caracteres",
                imeAction = ImeAction.Done, enabled = !f.guardando)
            DropdownField(
                label = "Rol", options = Rol.entries, selected = f.rol,
                onSelected = viewModel::onRolChange, optionLabel = { it.etiqueta },
                leadingIcon = Icons.Outlined.AdminPanelSettings, error = f.errores[CampoUsuario.ROL],
                enabled = !f.guardando && !f.esCuentaPropia
            )
            if (f.rol == Rol.TECNICO) {
                DropdownField(
                    label = "Jefe a cargo", options = estado.jefes,
                    selected = estado.jefes.firstOrNull { it.id == f.jefeId },
                    onSelected = viewModel::onJefeChange, optionLabel = { it.nombre }, optionDetail = { it.email },
                    leadingIcon = Icons.Outlined.SupervisorAccount, error = f.errores[CampoUsuario.JEFE],
                    enabled = !f.guardando
                )
            }
        }
    }

    estado.porCambiar?.let { u ->
        ConfirmDialog(
            titulo = if (u.activo) "Desactivar usuario" else "Activar usuario",
            mensaje = if (u.activo) {
                "${u.nombre} ya no podrá iniciar sesión. Sus registros se conservan."
            } else {
                "${u.nombre} podrá volver a iniciar sesión."
            },
            textoConfirmar = if (u.activo) "Desactivar" else "Activar",
            icono = Icons.Outlined.PowerSettingsNew,
            onConfirmar = viewModel::confirmarCambioActivo,
            onCancelar = viewModel::cancelarCambioActivo
        )
    }
}
