package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.dao.UsuarioDao
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.domain.auth.PasswordHasher

sealed interface ResultadoLogin {
    data class Exito(val usuario: Usuario) : ResultadoLogin
    data object CredencialesInvalidas : ResultadoLogin
    data object UsuarioInactivo : ResultadoLogin
}

class AuthRepository(private val usuarioDao: UsuarioDao) {

    suspend fun login(email: String, password: String): ResultadoLogin {
        val usuario = usuarioDao.obtenerPorEmail(email.trim().lowercase())
            ?: return ResultadoLogin.CredencialesInvalidas
        // Mismo mensaje si el correo no existe o la contraseña no coincide (RF-AUT-01)
        if (!PasswordHasher.coincide(password, usuario.passwordHash)) {
            return ResultadoLogin.CredencialesInvalidas
        }
        if (!usuario.activo) return ResultadoLogin.UsuarioInactivo
        return ResultadoLogin.Exito(usuario)
    }

    suspend fun obtenerUsuario(id: Long): Usuario? = usuarioDao.obtenerPorId(id)
}
