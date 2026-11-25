package com.teqmed.teqruta.data.local.entity

import androidx.room.Embedded

/** Resultado de consulta: una jornada con los datos de su técnico y vehículo y totales de sus traslados. */
data class JornadaResumen(
    @Embedded val jornada: Jornada,
    val tecnicoNombre: String,
    /** Jefe a cargo del técnico (RN-15). */
    val jefeId: Long?,
    val vehiculoDescripcion: String,
    val vehiculoPatente: String,
    val totalTraslados: Int,
    /** Traslados con desviación o no catalogados. */
    val conAlerta: Int,
    val costoTotal: Double
) {
    val tieneAlerta get() = conAlerta > 0
}
