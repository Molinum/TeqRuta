package com.teqmed.teqruta.data.local.entity

import androidx.room.Embedded

/** Resultado de consulta: un tramo con los nombres de su origen y destino. */
data class TramoConNombres(
    @Embedded val tramo: Tramo,
    val origenNombre: String,
    val destinoNombre: String
)
