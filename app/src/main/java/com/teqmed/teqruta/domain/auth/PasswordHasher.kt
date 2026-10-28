package com.teqmed.teqruta.domain.auth

import java.security.MessageDigest

/**
 * Hash SHA-256 de las contraseñas para no guardarlas en texto plano.
 * Suficiente para una app local de demostración; en producción se usaría un algoritmo
 * con sal y costo configurable (bcrypt, Argon2).
 */
object PasswordHasher {
    fun hash(password: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(password.toByteArray())
            .joinToString("") { "%02x".format(it) }

    fun coincide(password: String, hash: String): Boolean = hash(password) == hash
}
