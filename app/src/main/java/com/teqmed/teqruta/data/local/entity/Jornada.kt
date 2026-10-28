package com.teqmed.teqruta.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.teqmed.teqruta.domain.model.EstadoJornada

@Entity(
    tableName = "jornadas",
    foreignKeys = [
        ForeignKey(entity = Usuario::class, parentColumns = ["id"], childColumns = ["tecnicoId"]),
        ForeignKey(entity = Vehiculo::class, parentColumns = ["id"], childColumns = ["vehiculoId"])
    ],
    indices = [Index(value = ["tecnicoId"]), Index(value = ["vehiculoId"])]
)
data class Jornada(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val tecnicoId: Long,
    val vehiculoId: Long,
    val inicio: Long,
    val fin: Long? = null,
    val kmInicial: Int,
    val kmFinal: Int? = null,
    val fotoInicioUri: String? = null,
    val fotoCierreUri: String? = null,
    val latInicio: Double? = null,
    val lonInicio: Double? = null,
    val estado: EstadoJornada = EstadoJornada.ABIERTA,
    val comentarioJefe: String? = null
)
