package com.teqmed.teqruta.domain.calculo

import com.teqmed.teqruta.domain.model.MarcaTraslado
import org.junit.Assert.assertEquals
import org.junit.Test

class DetectorDesviacionTest {

    @Test
    fun `km iguales a los oficiales es OK`() {
        assertEquals(MarcaTraslado.OK, DetectorDesviacion.marcar(108, 108.0))
    }

    @Test
    fun `justo en el umbral de 15 por ciento es OK`() {
        assertEquals(MarcaTraslado.OK, DetectorDesviacion.marcar(115, 100.0))
    }

    @Test
    fun `sobre el umbral es desviacion`() {
        assertEquals(MarcaTraslado.DESVIACION, DetectorDesviacion.marcar(116, 100.0))
    }

    @Test
    fun `menos km que los oficiales no es desviacion`() {
        assertEquals(MarcaTraslado.OK, DetectorDesviacion.marcar(80, 100.0))
    }

    @Test
    fun `tramo corto con un km de diferencia no es desviacion`() {
        // 3 km declarados sobre 2.1 oficiales es +43%, pero solo 0.9 km de diferencia
        assertEquals(MarcaTraslado.OK, DetectorDesviacion.marcar(3, 2.1))
    }

    @Test
    fun `tramo corto con diferencia mayor a la tolerancia es desviacion`() {
        assertEquals(MarcaTraslado.DESVIACION, DetectorDesviacion.marcar(5, 2.1))
    }

    @Test
    fun `sin tramo oficial es no catalogado`() {
        assertEquals(MarcaTraslado.NO_CATALOGADO, DetectorDesviacion.marcar(50, null))
    }

    @Test
    fun `porcentaje se calcula sobre los km oficiales`() {
        assertEquals(0.25, DetectorDesviacion.porcentaje(125, 100.0), 1e-9)
        assertEquals(-0.1, DetectorDesviacion.porcentaje(90, 100.0), 1e-9)
    }
}
