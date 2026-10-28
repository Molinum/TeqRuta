package com.teqmed.teqruta.domain.calculo

import com.teqmed.teqruta.domain.model.MarcaTraslado

/** Compara los km declarados por el técnico con los km oficiales del tramo (RN-02, RN-03). */
object DetectorDesviacion {

    /** Porcentaje sobre los km oficiales a partir del cual se marca desviación. */
    const val UMBRAL_DESVIACION = 0.15

    /**
     * Diferencia mínima en km para marcar desviación. El odómetro registra km enteros, así que
     * en tramos urbanos cortos (2–3 km) un solo km de diferencia superaría el 15%.
     */
    const val TOLERANCIA_MINIMA_KM = 2.0

    /** Desviación relativa: positiva si el técnico declaró más km que los oficiales. */
    fun porcentaje(kmDeclarados: Int, kmOficial: Double): Double =
        (kmDeclarados - kmOficial) / kmOficial

    fun marcar(kmDeclarados: Int, kmOficial: Double?): MarcaTraslado {
        if (kmOficial == null) return MarcaTraslado.NO_CATALOGADO
        val diferenciaKm = kmDeclarados - kmOficial
        return if (porcentaje(kmDeclarados, kmOficial) > UMBRAL_DESVIACION && diferenciaKm > TOLERANCIA_MINIMA_KM) {
            MarcaTraslado.DESVIACION
        } else {
            MarcaTraslado.OK
        }
    }
}
