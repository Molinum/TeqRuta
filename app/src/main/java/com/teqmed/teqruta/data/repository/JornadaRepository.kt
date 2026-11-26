package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.Jornada
import com.teqmed.teqruta.data.local.entity.JornadaResumen
import com.teqmed.teqruta.data.local.entity.Traslado
import com.teqmed.teqruta.data.local.entity.TrasladoConNombres
import com.teqmed.teqruta.data.remote.CatalogosDto
import com.teqmed.teqruta.data.remote.DatosRemotos
import com.teqmed.teqruta.data.remote.JornadaConTraslados
import com.teqmed.teqruta.data.remote.mensajeDeError
import com.teqmed.teqruta.domain.calculo.CalculadoraCosto
import com.teqmed.teqruta.domain.calculo.PoliticaValidacion
import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.domain.model.MarcaTraslado
import com.teqmed.teqruta.domain.model.MotivoDesvio
import com.teqmed.teqruta.domain.model.TipoDestino
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.coroutines.cancellation.CancellationException

/** Error de negocio con un mensaje apto para mostrar al usuario. */
class ReglaNegocioException(mensaje: String) : Exception(mensaje)

/** Ejecuta una escritura en la API y convierte los errores de red o del servidor en un mensaje para el usuario. */
internal suspend fun <T> llamarApi(bloque: suspend () -> T): Result<T> = try {
    Result.success(bloque())
} catch (e: CancellationException) {
    throw e
} catch (e: ReglaNegocioException) {
    Result.failure(e)
} catch (e: Exception) {
    Result.failure(ReglaNegocioException(mensajeDeError(e)))
}

/** Coordenadas GPS; null en los parámetros si no hay permiso o señal. */
data class Coordenadas(val lat: Double, val lon: Double)

/** Traslado ya calculado y, si la llegada cerró la jornada, el estado con que quedó. */
data class ResultadoLlegada(val traslado: Traslado, val estadoJornada: EstadoJornada?)

/**
 * Jornadas de los técnicos y los traslados que las componen, leídas y escritas a través de la API.
 * Cada cambio envía la jornada completa al servidor, que valida las reglas y recalcula los costos.
 */
class JornadaRepository(
    private val datos: DatosRemotos,
    private val catalogo: CatalogoRepository
) {
    private val alertas = setOf(MarcaTraslado.DESVIACION, MarcaTraslado.NO_CATALOGADO)

    fun observarJornadaAbierta(tecnicoId: Long): Flow<Jornada?> = datos.observarJornadas()
        .map { lista -> abiertaDe(lista, tecnicoId)?.jornada }
        .distinctUntilChanged()

    fun observarTraslados(jornadaId: Long): Flow<List<TrasladoConNombres>> =
        combine(datos.observarJornadas(), datos.observarCatalogo()) { lista, cat ->
            lista.find { it.jornada.id == jornadaId }?.traslados.orEmpty()
                .sortedBy { it.orden }
                .mapNotNull { conNombres(it, cat) }
        }

    suspend fun obtenerJornada(id: Long): Jornada? = datos.jornadas().find { it.jornada.id == id }?.jornada

    fun observarResumenesDeTecnico(tecnicoId: Long): Flow<List<JornadaResumen>> = observarResumenes {
        it.jornada.tecnicoId == tecnicoId
    }

    fun observarResumenesDeJefe(jefeId: Long): Flow<List<JornadaResumen>> = observarResumenes { it.jefeId == jefeId }

    fun observarResumen(jornadaId: Long): Flow<JornadaResumen?> =
        combine(datos.observarJornadas(), datos.observarCatalogo()) { lista, cat ->
            lista.find { it.jornada.id == jornadaId }?.let { resumen(it, cat) }
        }

    /** Jornadas ya cerradas, de la más reciente a la más antigua. */
    private fun observarResumenes(filtro: (JornadaResumen) -> Boolean): Flow<List<JornadaResumen>> =
        combine(datos.observarJornadas(), datos.observarCatalogo()) { lista, cat ->
            lista.filter { it.jornada.estado != EstadoJornada.ABIERTA }
                .mapNotNull { resumen(it, cat) }
                .filter(filtro)
                .sortedByDescending { it.jornada.inicio }
        }

    /** Pasa una jornada Cerrada a Validada (RF-JEF-04). Solo el jefe del técnico puede hacerlo (RN-15). */
    suspend fun validar(jornadaId: Long, jefeId: Long): Result<Unit> =
        revisar(jornadaId, jefeId) { it.copy(estado = EstadoJornada.VALIDADA, comentarioJefe = null) }

    /** Pasa una jornada Cerrada a Rechazada con un comentario obligatorio (RF-JEF-05, RN-11). */
    suspend fun rechazar(jornadaId: Long, jefeId: Long, comentario: String): Result<Unit> {
        if (comentario.isBlank()) return Result.failure(ReglaNegocioException("El comentario es obligatorio"))
        return revisar(jornadaId, jefeId) {
            it.copy(estado = EstadoJornada.RECHAZADA, comentarioJefe = comentario.trim())
        }
    }

    private suspend fun revisar(jornadaId: Long, jefeId: Long, cambio: (Jornada) -> Jornada): Result<Unit> = llamarApi {
        val actual = datos.jornadas().find { it.jornada.id == jornadaId }
            ?: throw ReglaNegocioException("Jornada no encontrada")
        if (catalogo.obtenerUsuario(actual.jornada.tecnicoId)?.jefeId != jefeId) {
            throw ReglaNegocioException("Esta jornada no corresponde a tu equipo")
        }
        // Una jornada Validada o Rechazada no la modifica nadie (RN-10)
        if (actual.jornada.estado != EstadoJornada.CERRADA) {
            throw ReglaNegocioException("La jornada ya fue revisada")
        }
        datos.guardarJornada(cambio(actual.jornada), actual.traslados)
        Unit
    }

    /** Km oficiales del tramo, para saber si un km declarado exigirá explicar un motivo. */
    suspend fun obtenerKmOficial(tramoId: Long): Double? = catalogo.obtenerTramo(tramoId)?.kmOficial

    suspend fun obtenerUltimoKmVehiculo(vehiculoId: Long): Int? = datos.ultimoKm(vehiculoId)

    /**
     * Abre una jornada para el técnico (RF-TEC-01). Falla si el técnico ya tiene una jornada
     * abierta o si el vehículo está en uso en otra (RN-06; esto último lo verifica el servidor).
     */
    suspend fun iniciarJornada(
        tecnicoId: Long,
        vehiculoId: Long,
        kmInicial: Int,
        fotoUri: String?,
        ubicacion: Coordenadas?
    ): Result<Long> = llamarApi {
        if (abiertaDe(datos.jornadas(), tecnicoId) != null) {
            throw ReglaNegocioException("Ya tienes una jornada abierta")
        }
        val nueva = Jornada(
            tecnicoId = tecnicoId,
            vehiculoId = vehiculoId,
            inicio = System.currentTimeMillis(),
            kmInicial = kmInicial,
            fotoInicioUri = fotoUri,
            latInicio = ubicacion?.lat,
            lonInicio = ubicacion?.lon
        )
        datos.guardarJornada(nueva, emptyList()).jornada.id
    }

    /** Lugar donde está el técnico: el destino del último traslado o la sede. */
    suspend fun obtenerUbicacionActual(tecnicoId: Long): Destino? {
        val abierta = abiertaDe(datos.jornadas(), tecnicoId) ?: return null
        val ultimo = abierta.traslados.maxByOrNull { it.orden }
        return if (ultimo != null) catalogo.obtenerDestino(ultimo.destinoId) else catalogo.obtenerSede()
    }

    /** Traslado con salida registrada y llegada pendiente, si existe. */
    suspend fun obtenerTrasladoEnCurso(tecnicoId: Long): TrasladoConNombres? {
        val abierta = abiertaDe(datos.jornadas(), tecnicoId) ?: return null
        val ultimo = abierta.traslados.maxByOrNull { it.orden } ?: return null
        if (ultimo.horaLlegada != null) return null
        return conNombres(ultimo, datos.catalogo())
    }

    /**
     * Registra la salida hacia [destinoId] (RF-TEC-04). Si no existe un tramo oficial vigente
     * el traslado queda sin tramo y se marcará como no catalogado al llegar (RF-TEC-05).
     */
    suspend fun registrarSalida(tecnicoId: Long, destinoId: Long, ubicacion: Coordenadas?): Result<Long> = llamarApi {
        val abierta = abiertaDe(datos.jornadas(), tecnicoId)
            ?: throw ReglaNegocioException("No tienes una jornada abierta")
        val jornada = abierta.jornada
        val ultimo = abierta.traslados.maxByOrNull { it.orden }
        if (ultimo != null && ultimo.horaLlegada == null) {
            throw ReglaNegocioException("Primero registra la llegada del traslado en curso")
        }
        val origenId = ultimo?.destinoId
            ?: catalogo.obtenerSede()?.id
            ?: throw ReglaNegocioException("No hay una sede configurada")
        if (origenId == destinoId) {
            throw ReglaNegocioException("El destino debe ser distinto del origen")
        }
        val ahora = System.currentTimeMillis()
        val nuevo = Traslado(
            jornadaId = jornada.id,
            orden = (ultimo?.orden ?: 0) + 1,
            origenId = origenId,
            destinoId = destinoId,
            tramoId = catalogo.obtenerTramoVigente(origenId, destinoId, ahora)?.id,
            horaSalida = ahora,
            // El km de inicio es el de la llegada anterior o el inicial de la jornada (RF-TEC-07)
            kmInicio = ultimo?.kmFin ?: jornada.kmInicial,
            latSalida = ubicacion?.lat,
            lonSalida = ubicacion?.lon
        )
        val guardada = datos.guardarJornada(jornada, abierta.traslados + nuevo)
        guardada.traslados.first { it.uuid == nuevo.uuid }.id
    }

    /**
     * Registra la llegada del traslado (RF-TEC-09, RN-04). La app calcula el resultado para exigir
     * el motivo si hay alerta (RF-TEC-16); el servidor lo vuelve a calcular con su catálogo y su
     * respuesta es la que vale. Si el destino es la sede, además cierra la jornada (RF-TEC-11,
     * RF-TEC-12): queda Validada si ningún traslado tiene alertas y Cerrada, pendiente del jefe,
     * si alguno las tiene ([PoliticaValidacion]).
     */
    suspend fun registrarLlegada(
        trasladoId: Long,
        kmFin: Int,
        fotoUri: String?,
        ubicacion: Coordenadas?,
        motivoTipo: MotivoDesvio? = null,
        motivoDetalle: String? = null
    ): Result<ResultadoLlegada> = llamarApi {
        val actual = datos.jornadas().find { j -> j.traslados.any { it.id == trasladoId } }
            ?: throw ReglaNegocioException("Traslado no encontrado")
        val jornada = actual.jornada
        val traslado = actual.traslados.first { it.id == trasladoId }
        val vehiculo = catalogo.obtenerVehiculo(jornada.vehiculoId)
            ?: throw ReglaNegocioException("Vehículo no encontrado")
        val tarifa = catalogo.obtenerTarifaVigente(traslado.horaSalida)
            ?: throw ReglaNegocioException("No hay una tarifa de combustible vigente")
        val tramo = traslado.tramoId?.let { catalogo.obtenerTramo(it) }

        val resultado = CalculadoraCosto.calcular(
            kmDeclarados = kmFin - traslado.kmInicio,
            kmOficial = tramo?.kmOficial,
            peaje = tramo?.peaje ?: 0,
            rendimientoKmL = vehiculo.rendimientoKmL,
            precioLitro = tarifa.precioLitro
        )
        if (PoliticaValidacion.requiereMotivo(resultado.marca) && motivoTipo == null) {
            throw ReglaNegocioException("Indica qué ocurrió en este traslado")
        }
        val ahora = System.currentTimeMillis()
        val actualizado = traslado.copy(
            horaLlegada = ahora,
            kmFin = kmFin,
            latLlegada = ubicacion?.lat,
            lonLlegada = ubicacion?.lon,
            fotoUri = fotoUri,
            costo = resultado.costo,
            desviacionPct = resultado.desviacionPct,
            marca = resultado.marca,
            motivoTipo = motivoTipo.takeIf { PoliticaValidacion.requiereMotivo(resultado.marca) },
            motivoDetalle = motivoDetalle?.trim()?.takeIf { it.isNotEmpty() && motivoTipo != null }
        )
        val traslados = actual.traslados.map { if (it.id == trasladoId) actualizado else it }

        val cierra = catalogo.obtenerDestino(traslado.destinoId)?.tipo == TipoDestino.SEDE
        val jornadaNueva = if (cierra) {
            val estado = PoliticaValidacion.estadoAlCerrar(traslados.map { it.marca })
            jornada.copy(
                fin = ahora,
                kmFinal = kmFin,
                fotoCierreUri = fotoUri,
                estado = estado,
                validacionAutomatica = estado == EstadoJornada.VALIDADA
            )
        } else {
            jornada
        }
        val guardada = datos.guardarJornada(jornadaNueva, traslados)
        ResultadoLlegada(
            traslado = guardada.traslados.first { it.uuid == traslado.uuid },
            estadoJornada = guardada.jornada.estado.takeIf { cierra }
        )
    }

    private fun abiertaDe(lista: List<JornadaConTraslados>, tecnicoId: Long): JornadaConTraslados? =
        lista.find { it.jornada.tecnicoId == tecnicoId && it.jornada.estado == EstadoJornada.ABIERTA }

    private fun resumen(j: JornadaConTraslados, cat: CatalogosDto): JornadaResumen? {
        val tecnico = cat.usuarios.find { it.id == j.jornada.tecnicoId } ?: return null
        val vehiculo = cat.vehiculos.find { it.id == j.jornada.vehiculoId } ?: return null
        return JornadaResumen(
            jornada = j.jornada,
            tecnicoNombre = tecnico.nombre,
            jefeId = tecnico.jefeId,
            vehiculoDescripcion = vehiculo.descripcion,
            vehiculoPatente = vehiculo.patente,
            totalTraslados = j.traslados.size,
            conAlerta = j.traslados.count { it.marca in alertas },
            costoTotal = j.traslados.sumOf { it.costo ?: 0.0 }
        )
    }

    private fun conNombres(t: Traslado, cat: CatalogosDto): TrasladoConNombres? {
        val origen = cat.destinos.find { it.id == t.origenId } ?: return null
        val destino = cat.destinos.find { it.id == t.destinoId } ?: return null
        return TrasladoConNombres(t, origen.nombre, destino.nombre, destino.tipo)
    }
}
