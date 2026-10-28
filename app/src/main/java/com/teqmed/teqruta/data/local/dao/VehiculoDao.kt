package com.teqmed.teqruta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.teqmed.teqruta.data.local.entity.Vehiculo
import kotlinx.coroutines.flow.Flow

@Dao
interface VehiculoDao {
    @Insert
    suspend fun insertar(vehiculo: Vehiculo): Long

    @Update
    suspend fun actualizar(vehiculo: Vehiculo)

    @Query("SELECT * FROM vehiculos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Vehiculo?

    @Query("SELECT * FROM vehiculos WHERE activo = 1 ORDER BY descripcion")
    fun observarActivos(): Flow<List<Vehiculo>>

    @Query("SELECT * FROM vehiculos ORDER BY descripcion")
    fun observarTodos(): Flow<List<Vehiculo>>
}
