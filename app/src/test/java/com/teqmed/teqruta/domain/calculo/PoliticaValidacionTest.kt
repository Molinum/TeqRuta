package com.teqmed.teqruta.domain.calculo

import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.domain.model.MarcaTraslado
import com.teqmed.teqruta.domain.model.MotivoDesvio
import com.teqmed.teqruta.domain.validation.JornadaValidator
import com.teqmed.teqruta.domain.validation.esValido
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaValidacionTest {

    @Test
    fun `solo desviacion y no catalogado exigen motivo`() {
        assertFalse(PoliticaValidacion.requiereMotivo(MarcaTraslado.OK))
        assertTrue(PoliticaValidacion.requiereMotivo(MarcaTraslado.DESVIACION))
        assertTrue(PoliticaValidacion.requiereMotivo(MarcaTraslado.NO_CATALOGADO))
    }

    @Test
    fun `jornada sin alertas queda validada al cerrarse`() {
        val marcas = listOf(MarcaTraslado.OK, MarcaTraslado.OK)
        assertEquals(EstadoJornada.VALIDADA, PoliticaValidacion.estadoAlCerrar(marcas))
    }

    @Test
    fun `un solo traslado con alerta deja la jornada para el jefe`() {
        assertEquals(
            EstadoJornada.CERRADA,
            PoliticaValidacion.estadoAlCerrar(listOf(MarcaTraslado.OK, MarcaTraslado.DESVIACION))
        )
        assertEquals(
            EstadoJornada.CERRADA,
            PoliticaValidacion.estadoAlCerrar(listOf(MarcaTraslado.NO_CATALOGADO, MarcaTraslado.OK))
        )
    }

    @Test
    fun `un traslado sin calcular no se da por valido`() {
        assertEquals(EstadoJornada.CERRADA, PoliticaValidacion.estadoAlCerrar(listOf(MarcaTraslado.OK, null)))
    }

    @Test
    fun `el motivo es obligatorio y otro exige descripcion`() {
        assertFalse(JornadaValidator.validarMotivo(null, "").esValido)
        assertTrue(JornadaValidator.validarMotivo(MotivoDesvio.RUTA_ALTERNATIVA, "").esValido)
        assertFalse(JornadaValidator.validarMotivo(MotivoDesvio.OTRO, "").esValido)
        assertFalse(JornadaValidator.validarMotivo(MotivoDesvio.OTRO, "  ab ").esValido)
        assertTrue(JornadaValidator.validarMotivo(MotivoDesvio.OTRO, "Corte de ruta").esValido)
    }
}
