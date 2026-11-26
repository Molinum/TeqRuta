package com.teqmed.teqruta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.teqmed.teqruta.data.local.entity.Jornada
import com.teqmed.teqruta.data.local.entity.JornadaResumen
import kotlinx.coroutines.flow.Flow

private const val SELECT_RESUMEN = """
    SELECT j.*, u.nombre AS tecnicoNombre, u.jefeId AS jefeId,
           v.descripcion AS vehiculoDescripcion, v.patente AS vehiculoPatente,
           (SELECT COUNT(*) FROM traslados t WHERE t.jornadaId = j.id) AS totalTraslados,
           (SELECT COUNT(*) FROM traslados t
             WHERE t.jornadaId = j.id AND t.marca IN ('DESVIACION', 'NO_CATALOGADO')) AS conAlerta,
           (SELECT COALESCE(SUM(t.costo), 0) FROM traslados t WHERE t.jornadaId = j.id) AS costoTotal
    FROM jornadas j
    JOIN usuarios u ON u.id = j.tecnicoId
    JOIN vehiculos v ON v.id = j.vehiculoId
"""

@Dao
interface JornadaDao {
    @Insert
    suspend fun insertar(jornada: Jornada): Long

    @Update
    suspend fun actualizar(jornada: Jornada)

    @Query("SELECT * FROM jornadas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Jornada?

    @Query("SELECT * FROM jornadas WHERE tecnicoId = :tecnicoId AND estado = 'ABIERTA' LIMIT 1")
    fun observarAbierta(tecnicoId: Long): Flow<Jornada?>

    @Query("SELECT * FROM jornadas WHERE tecnicoId = :tecnicoId AND estado = 'ABIERTA' LIMIT 1")
    suspend fun obtenerAbierta(tecnicoId: Long): Jornada?

    @Query("SELECT * FROM jornadas WHERE vehiculoId = :vehiculoId AND estado = 'ABIERTA' LIMIT 1")
    suspend fun obtenerAbiertaPorVehiculo(vehiculoId: Long): Jornada?

    /** Km final de la última jornada cerrada del vehículo (último odómetro conocido). */
    @Query(
        """
        SELECT kmFinal FROM jornadas
        WHERE vehiculoId = :vehiculoId AND kmFinal IS NOT NULL
        ORDER BY fin DESC
        LIMIT 1
        """
    )
    suspend fun obtenerUltimoKmFinal(vehiculoId: Long): Int?

    /** Jornadas ya cerradas del técnico (las más recientes primero). */
    @Query(SELECT_RESUMEN + " WHERE j.tecnicoId = :tecnicoId AND j.estado != 'ABIERTA' ORDER BY j.inicio DESC")
    fun observarResumenesDeTecnico(tecnicoId: Long): Flow<List<JornadaResumen>>

    /** Jornadas ya cerradas de los técnicos a cargo del jefe (las más recientes primero). */
    @Query(SELECT_RESUMEN + " WHERE u.jefeId = :jefeId AND j.estado != 'ABIERTA' ORDER BY j.inicio DESC")
    fun observarResumenesDeJefe(jefeId: Long): Flow<List<JornadaResumen>>

    @Query(SELECT_RESUMEN + " WHERE j.id = :id")
    fun observarResumen(id: Long): Flow<JornadaResumen?>

    @Query("SELECT * FROM jornadas WHERE pendienteSync = 1")
    suspend fun obtenerPendientes(): List<Jornada>

    @Query("SELECT * FROM jornadas WHERE uuid = :uuid LIMIT 1")
    suspend fun obtenerPorUuid(uuid: String): Jornada?

    /** Marca la jornada como enviada, salvo que haya cambiado mientras se enviaba. */
    @Query("UPDATE jornadas SET pendienteSync = 0 WHERE id = :id AND actualizadoEn = :actualizadoEn")
    suspend fun marcarSincronizada(id: Long, actualizadoEn: Long)

    @Query("UPDATE jornadas SET pendienteSync = 1, actualizadoEn = :ahora WHERE id = :id")
    suspend fun marcarPendiente(id: Long, ahora: Long)

    @Query(SELECT_RESUMEN + " WHERE j.id = :id")
    suspend fun obtenerResumen(id: Long): JornadaResumen?
}
