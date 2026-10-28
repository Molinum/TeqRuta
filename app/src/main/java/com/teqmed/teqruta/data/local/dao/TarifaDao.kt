package com.teqmed.teqruta.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.teqmed.teqruta.data.local.entity.TarifaCombustible
import kotlinx.coroutines.flow.Flow

@Dao
interface TarifaDao {
    @Insert
    suspend fun insertar(tarifa: TarifaCombustible): Long

    /** Tarifa vigente en la fecha indicada: la más reciente que ya había empezado a regir. */
    @Query(
        """
        SELECT * FROM tarifas_combustible
        WHERE vigenteDesde <= :fecha
        ORDER BY vigenteDesde DESC
        LIMIT 1
        """
    )
    suspend fun obtenerVigente(fecha: Long): TarifaCombustible?

    @Query("SELECT * FROM tarifas_combustible ORDER BY vigenteDesde DESC")
    fun observarHistorial(): Flow<List<TarifaCombustible>>
}
