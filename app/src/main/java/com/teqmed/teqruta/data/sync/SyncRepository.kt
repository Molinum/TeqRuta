package com.teqmed.teqruta.data.sync

import android.content.SharedPreferences
import android.util.Log
import androidx.room.withTransaction
import com.teqmed.teqruta.data.local.AppDatabase
import com.teqmed.teqruta.data.remote.ApiConfig
import com.teqmed.teqruta.data.remote.CatalogosDto
import com.teqmed.teqruta.data.remote.JornadaDto
import com.teqmed.teqruta.data.remote.TeqRutaApi
import com.teqmed.teqruta.data.remote.aDto
import com.teqmed.teqruta.data.remote.aEntidad
import com.teqmed.teqruta.data.remote.mensajeDeError
import com.teqmed.teqruta.data.repository.Sincronizador
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import kotlin.coroutines.cancellation.CancellationException

sealed interface SyncEstado {
    data object Inactiva : SyncEstado
    data object Sincronizando : SyncEstado
    data class Ok(val hora: Long) : SyncEstado
    data class Error(val mensaje: String) : SyncEstado
}

/**
 * Sincroniza Room con la API sin quitarle a la app su funcionamiento sin conexión:
 *  1. baja el catálogo (el servidor manda: usuarios, vehículos, destinos, tramos, tarifas),
 *  2. sube las jornadas con cambios pendientes (con sus traslados),
 *  3. baja las jornadas que el usuario puede ver y que cambiaron desde la última vez.
 * Si algo falla por falta de red, lo pendiente queda marcado y se reintenta en la próxima.
 */
class SyncRepository(
    private val db: AppDatabase,
    private val api: () -> TeqRutaApi,
    private val prefs: SharedPreferences
) : Sincronizador {

    private companion object {
        const val TAG = "TeqRutaSync"
    }

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutex = Mutex()

    private val _estado = MutableStateFlow<SyncEstado>(SyncEstado.Inactiva)
    val estado: StateFlow<SyncEstado> = _estado.asStateFlow()

    @Volatile
    override var usuarioId: Long? = null

    override fun solicitar() {
        if (ApiConfig.habilitada) alcance.launch { sincronizar() }
    }

    /** Como [sincronizar], pero no se cancela si la pantalla que lo pidió se cierra. */
    suspend fun sincronizarAhora(): Result<Unit> = alcance.async { sincronizar() }.await()

    suspend fun sincronizar(): Result<Unit> {
        if (!ApiConfig.habilitada) return Result.success(Unit)
        return mutex.withLock {
            _estado.value = SyncEstado.Sincronizando
            try {
                aplicarCatalogos(api().catalogos())
                val errorParcial = subirPendientes()
                bajarJornadas()
                if (errorParcial != null) throw IllegalStateException(errorParcial)
                _estado.value = SyncEstado.Ok(System.currentTimeMillis())
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Sincronización fallida", e)
                _estado.value = SyncEstado.Error(mensajeDeError(e))
                Result.failure(e)
            }
        }
    }

    override suspend fun descargarCatalogos(): Result<Unit> {
        if (!ApiConfig.habilitada) return Result.failure(IllegalStateException("La API no está configurada"))
        return mutex.withLock {
            try {
                aplicarCatalogos(api().catalogos())
                Result.success(Unit)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo descargar el catálogo", e)
                Result.failure(e)
            }
        }
    }

    private suspend fun aplicarCatalogos(c: CatalogosDto) = db.withTransaction {
        db.usuarioDao().upsertTodos(c.usuarios)
        db.vehiculoDao().upsertTodos(c.vehiculos)
        db.destinoDao().upsertTodos(c.destinos)
        db.tramoDao().upsertTodos(c.tramos)
        db.tarifaDao().upsertTodos(c.tarifas)
    }

    /** Devuelve el mensaje del primer error de validación del servidor, si hubo; sin red lanza la excepción. */
    private suspend fun subirPendientes(): String? {
        var error: String? = null
        for (jornada in db.jornadaDao().obtenerPendientes()) {
            val traslados = db.trasladoDao().obtenerPorJornada(jornada.id)
            try {
                api().subirJornada(jornada.uuid, jornada.aDto(traslados))
                db.jornadaDao().marcarSincronizada(jornada.id, jornada.actualizadoEn)
            } catch (e: HttpException) {
                // El servidor rechazó esta jornada: se sigue con las demás y queda pendiente
                Log.w(TAG, "El servidor rechazó la jornada ${jornada.uuid}", e)
                if (error == null) error = mensajeDeError(e)
            }
        }
        return error
    }

    private suspend fun bajarJornadas() {
        val usuario = usuarioId ?: return
        val clave = "desde_$usuario"
        val respuesta = api().jornadas(usuario, prefs.getLong(clave, 0L))
        db.withTransaction { respuesta.jornadas.forEach { aplicarJornada(it) } }
        prefs.edit().putLong(clave, respuesta.ahora).apply()
    }

    /** El servidor manda, salvo que el dispositivo tenga cambios locales aún sin enviar. */
    private suspend fun aplicarJornada(dto: JornadaDto) {
        val local = db.jornadaDao().obtenerPorUuid(dto.uuid)
        if (local?.pendienteSync == true) return
        val id = if (local == null) {
            db.jornadaDao().insertar(dto.aEntidad(0))
        } else {
            db.jornadaDao().actualizar(dto.aEntidad(local.id))
            local.id
        }
        db.trasladoDao().eliminarPorJornada(id)
        db.trasladoDao().insertarTodos(dto.traslados.map { it.aEntidad(id) })
    }
}
