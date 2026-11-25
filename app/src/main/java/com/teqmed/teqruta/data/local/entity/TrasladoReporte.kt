package com.teqmed.teqruta.data.local.entity

import com.teqmed.teqruta.domain.model.MarcaTraslado
import com.teqmed.teqruta.domain.model.TipoDestino

/** Resultado de consulta: un traslado completado de una jornada validada, con los nombres que usan los reportes. */
data class TrasladoReporte(
    val trasladoId: Long,
    val horaSalida: Long,
    val origenId: Long,
    val origenNombre: String,
    val destinoId: Long,
    val destinoNombre: String,
    val destinoTipo: TipoDestino,
    val tecnicoId: Long,
    val tecnicoNombre: String,
    val vehiculoId: Long,
    val vehiculoDescripcion: String,
    val vehiculoPatente: String,
    val kmDeclarados: Int,
    val costo: Double,
    val marca: MarcaTraslado?
)
