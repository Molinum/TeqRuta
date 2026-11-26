package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.remote.DatosRemotos
import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.domain.model.MarcaTraslado
import com.teqmed.teqruta.domain.model.TipoDestino
import com.teqmed.teqruta.domain.reporte.LineaCosto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/** Datos para los reportes de costo de gerencia, desde la API. Solo considera jornadas validadas (RN-12). */
class ReporteRepository(private val datos: DatosRemotos) {

    /**
     * Traslados completados con salida en [desde, hasta). El traslado de regreso a la sede se
     * atribuye al último cliente visitado, de modo que la suma de los clientes sea el costo total.
     */
    fun observarLineas(desde: Long, hasta: Long): Flow<List<LineaCosto>> =
        combine(datos.observarJornadas(), datos.observarCatalogo()) { jornadas, cat ->
            jornadas.filter { it.jornada.estado == EstadoJornada.VALIDADA }.flatMap { j ->
                val tecnico = cat.usuarios.find { it.id == j.jornada.tecnicoId }
                val vehiculo = cat.vehiculos.find { it.id == j.jornada.vehiculoId }
                j.traslados
                    .filter { it.horaLlegada != null && it.kmFin != null && it.horaSalida in desde until hasta }
                    .mapNotNull { t ->
                        val origen = cat.destinos.find { it.id == t.origenId } ?: return@mapNotNull null
                        val destino = cat.destinos.find { it.id == t.destinoId } ?: return@mapNotNull null
                        val cliente = if (destino.tipo == TipoDestino.SEDE) origen else destino
                        LineaCosto(
                            trasladoId = t.id,
                            fecha = t.horaSalida,
                            clienteId = cliente.id,
                            clienteNombre = cliente.nombre,
                            tecnicoId = j.jornada.tecnicoId,
                            tecnicoNombre = tecnico?.nombre.orEmpty(),
                            vehiculoId = j.jornada.vehiculoId,
                            vehiculoNombre = vehiculo?.let { "${it.descripcion} · ${it.patente}" }.orEmpty(),
                            kmDeclarados = t.kmFin!! - t.kmInicio,
                            costo = t.costo ?: 0.0,
                            conDesviacion = t.marca == MarcaTraslado.DESVIACION
                        )
                    }
            }.sortedBy { it.fecha }
        }
}
