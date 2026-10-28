package com.teqmed.teqruta.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Distancia oficial entre dos destinos. Al editar un tramo se crea una nueva versión:
 * la anterior se cierra con [vigenteHasta] y los traslados antiguos siguen apuntando a ella.
 */
@Entity(
    tableName = "tramos",
    foreignKeys = [
        ForeignKey(entity = Destino::class, parentColumns = ["id"], childColumns = ["origenId"]),
        ForeignKey(entity = Destino::class, parentColumns = ["id"], childColumns = ["destinoId"])
    ],
    indices = [Index(value = ["origenId", "destinoId"]), Index(value = ["destinoId"])]
)
data class Tramo(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val origenId: Long,
    val destinoId: Long,
    val kmOficial: Double,
    /** Peaje del tramo en pesos chilenos. */
    val peaje: Int,
    /** Fechas en milisegundos (epoch). */
    val vigenteDesde: Long,
    val vigenteHasta: Long? = null
)
