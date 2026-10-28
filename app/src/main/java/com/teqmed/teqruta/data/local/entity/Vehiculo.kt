package com.teqmed.teqruta.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teqmed.teqruta.domain.model.TipoVehiculo

@Entity(
    tableName = "vehiculos",
    indices = [Index(value = ["patente"], unique = true)]
)
data class Vehiculo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val patente: String,
    val descripcion: String,
    val tipo: TipoVehiculo,
    /** Rendimiento en km por litro. */
    val rendimientoKmL: Double,
    val activo: Boolean = true
)
