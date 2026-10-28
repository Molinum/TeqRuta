package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.dao.UsuarioDao
import com.teqmed.teqruta.data.local.entity.Usuario

class AuthRepository(private val usuarioDao: UsuarioDao) {

    suspend fun obtenerUsuario(id: Long): Usuario? = usuarioDao.obtenerPorId(id)
}
