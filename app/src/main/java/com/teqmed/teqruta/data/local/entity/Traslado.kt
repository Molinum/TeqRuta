package com.teqmed.teqruta.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID
import com.teqmed.teqruta.domain.model.MarcaTraslado
import com.teqmed.teqruta.domain.model.MotivoDesvio

/**
 * Un tramo recorrido dentro de una jornada. Se crea al registrar la salida y se completa
 * al registrar la llegada. Los campos "Usado" guardan una copia de los valores con que se
 * calculó el costo, para que un cambio posterior de tarifas no altere el registro.
 */
@Entity(
    tableName = "traslados",
    foreignKeys = [
        ForeignKey(
            entity = Jornada::class,
            parentColumns = ["id"],
            childColumns = ["jornadaId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(entity = Destino::class, parentColumns = ["id"], childColumns = ["origenId"]),
        ForeignKey(entity = Destino::class, parentColumns = ["id"], childColumns = ["destinoId"])
    ],
    indices = [
        Index(value = ["jornadaId"]),
        Index(value = ["origenId"]),
        Index(value = ["destinoId"]),
        Index(value = ["tramoId"]),
        Index(value = ["uuid"], unique = true)
    ]
)
data class Traslado(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val uuid: String = UUID.randomUUID().toString(),
    val jornadaId: Long,
    val orden: Int,
    val origenId: Long,
    val destinoId: Long,
    /** Tramo oficial usado; null si el traslado no está catalogado. */
    val tramoId: Long? = null,
    val horaSalida: Long,
    val horaLlegada: Long? = null,
    val kmInicio: Int,
    val kmFin: Int? = null,
    val latSalida: Double? = null,
    val lonSalida: Double? = null,
    val latLlegada: Double? = null,
    val lonLlegada: Double? = null,
    val fotoUri: String? = null,
    val kmOficialUsado: Double? = null,
    val peajeUsado: Int? = null,
    val precioLitroUsado: Double? = null,
    val rendimientoUsado: Double? = null,
    val costo: Double? = null,
    val desviacionPct: Double? = null,
    val marca: MarcaTraslado? = null,
    /** Explicación del técnico; solo se pide si el traslado tiene desviación o no está catalogado. */
    val motivoTipo: MotivoDesvio? = null,
    val motivoDetalle: String? = null
)
