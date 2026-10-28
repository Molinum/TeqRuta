package com.teqmed.teqruta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.teqmed.teqruta.data.local.entity.Destino
import kotlinx.coroutines.flow.Flow

@Dao
interface DestinoDao {
    @Insert
    suspend fun insertar(destino: Destino): Long

    @Update
    suspend fun actualizar(destino: Destino)

    @Query("SELECT * FROM destinos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Destino?

    @Query("SELECT * FROM destinos WHERE tipo = 'SEDE' AND activo = 1 LIMIT 1")
    suspend fun obtenerSede(): Destino?

    @Query("SELECT * FROM destinos WHERE activo = 1 ORDER BY ciudad, nombre")
    fun observarActivos(): Flow<List<Destino>>

    @Query("SELECT * FROM destinos ORDER BY ciudad, nombre")
    fun observarTodos(): Flow<List<Destino>>
}
