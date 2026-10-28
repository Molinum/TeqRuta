package com.teqmed.teqruta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.teqmed.teqruta.data.local.entity.Traslado
import com.teqmed.teqruta.data.local.entity.TrasladoConNombres
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

    @Query(
        """
        SELECT t.*, o.nombre AS origenNombre, d.nombre AS destinoNombre, d.tipo AS destinoTipo
        FROM traslados t
        JOIN destinos o ON o.id = t.origenId
        JOIN destinos d ON d.id = t.destinoId
        WHERE t.jornadaId = :jornadaId
        ORDER BY t.orden
        """
    )
    fun observarConNombres(jornadaId: Long): Flow<List<TrasladoConNombres>>

    @Query(
        """
        SELECT t.*, o.nombre AS origenNombre, d.nombre AS destinoNombre, d.tipo AS destinoTipo
        FROM traslados t
        JOIN destinos o ON o.id = t.origenId
        JOIN destinos d ON d.id = t.destinoId
        WHERE t.id = :id
        """
    )
    suspend fun obtenerConNombres(id: Long): TrasladoConNombres?
}
