package com.teqmed.teqruta.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.teqmed.teqruta.domain.model.TipoDestino

@Entity(tableName = "destinos")
data class Destino(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nombre: String,
    val direccion: String,
    val ciudad: String,
    val lat: Double,
    val lon: Double,
    val tipo: TipoDestino,
    val activo: Boolean = true
)
