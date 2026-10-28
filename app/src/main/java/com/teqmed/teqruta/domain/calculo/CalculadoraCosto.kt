package com.teqmed.teqruta.domain.calculo

import com.teqmed.teqruta.domain.model.MarcaTraslado

data class ResultadoCosto(
    /** Km sobre los que se calculó el costo: los oficiales, o los declarados si no hay tramo. */
    val kmCobrados: Double,
    /** Costo total en pesos chilenos (combustible + peaje). */
    val costo: Double,
    /** Desviación relativa respecto de los km oficiales; null si no hay tramo oficial. */
    val desviacionPct: Double?,
    val marca: MarcaTraslado
)

/** Calcula el costo de un traslado (RN-01 a RN-03). Lógica pura, sin dependencias de Android. */
object CalculadoraCosto {

    /**
     * @param kmDeclarados km recorridos según el odómetro.
     * @param kmOficial km del tramo oficial vigente, o null si el traslado no está catalogado.
     * @param peaje peaje del tramo en CLP (se ignora si no hay tramo oficial).
     * @param rendimientoKmL rendimiento del vehículo en km por litro.
     * @param precioLitro precio vigente del combustible en CLP.
     */
    fun calcular(
        kmDeclarados: Int,
        kmOficial: Double?,
        peaje: Int,
        rendimientoKmL: Double,
        precioLitro: Double
    ): ResultadoCosto {
        require(kmDeclarados >= 0) { "Los km declarados no pueden ser negativos" }
        require(rendimientoKmL > 0) { "El rendimiento debe ser mayor a 0" }
        require(precioLitro > 0) { "El precio del litro debe ser mayor a 0" }

        // Se pagan los km oficiales; sin tramo oficial se usan los declarados (RN-03)
        val kmCobrados = kmOficial ?: kmDeclarados.toDouble()
        val peajeAplicado = if (kmOficial == null) 0 else peaje
        val costo = kmCobrados / rendimientoKmL * precioLitro + peajeAplicado

        return ResultadoCosto(
            kmCobrados = kmCobrados,
            costo = costo,
            desviacionPct = kmOficial?.let { DetectorDesviacion.porcentaje(kmDeclarados, it) },
            marca = DetectorDesviacion.marcar(kmDeclarados, kmOficial)
        )
    }
}
