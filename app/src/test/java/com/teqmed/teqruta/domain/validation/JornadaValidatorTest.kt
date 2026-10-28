package com.teqmed.teqruta.domain.validation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JornadaValidatorTest {

    @Test
    fun `inicio valido sin registros previos del vehiculo`() {
        assertTrue(JornadaValidator.validarInicio(1L, "45200", null).esValido)
    }

    @Test
    fun `inicio exige vehiculo`() {
        val r = JornadaValidator.validarInicio(null, "45200", null)
        assertFalse(r.vehiculo.esValido)
        assertTrue(r.kmInicial.esValido)
    }

    @Test
    fun `km inicial no puede ser menor al ultimo registrado`() {
        assertFalse(JornadaValidator.validarInicio(1L, "45100", 45200).kmInicial.esValido)
        assertTrue(JornadaValidator.validarInicio(1L, "45200", 45200).kmInicial.esValido)
    }

    @Test
    fun `km inicial debe ser numerico`() {
        assertFalse(JornadaValidator.validarInicio(1L, "45.200,5", null).kmInicial.esValido)
        assertFalse(JornadaValidator.validarInicio(1L, "", null).kmInicial.esValido)
    }

    @Test
    fun `km de llegada debe superar al de salida`() {
        assertTrue(JornadaValidator.validarKmLlegada("45210", 45200).esValido)
        assertFalse(JornadaValidator.validarKmLlegada("45200", 45200).esValido)
        assertFalse(JornadaValidator.validarKmLlegada("45190", 45200).esValido)
        assertFalse(JornadaValidator.validarKmLlegada("abc", 45200).esValido)
    }
}
