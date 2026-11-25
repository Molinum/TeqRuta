package com.teqmed.teqruta.domain.reporte

/** Un traslado visto desde los reportes de gerencia (RF-GER-01 a RF-GER-06). */
data class LineaCosto(
    val trasladoId: Long,
    val fecha: Long,
    val clienteId: Long,
    val clienteNombre: String,
    val tecnicoId: Long,
    val tecnicoNombre: String,
    val vehiculoId: Long,
    val vehiculoNombre: String,
    val kmDeclarados: Int,
    val costo: Double,
    /** Traslado con desviación (RF-GER-06). */
    val conDesviacion: Boolean
)

/** Costo acumulado de un cliente, técnico o vehículo en el período. */
data class FilaReporte(
    val clave: Long,
    val nombre: String,
    val traslados: Int,
    val km: Int,
    val costo: Double,
    val conDesviacion: Int,
    /** Traslados que componen la fila, en orden cronológico. */
    val lineas: List<LineaCosto>
) {
    /** Porcentaje (0 a 1) de traslados con desviación. */
    val porcentajeDesviacion: Double get() = if (traslados == 0) 0.0 else conDesviacion.toDouble() / traslados
}

enum class AgrupadoPor(val etiqueta: String) {
    CLIENTE("Cliente"),
    TECNICO("Técnico"),
    VEHICULO("Vehículo")
}

/** Agrupa y totaliza los traslados de un período. Lógica pura, sin dependencias de Android. */
object ReporteCostos {

    fun agrupar(lineas: List<LineaCosto>, criterio: AgrupadoPor): List<FilaReporte> {
        val clave: (LineaCosto) -> Pair<Long, String> = when (criterio) {
            AgrupadoPor.CLIENTE -> { l -> l.clienteId to l.clienteNombre }
            AgrupadoPor.TECNICO -> { l -> l.tecnicoId to l.tecnicoNombre }
            AgrupadoPor.VEHICULO -> { l -> l.vehiculoId to l.vehiculoNombre }
        }
        return lineas
            .groupBy(clave)
            .map { (grupo, items) ->
                FilaReporte(
                    clave = grupo.first,
                    nombre = grupo.second,
                    traslados = items.size,
                    km = items.sumOf { it.kmDeclarados },
                    costo = items.sumOf { it.costo },
                    conDesviacion = items.count { it.conDesviacion },
                    lineas = items.sortedBy { it.fecha }
                )
            }
            .sortedByDescending { it.costo }
    }

    fun costoTotal(lineas: List<LineaCosto>): Double = lineas.sumOf { it.costo }
}
