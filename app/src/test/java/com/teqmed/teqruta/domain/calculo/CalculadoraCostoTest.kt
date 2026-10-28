package com.teqmed.teqruta.domain.calculo

import com.teqmed.teqruta.domain.model.MarcaTraslado
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CalculadoraCostoTest {

    private val delta = 1e-6

    @Test
    fun `caso normal usa km oficiales y suma el peaje`() {
        // 105 km / 10.5 km/L = 10 L × $1.300 = $13.000 + $3.200 de peaje
        val r = CalculadoraCosto.calcular(
            kmDeclarados = 106, kmOficial = 105.0, peaje = 3200,
            rendimientoKmL = 10.5, precioLitro = 1300.0
        )
        assertEquals(105.0, r.kmCobrados, delta)
        assertEquals(16_200.0, r.costo, delta)
        assertEquals(MarcaTraslado.OK, r.marca)
    }

    @Test
    fun `con desviacion se cobran igual los km oficiales`() {
        val r = CalculadoraCosto.calcular(
            kmDeclarados = 150, kmOficial = 100.0, peaje = 0,
            rendimientoKmL = 10.0, precioLitro = 1000.0
        )
        assertEquals(100.0, r.kmCobrados, delta)
        assertEquals(10_000.0, r.costo, delta)
        assertEquals(0.5, r.desviacionPct!!, delta)
        assertEquals(MarcaTraslado.DESVIACION, r.marca)
    }

    @Test
    fun `no catalogado usa km declarados y no cobra peaje`() {
        val r = CalculadoraCosto.calcular(
            kmDeclarados = 40, kmOficial = null, peaje = 3200,
            rendimientoKmL = 10.0, precioLitro = 1000.0
        )
        assertEquals(40.0, r.kmCobrados, delta)
        assertEquals(4_000.0, r.costo, delta)
        assertNull(r.desviacionPct)
        assertEquals(MarcaTraslado.NO_CATALOGADO, r.marca)
    }

    @Test
    fun `peaje cero solo cobra combustible`() {
        val r = CalculadoraCosto.calcular(
            kmDeclarados = 2, kmOficial = 2.1, peaje = 0,
            rendimientoKmL = 10.5, precioLitro = 1290.0
        )
        assertEquals(2.1 / 10.5 * 1290.0, r.costo, delta)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rendimiento cero es invalido`() {
        CalculadoraCosto.calcular(10, 10.0, 0, rendimientoKmL = 0.0, precioLitro = 1000.0)
    }
}
