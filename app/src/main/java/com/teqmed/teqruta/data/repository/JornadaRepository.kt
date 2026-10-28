package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.dao.JornadaDao
import com.teqmed.teqruta.data.local.dao.TrasladoDao
import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.Jornada
import com.teqmed.teqruta.data.local.entity.Traslado
import com.teqmed.teqruta.data.local.entity.TrasladoConNombres
import com.teqmed.teqruta.domain.calculo.CalculadoraCosto
import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.domain.model.TipoDestino
import kotlinx.coroutines.flow.Flow

/** Error de negocio con un mensaje apto para mostrar al usuario. */
class ReglaNegocioException(mensaje: String) : Exception(mensaje)

/** Coordenadas GPS; null en los parámetros si no hay permiso o señal. */
data class Coordenadas(val lat: Double, val lon: Double)

/** Jornadas de los técnicos y los traslados que las componen. */
class JornadaRepository(
    private val jornadaDao: JornadaDao,
    private val trasladoDao: TrasladoDao,
    private val catalogo: CatalogoRepository
) {
    fun observarJornadaAbierta(tecnicoId: Long): Flow<Jornada?> = jornadaDao.observarAbierta(tecnicoId)

    fun observarTraslados(jornadaId: Long): Flow<List<TrasladoConNombres>> =
        trasladoDao.observarConNombres(jornadaId)

    suspend fun obtenerJornada(id: Long): Jornada? = jornadaDao.obtenerPorId(id)

    suspend fun obtenerUltimoKmVehiculo(vehiculoId: Long): Int? = jornadaDao.obtenerUltimoKmFinal(vehiculoId)

    /**
     * Abre una jornada para el técnico (RF-TEC-01). Falla si el técnico ya tiene una jornada
     * abierta o si el vehículo está en uso en otra (RN-06).
     */
    suspend fun iniciarJornada(
        tecnicoId: Long,
        vehiculoId: Long,
        kmInicial: Int,
        fotoUri: String?,
        ubicacion: Coordenadas?
    ): Result<Long> {
        if (jornadaDao.obtenerAbierta(tecnicoId) != null) {
            return Result.failure(ReglaNegocioException("Ya tienes una jornada abierta"))
        }
        if (jornadaDao.obtenerAbiertaPorVehiculo(vehiculoId) != null) {
            return Result.failure(ReglaNegocioException("El vehículo está en uso en otra jornada abierta"))
        }
        val id = jornadaDao.insertar(
            Jornada(
                tecnicoId = tecnicoId,
                vehiculoId = vehiculoId,
                inicio = System.currentTimeMillis(),
                kmInicial = kmInicial,
                fotoInicioUri = fotoUri,
                latInicio = ubicacion?.lat,
                lonInicio = ubicacion?.lon
            )
        )
        return Result.success(id)
    }

    /** Lugar donde está el técnico: el destino del último traslado o la sede. */
    suspend fun obtenerUbicacionActual(tecnicoId: Long): Destino? {
        val jornada = jornadaDao.obtenerAbierta(tecnicoId) ?: return null
        val ultimo = trasladoDao.obtenerUltimo(jornada.id)
        return if (ultimo != null) catalogo.obtenerDestino(ultimo.destinoId) else catalogo.obtenerSede()
    }

    /** Traslado con salida registrada y llegada pendiente, si existe. */
    suspend fun obtenerTrasladoEnCurso(tecnicoId: Long): TrasladoConNombres? {
        val jornada = jornadaDao.obtenerAbierta(tecnicoId) ?: return null
        val ultimo = trasladoDao.obtenerUltimo(jornada.id) ?: return null
        if (ultimo.horaLlegada != null) return null
        return trasladoDao.obtenerConNombres(ultimo.id)
    }

    /**
     * Registra la salida hacia [destinoId] (RF-TEC-04). Si no existe un tramo oficial vigente
     * el traslado queda sin tramo y se marcará como no catalogado al llegar (RF-TEC-05).
     */
    suspend fun registrarSalida(tecnicoId: Long, destinoId: Long, ubicacion: Coordenadas?): Result<Long> {
        val jornada = jornadaDao.obtenerAbierta(tecnicoId)
            ?: return Result.failure(ReglaNegocioException("No tienes una jornada abierta"))
        val ultimo = trasladoDao.obtenerUltimo(jornada.id)
        if (ultimo != null && ultimo.horaLlegada == null) {
            return Result.failure(ReglaNegocioException("Primero registra la llegada del traslado en curso"))
        }
        val origenId = ultimo?.destinoId
            ?: catalogo.obtenerSede()?.id
            ?: return Result.failure(ReglaNegocioException("No hay una sede configurada"))
        if (origenId == destinoId) {
            return Result.failure(ReglaNegocioException("El destino debe ser distinto del origen"))
        }
        val ahora = System.currentTimeMillis()
        val tramo = catalogo.obtenerTramoVigente(origenId, destinoId, ahora)
        val id = trasladoDao.insertar(
            Traslado(
                jornadaId = jornada.id,
                orden = (ultimo?.orden ?: 0) + 1,
                origenId = origenId,
                destinoId = destinoId,
                tramoId = tramo?.id,
                horaSalida = ahora,
                // El km de inicio es el de la llegada anterior o el inicial de la jornada (RF-TEC-07)
                kmInicio = ultimo?.kmFin ?: jornada.kmInicial,
                latSalida = ubicacion?.lat,
                lonSalida = ubicacion?.lon
            )
        )
        return Result.success(id)
    }

    /**
     * Registra la llegada del traslado y calcula su costo con los valores vigentes a la fecha
     * de salida, guardando una copia de ellos (RF-TEC-09, RN-04). Si el destino es la sede,
     * además cierra la jornada (RF-TEC-11, RF-TEC-12).
     */
    suspend fun registrarLlegada(
        trasladoId: Long,
        kmFin: Int,
        fotoUri: String?,
        ubicacion: Coordenadas?
    ): Result<Traslado> {
        val traslado = trasladoDao.obtenerPorId(trasladoId)
            ?: return Result.failure(ReglaNegocioException("Traslado no encontrado"))
        val jornada = jornadaDao.obtenerPorId(traslado.jornadaId)
            ?: return Result.failure(ReglaNegocioException("Jornada no encontrada"))
        val vehiculo = catalogo.obtenerVehiculo(jornada.vehiculoId)
            ?: return Result.failure(ReglaNegocioException("Vehículo no encontrado"))
        val tarifa = catalogo.obtenerTarifaVigente(traslado.horaSalida)
            ?: return Result.failure(ReglaNegocioException("No hay una tarifa de combustible vigente"))
        val tramo = traslado.tramoId?.let { catalogo.obtenerTramo(it) }

        val resultado = CalculadoraCosto.calcular(
            kmDeclarados = kmFin - traslado.kmInicio,
            kmOficial = tramo?.kmOficial,
            peaje = tramo?.peaje ?: 0,
            rendimientoKmL = vehiculo.rendimientoKmL,
            precioLitro = tarifa.precioLitro
        )
        val ahora = System.currentTimeMillis()
        val actualizado = traslado.copy(
            horaLlegada = ahora,
            kmFin = kmFin,
            latLlegada = ubicacion?.lat,
            lonLlegada = ubicacion?.lon,
            fotoUri = fotoUri,
            kmOficialUsado = tramo?.kmOficial,
            peajeUsado = tramo?.peaje,
            precioLitroUsado = tarifa.precioLitro,
            rendimientoUsado = vehiculo.rendimientoKmL,
            costo = resultado.costo,
            desviacionPct = resultado.desviacionPct,
            marca = resultado.marca
        )
        trasladoDao.actualizar(actualizado)

        val destino = catalogo.obtenerDestino(traslado.destinoId)
        if (destino?.tipo == TipoDestino.SEDE) {
            jornadaDao.actualizar(
                jornada.copy(
                    fin = ahora,
                    kmFinal = kmFin,
                    fotoCierreUri = fotoUri,
                    estado = EstadoJornada.CERRADA
                )
            )
        }
        return Result.success(actualizado)
    }
}
