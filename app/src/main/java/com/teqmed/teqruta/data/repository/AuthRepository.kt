package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.data.remote.CredencialesDto
import com.teqmed.teqruta.data.remote.TeqRutaApi
import com.teqmed.teqruta.data.remote.mensajeDeError
import retrofit2.HttpException
import kotlin.coroutines.cancellation.CancellationException

sealed interface ResultadoLogin {
    data class Exito(val usuario: Usuario) : ResultadoLogin
    data object CredencialesInvalidas : ResultadoLogin
    data object UsuarioInactivo : ResultadoLogin
    data class SinConexion(val mensaje: String) : ResultadoLogin
}

/** Inicio de sesión contra la API: la contraseña la valida el servidor. */
class AuthRepository(private val api: () -> TeqRutaApi) {

    suspend fun login(email: String, password: String): ResultadoLogin = try {
        ResultadoLogin.Exito(api().login(CredencialesDto(email.trim().lowercase(), password)))
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        when (e.code()) {
            // Mismo mensaje si el correo no existe o la contraseña no coincide (RF-AUT-01)
            401 -> ResultadoLogin.CredencialesInvalidas
            403 -> ResultadoLogin.UsuarioInactivo
            else -> ResultadoLogin.SinConexion(mensajeDeError(e))
        }
    } catch (e: Exception) {
        ResultadoLogin.SinConexion(mensajeDeError(e))
    }
}
