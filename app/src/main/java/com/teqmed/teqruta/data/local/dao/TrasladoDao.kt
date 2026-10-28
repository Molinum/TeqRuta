package com.teqmed.teqruta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.teqmed.teqruta.data.local.entity.Traslado
import kotlinx.coroutines.flow.Flow

@Dao
interface TrasladoDao {
    @Insert
    suspend fun insertar(traslado: Traslado): Long

    @Update
    suspend fun actualizar(traslado: Traslado)

    @Query("SELECT * FROM traslados WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Traslado?

    @Query("SELECT * FROM traslados WHERE jornadaId = :jornadaId ORDER BY orden")
    fun observarPorJornada(jornadaId: Long): Flow<List<Traslado>>

    @Query("SELECT * FROM traslados WHERE jornadaId = :jornadaId ORDER BY orden DESC LIMIT 1")
    suspend fun obtenerUltimo(jornadaId: Long): Traslado?
}
