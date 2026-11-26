package com.teqmed.teqruta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.teqmed.teqruta.data.local.entity.Traslado
import com.teqmed.teqruta.data.local.entity.TrasladoConNombres
import com.teqmed.teqruta.data.local.entity.TrasladoReporte
import kotlinx.coroutines.flow.Flow

@Dao
interface TrasladoDao {
    @Insert
    suspend fun insertar(traslado: Traslado): Long

    @Insert
    suspend fun insertarTodos(traslados: List<Traslado>)

    @Update
    suspend fun actualizar(traslado: Traslado)

    @Query("DELETE FROM traslados WHERE jornadaId = :jornadaId")
    suspend fun eliminarPorJornada(jornadaId: Long)

    @Query("SELECT * FROM traslados WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Traslado?

    @Query("SELECT * FROM traslados WHERE jornadaId = :jornadaId ORDER BY orden")
    fun observarPorJornada(jornadaId: Long): Flow<List<Traslado>>

    @Query("SELECT * FROM traslados WHERE jornadaId = :jornadaId ORDER BY orden")
    suspend fun obtenerPorJornada(jornadaId: Long): List<Traslado>

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

    /** Traslados completados de jornadas validadas cuya salida cae en [desde, hasta) (RN-12). */
    @Query(
        """
        SELECT t.id AS trasladoId, t.horaSalida AS horaSalida,
               o.id AS origenId, o.nombre AS origenNombre,
               d.id AS destinoId, d.nombre AS destinoNombre, d.tipo AS destinoTipo,
               u.id AS tecnicoId, u.nombre AS tecnicoNombre,
               v.id AS vehiculoId, v.descripcion AS vehiculoDescripcion, v.patente AS vehiculoPatente,
               (t.kmFin - t.kmInicio) AS kmDeclarados, COALESCE(t.costo, 0) AS costo, t.marca AS marca
        FROM traslados t
        JOIN jornadas j ON j.id = t.jornadaId
        JOIN usuarios u ON u.id = j.tecnicoId
        JOIN vehiculos v ON v.id = j.vehiculoId
        JOIN destinos o ON o.id = t.origenId
        JOIN destinos d ON d.id = t.destinoId
        WHERE j.estado = 'VALIDADA' AND t.horaLlegada IS NOT NULL
          AND t.horaSalida >= :desde AND t.horaSalida < :hasta
        ORDER BY t.horaSalida
        """
    )
    fun observarReporte(desde: Long, hasta: Long): Flow<List<TrasladoReporte>>
}
