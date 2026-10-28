package com.teqmed.teqruta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.teqmed.teqruta.data.local.entity.Tramo

@Dao
interface TramoDao {
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
}
