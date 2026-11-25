package com.teqmed.teqruta.data.repository

import androidx.room.withTransaction
import com.teqmed.teqruta.data.local.AppDatabase
import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.TarifaCombustible
import com.teqmed.teqruta.data.local.entity.Tramo
import com.teqmed.teqruta.data.local.entity.TramoConNombres
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.data.local.entity.Vehiculo
import com.teqmed.teqruta.data.remote.ApiConfig
import com.teqmed.teqruta.data.remote.TeqRutaApi
import com.teqmed.teqruta.data.remote.mensajeDeError
import kotlinx.coroutines.flow.Flow
import kotlin.coroutines.cancellation.CancellationException

/**
 * Mantenedores del administrador: destinos, vehículos, tramos, tarifas y usuarios.
 * Nada se elimina: lo que tiene registros asociados se desactiva (RN-13).
 *
 * Con la API configurada el servidor es la fuente de verdad del catálogo: el cambio se envía
 * primero allí (que valida y asigna los ids) y luego se baja a Room; sin conexión no se puede
 * modificar. Sin API, los cambios se guardan directamente en Room.
 */
class AdminRepository(
    private val db: AppDatabase,
    private val api: () -> TeqRutaApi,
    private val sync: Sincronizador = SinSincronizacion
) {

    private val destinoDao = db.destinoDao()
    private val vehiculoDao = db.vehiculoDao()
    private val tramoDao = db.tramoDao()
    private val tarifaDao = db.tarifaDao()
    private val usuarioDao = db.usuarioDao()

    /** Ejecuta [local] sin API, o [remoto] y luego refresca el catálogo desde el servidor. */
    private suspend fun escribir(local: suspend () -> Unit, remoto: suspend () -> Unit): Result<Unit> {
        if (!ApiConfig.habilitada) {
            local()
            return Result.success(Unit)
        }
        return try {
            remoto()
            sync.descargarCatalogos().getOrThrow()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(ReglaNegocioException(mensajeDeError(e)))
        }
    }

    fun observarDestinos(): Flow<List<Destino>> = destinoDao.observarTodos()

    suspend fun guardarDestino(destino: Destino): Result<Unit> = escribir(
        local = { if (destino.id == 0L) destinoDao.insertar(destino) else destinoDao.actualizar(destino) },
        remoto = { api().guardarDestino(destino) }
    )

    fun observarVehiculos(): Flow<List<Vehiculo>> = vehiculoDao.observarTodos()

    suspend fun guardarVehiculo(vehiculo: Vehiculo): Result<Unit> = escribir(
        local = { if (vehiculo.id == 0L) vehiculoDao.insertar(vehiculo) else vehiculoDao.actualizar(vehiculo) },
        remoto = { api().guardarVehiculo(vehiculo) }
    )

    fun observarUsuarios(): Flow<List<Usuario>> = usuarioDao.observarTodos()

    suspend fun guardarUsuario(usuario: Usuario): Result<Unit> = escribir(
        local = { if (usuario.id == 0L) usuarioDao.insertar(usuario) else usuarioDao.actualizar(usuario) },
        remoto = { api().guardarUsuario(usuario) }
    )

    fun observarTramos(): Flow<List<TramoConNombres>> = tramoDao.observarTodosConNombres()

    /**
     * Crea el tramo, o una nueva versión si ya existe uno para el par (RF-ADM-04): la versión
     * anterior se cierra en la fecha de vigencia de la nueva y se conserva.
     */
    suspend fun guardarTramo(nuevo: Tramo, versionAnterior: Tramo?): Result<Unit> = escribir(
        local = {
            db.withTransaction {
                if (versionAnterior != null) {
                    tramoDao.actualizar(versionAnterior.copy(vigenteHasta = nuevo.vigenteDesde))
                }
                tramoDao.insertar(nuevo)
            }
        },
        remoto = { api().guardarTramo(nuevo) }
    )

    fun observarTarifas(): Flow<List<TarifaCombustible>> = tarifaDao.observarHistorial()

    suspend fun registrarTarifa(tarifa: TarifaCombustible): Result<Unit> = escribir(
        local = { tarifaDao.insertar(tarifa) },
        remoto = { api().registrarTarifa(tarifa) }
    )
}
