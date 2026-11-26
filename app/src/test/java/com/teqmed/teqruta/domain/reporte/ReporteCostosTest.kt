package com.teqmed.teqruta.domain.reporte

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReporteCostosTest {

    private fun linea(
        id: Long, cliente: Long, tecnico: Long, vehiculo: Long, km: Int, costo: Double,
        desviado: Boolean = false, fecha: Long = id
    ) = LineaCosto(
        trasladoId = id, fecha = fecha,
        clienteId = cliente, clienteNombre = "Cliente $cliente",
        tecnicoId = tecnico, tecnicoNombre = "Técnico $tecnico",
        vehiculoId = vehiculo, vehiculoNombre = "Vehículo $vehiculo",
        kmDeclarados = km, costo = costo, conDesviacion = desviado
    )

    private val lineas = listOf(
        linea(1, cliente = 1, tecnico = 1, vehiculo = 1, km = 10, costo = 1_000.0),
        linea(2, cliente = 1, tecnico = 2, vehiculo = 2, km = 20, costo = 2_500.0, desviado = true),
        linea(3, cliente = 2, tecnico = 1, vehiculo = 1, km = 5, costo = 500.0)
    )

    @Test
    fun `agrupa por cliente sumando costo y km, del mayor costo al menor`() {
        val filas = ReporteCostos.agrupar(lineas, AgrupadoPor.CLIENTE)
        assertEquals(listOf(1L, 2L), filas.map { it.clave })
        assertEquals(3_500.0, filas[0].costo, 0.001)
        assertEquals(30, filas[0].km)
        assertEquals(2, filas[0].traslados)
    }

    @Test
    fun `agrupa por tecnico y calcula el porcentaje de desviacion`() {
        val filas = ReporteCostos.agrupar(lineas, AgrupadoPor.TECNICO).associateBy { it.clave }
        assertEquals(0.0, filas.getValue(1L).porcentajeDesviacion, 0.001)
        assertEquals(1, filas.getValue(2L).conDesviacion)
        assertEquals(1.0, filas.getValue(2L).porcentajeDesviacion, 0.001)
    }

    @Test
    fun `la suma de las filas de cualquier agrupacion es el costo total`() {
        val total = ReporteCostos.costoTotal(lineas)
        AgrupadoPor.entries.forEach { criterio ->
            assertEquals(total, ReporteCostos.agrupar(lineas, criterio).sumOf { it.costo }, 0.001)
        }
    }

    @Test
    fun `las lineas de cada fila quedan en orden cronologico`() {
        val desordenadas = listOf(
            linea(1, 1, 1, 1, 1, 1.0, fecha = 300),
            linea(2, 1, 1, 1, 1, 1.0, fecha = 100)
        )
        val fila = ReporteCostos.agrupar(desordenadas, AgrupadoPor.CLIENTE).single()
        assertEquals(listOf(2L, 1L), fila.lineas.map { it.trasladoId })
    }

    @Test
    fun `sin lineas el reporte esta vacio y el porcentaje es cero`() {
        assertTrue(ReporteCostos.agrupar(emptyList(), AgrupadoPor.CLIENTE).isEmpty())
        assertEquals(0.0, FilaReporte(1, "x", 0, 0, 0.0, 0, emptyList()).porcentajeDesviacion, 0.0)
    }
}
