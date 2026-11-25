package com.teqmed.teqruta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.teqmed.teqruta.data.local.entity.Tramo
import com.teqmed.teqruta.data.local.entity.TramoConNombres
import kotlinx.coroutines.flow.Flow

@Dao
interface TramoDao {
    /** Inserta o actualiza por id (sin borrar la fila, para no romper las claves foráneas). */
    @Upsert
    suspend fun upsertTodos(items: List<Tramo>)

    @Insert
    suspend fun insertar(tramo: Tramo): Long

    @Update
    suspend fun actualizar(tramo: Tramo)

    @Query("SELECT * FROM tramos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Tramo?

    /** Versión del tramo vigente en la fecha indicada (milisegundos). */
    @Query(
        """
        SELECT * FROM tramos
        WHERE origenId = :origenId AND destinoId = :destinoId
          AND vigenteDesde <= :fecha
          AND (vigenteHasta IS NULL OR vigenteHasta > :fecha)
        ORDER BY vigenteDesde DESC
        LIMIT 1
        """
    )
    suspend fun obtenerVigente(origenId: Long, destinoId: Long, fecha: Long): Tramo?

    /** Todas las versiones de todos los tramos, con los nombres de origen y destino. */
    @Query(
        """
        SELECT t.*, o.nombre AS origenNombre, d.nombre AS destinoNombre
        FROM tramos t
        JOIN destinos o ON o.id = t.origenId
        JOIN destinos d ON d.id = t.destinoId
        ORDER BY o.nombre, d.nombre, t.vigenteDesde DESC, t.id DESC
        """
    )
    fun observarTodosConNombres(): Flow<List<TramoConNombres>>
}
