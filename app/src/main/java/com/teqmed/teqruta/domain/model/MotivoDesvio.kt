package com.teqmed.teqruta.domain.model

/** Qué ocurrió en un traslado con desviación o sin catalogar, según el técnico. */
enum class MotivoDesvio(val etiqueta: String, val requiereDetalle: Boolean = false) {
    RUTA_ALTERNATIVA("Ruta alternativa o tráfico"),
    PARADA_ADICIONAL("Parada adicional"),
    ERROR_ODOMETRO("Error al leer el odómetro"),
    TRAYECTO_NUEVO("Trayecto o cliente nuevo"),
    OTRO("Otro", requiereDetalle = true)
}
