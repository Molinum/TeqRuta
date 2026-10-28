package com.teqmed.teqruta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.teqmed.teqruta.data.local.entity.Jornada
import kotlinx.coroutines.flow.Flow

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
}
