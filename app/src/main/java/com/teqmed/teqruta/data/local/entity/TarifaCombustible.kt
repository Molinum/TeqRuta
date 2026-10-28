package com.teqmed.teqruta.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tarifas_combustible")
data class TarifaCombustible(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Precio del litro en pesos chilenos. */
    val precioLitro: Double,
    val vigenteDesde: Long
)
