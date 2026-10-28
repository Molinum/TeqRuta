package com.teqmed.teqruta.data.local.entity

import androidx.room.Embedded
import com.teqmed.teqruta.domain.model.TipoDestino

/** Resultado de consulta: un traslado con los nombres de su origen y destino. */
data class TrasladoConNombres(
    @Embedded val traslado: Traslado,
    val origenNombre: String,
    val destinoNombre: String,
    val destinoTipo: TipoDestino
)
