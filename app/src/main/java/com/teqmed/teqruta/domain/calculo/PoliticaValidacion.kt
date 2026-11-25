package com.teqmed.teqruta.domain.calculo

import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.domain.model.MarcaTraslado

/**
 * Decide qué traslados exigen una explicación del técnico y qué jornadas requieren revisión
 * del jefe. Lo normal se acepta sin fricción; solo las excepciones consumen atención humana.
 * Lógica pura, sin dependencias de Android.
 */
object PoliticaValidacion {

    /**
     * Si es true, al cerrarse una jornada sin alertas queda Validada sin pasar por el jefe.
     * Poner en false para que el jefe valide todas las jornadas.
     */
    const val VALIDACION_AUTOMATICA = true

    /** Un traslado con desviación o sin catalogar exige que el técnico explique qué ocurrió. */
    fun requiereMotivo(marca: MarcaTraslado): Boolean = marca != MarcaTraslado.OK

    /** Estado con que queda una jornada al cerrarse, según las marcas de todos sus traslados. */
    fun estadoAlCerrar(marcas: List<MarcaTraslado?>): EstadoJornada =
        if (VALIDACION_AUTOMATICA && marcas.all { it == MarcaTraslado.OK }) EstadoJornada.VALIDADA
        else EstadoJornada.CERRADA
}
