package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.dao.TrasladoDao
import com.teqmed.teqruta.domain.model.MarcaTraslado
import com.teqmed.teqruta.domain.model.TipoDestino
import com.teqmed.teqruta.domain.reporte.LineaCosto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Datos para los reportes de costo de gerencia. Solo considera jornadas validadas (RN-12). */
class ReporteRepository(private val trasladoDao: TrasladoDao) {

    /**
     * Traslados con salida en [desde, hasta). El traslado de regreso a la sede se atribuye al
     * último cliente visitado, de modo que la suma de los clientes sea el costo total.
     */
    fun observarLineas(desde: Long, hasta: Long): Flow<List<LineaCosto>> =
        trasladoDao.observarReporte(desde, hasta).map { filas ->
            filas.map {
                val esRegreso = it.destinoTipo == TipoDestino.SEDE
                LineaCosto(
                    trasladoId = it.trasladoId,
                    fecha = it.horaSalida,
                    clienteId = if (esRegreso) it.origenId else it.destinoId,
                    clienteNombre = if (esRegreso) it.origenNombre else it.destinoNombre,
                    tecnicoId = it.tecnicoId,
                    tecnicoNombre = it.tecnicoNombre,
                    vehiculoId = it.vehiculoId,
                    vehiculoNombre = "${it.vehiculoDescripcion} · ${it.vehiculoPatente}",
                    kmDeclarados = it.kmDeclarados,
                    costo = it.costo,
                    conDesviacion = it.marca == MarcaTraslado.DESVIACION
                )
            }
        }
}
