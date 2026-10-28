package com.teqmed.teqruta.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun `requerido rechaza texto vacio o con espacios`() {
        assertFalse(Validators.requerido("", "Nombre").esValido)
        assertFalse(Validators.requerido("   ", "Nombre").esValido)
        assertTrue(Validators.requerido("Ana", "Nombre").esValido)
        assertEquals(Validators.CAMPO_OBLIGATORIO, Validators.requerido("", "Nombre").mensajeError)
    }

    @Test
    fun `correo valida el formato`() {
        assertTrue(Validators.correo("tecnico@teqmed.cl").esValido)
        assertTrue(Validators.correo("  tecnico@teqmed.cl ").esValido)
        assertFalse(Validators.correo("tecnico@").esValido)
        assertFalse(Validators.correo("tecnico.teqmed.cl").esValido)
        assertFalse(Validators.correo("").esValido)
    }

    @Test
    fun `entero valida numero y minimo`() {
        assertTrue(Validators.entero("150", "Km").esValido)
        assertFalse(Validators.entero("15.5", "Km").esValido)
        assertFalse(Validators.entero("abc", "Km").esValido)
        assertTrue(Validators.entero("100", "Km", minimo = 100).esValido)
        assertFalse(Validators.entero("99", "Km", minimo = 100).esValido)
    }

    @Test
    fun `decimal positivo acepta coma y punto`() {
        assertTrue(Validators.decimalPositivo("10,5", "Rendimiento").esValido)
        assertTrue(Validators.decimalPositivo("10.5", "Rendimiento").esValido)
        assertFalse(Validators.decimalPositivo("0", "Rendimiento").esValido)
        assertFalse(Validators.decimalPositivo("-3", "Rendimiento").esValido)
    }

    @Test
    fun `longitud minima`() {
        assertFalse(Validators.longitudMinima("abc", "Contraseña", 6).esValido)
        assertTrue(Validators.longitudMinima("abcdef", "Contraseña", 6).esValido)
    }

    @Test
    fun `patente acepta formato antiguo y nuevo`() {
        assertTrue(Validators.patente("AB1234").esValido)
        assertTrue(Validators.patente("abcd12").esValido)
        assertTrue(Validators.patente("AB-CD-12").esValido)
        assertFalse(Validators.patente("A12345").esValido)
        assertFalse(Validators.patente("ABCDE1").esValido)
        assertEquals("ABCD12", Validators.normalizarPatente("ab-cd·12"))
    }

    @Test
    fun `seleccion exige un valor`() {
        assertFalse(Validators.seleccion(null, "un vehículo").esValido)
        assertTrue(Validators.seleccion(1L, "un vehículo").esValido)
    }

    @Test
    fun `primer error devuelve el primero invalido`() {
        val r = Validators.primerError(
            ValidationResult.Valid,
            ValidationResult.Invalid("uno"),
            ValidationResult.Invalid("dos")
        )
        assertEquals("uno", r.mensajeError)
        assertTrue(Validators.primerError(ValidationResult.Valid).esValido)
    }
}
