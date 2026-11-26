package com.teqmed.teqruta.data.remote

import com.teqmed.teqruta.data.local.entity.Jornada
import com.teqmed.teqruta.data.local.entity.Traslado

fun Jornada.aDto(traslados: List<Traslado>) = JornadaDto(
    uuid = uuid, tecnicoId = tecnicoId, vehiculoId = vehiculoId, inicio = inicio, fin = fin,
    kmInicial = kmInicial, kmFinal = kmFinal, fotoInicioUri = fotoInicioUri, fotoCierreUri = fotoCierreUri,
    latInicio = latInicio, lonInicio = lonInicio, estado = estado, comentarioJefe = comentarioJefe,
    validacionAutomatica = validacionAutomatica, actualizadoEn = actualizadoEn,
    traslados = traslados.map { it.aDto() }
)

fun Traslado.aDto() = TrasladoDto(
    uuid = uuid, orden = orden, origenId = origenId, destinoId = destinoId, tramoId = tramoId,
    horaSalida = horaSalida, horaLlegada = horaLlegada, kmInicio = kmInicio, kmFin = kmFin,
    latSalida = latSalida, lonSalida = lonSalida, latLlegada = latLlegada, lonLlegada = lonLlegada,
    fotoUri = fotoUri, kmOficialUsado = kmOficialUsado, peajeUsado = peajeUsado,
    precioLitroUsado = precioLitroUsado, rendimientoUsado = rendimientoUsado, costo = costo,
    desviacionPct = desviacionPct, marca = marca, motivoTipo = motivoTipo, motivoDetalle = motivoDetalle
)

/** [id] es el id local de la jornada si ya existe en el dispositivo, o 0 para que Room asigne uno. */
fun JornadaDto.aEntidad(id: Long) = Jornada(
    id = id, uuid = uuid, tecnicoId = tecnicoId, vehiculoId = vehiculoId, inicio = inicio, fin = fin,
    kmInicial = kmInicial, kmFinal = kmFinal, fotoInicioUri = fotoInicioUri, fotoCierreUri = fotoCierreUri,
    latInicio = latInicio, lonInicio = lonInicio, estado = estado, comentarioJefe = comentarioJefe,
    validacionAutomatica = validacionAutomatica,
    actualizadoEn = actualizadoEn ?: System.currentTimeMillis(),
    pendienteSync = false
)

fun TrasladoDto.aEntidad(jornadaId: Long) = Traslado(
    uuid = uuid, jornadaId = jornadaId, orden = orden, origenId = origenId, destinoId = destinoId,
    tramoId = tramoId, horaSalida = horaSalida, horaLlegada = horaLlegada, kmInicio = kmInicio,
    kmFin = kmFin, latSalida = latSalida, lonSalida = lonSalida, latLlegada = latLlegada,
    lonLlegada = lonLlegada, fotoUri = fotoUri, kmOficialUsado = kmOficialUsado, peajeUsado = peajeUsado,
    precioLitroUsado = precioLitroUsado, rendimientoUsado = rendimientoUsado, costo = costo,
    desviacionPct = desviacionPct, marca = marca, motivoTipo = motivoTipo, motivoDetalle = motivoDetalle
)
