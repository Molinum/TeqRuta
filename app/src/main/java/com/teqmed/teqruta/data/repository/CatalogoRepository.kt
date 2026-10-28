package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.dao.DestinoDao
import com.teqmed.teqruta.data.local.dao.TarifaDao
import com.teqmed.teqruta.data.local.dao.TramoDao
import com.teqmed.teqruta.data.local.dao.VehiculoDao
import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.TarifaCombustible
import com.teqmed.teqruta.data.local.entity.Tramo
import com.teqmed.teqruta.data.local.entity.Vehiculo
import kotlinx.coroutines.flow.Flow

/** Datos maestros: destinos, vehículos, tramos oficiales y tarifas de combustible. */
class CatalogoRepository(
    private val destinoDao: DestinoDao,
    private val vehiculoDao: VehiculoDao,
    private val tramoDao: TramoDao,
    private val tarifaDao: TarifaDao
) {
    fun observarDestinosActivos(): Flow<List<Destino>> = destinoDao.observarActivos()

    fun observarVehiculosActivos(): Flow<List<Vehiculo>> = vehiculoDao.observarActivos()

    suspend fun obtenerSede(): Destino? = destinoDao.obtenerSede()

    suspend fun obtenerDestino(id: Long): Destino? = destinoDao.obtenerPorId(id)

    suspend fun obtenerVehiculo(id: Long): Vehiculo? = vehiculoDao.obtenerPorId(id)

    suspend fun obtenerTramo(id: Long): Tramo? = tramoDao.obtenerPorId(id)

    suspend fun obtenerTramoVigente(origenId: Long, destinoId: Long, fecha: Long): Tramo? =
        tramoDao.obtenerVigente(origenId, destinoId, fecha)

    suspend fun obtenerTarifaVigente(fecha: Long): TarifaCombustible? =
        tarifaDao.obtenerVigente(fecha)
}
