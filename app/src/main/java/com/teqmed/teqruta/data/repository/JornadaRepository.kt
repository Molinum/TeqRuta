package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.dao.JornadaDao
import com.teqmed.teqruta.data.local.dao.TrasladoDao
import com.teqmed.teqruta.data.local.entity.Jornada
import com.teqmed.teqruta.data.local.entity.Traslado
import kotlinx.coroutines.flow.Flow

/** Error de negocio con un mensaje apto para mostrar al usuario. */
class ReglaNegocioException(mensaje: String) : Exception(mensaje)

/** Jornadas de los técnicos y los traslados que las componen. */
class JornadaRepository(
    private val jornadaDao: JornadaDao,
    private val trasladoDao: TrasladoDao
) {
    fun observarJornadaAbierta(tecnicoId: Long): Flow<Jornada?> = jornadaDao.observarAbierta(tecnicoId)

    fun observarTraslados(jornadaId: Long): Flow<List<Traslado>> = trasladoDao.observarPorJornada(jornadaId)

    suspend fun obtenerJornada(id: Long): Jornada? = jornadaDao.obtenerPorId(id)

    suspend fun obtenerTraslado(id: Long): Traslado? = trasladoDao.obtenerPorId(id)

    suspend fun obtenerUltimoKmVehiculo(vehiculoId: Long): Int? = jornadaDao.obtenerUltimoKmFinal(vehiculoId)

    /**
     * Abre una jornada para el técnico (RF-TEC-01). Falla si el técnico ya tiene una jornada
     * abierta o si el vehículo está en uso en otra (RN-06).
     */
    suspend fun iniciarJornada(tecnicoId: Long, vehiculoId: Long, kmInicial: Int): Result<Long> {
        if (jornadaDao.obtenerAbierta(tecnicoId) != null) {
            return Result.failure(ReglaNegocioException("Ya tienes una jornada abierta"))
        }
        if (jornadaDao.obtenerAbiertaPorVehiculo(vehiculoId) != null) {
            return Result.failure(ReglaNegocioException("El vehículo está en uso en otra jornada abierta"))
        }
        val id = jornadaDao.insertar(
            Jornada(
                tecnicoId = tecnicoId,
                vehiculoId = vehiculoId,
                inicio = System.currentTimeMillis(),
                kmInicial = kmInicial
            )
        )
        return Result.success(id)
    }
}
