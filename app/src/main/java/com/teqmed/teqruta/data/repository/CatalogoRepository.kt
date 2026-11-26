package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.TarifaCombustible
import com.teqmed.teqruta.data.local.entity.Tramo
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.data.local.entity.Vehiculo
import com.teqmed.teqruta.data.remote.DatosRemotos
import com.teqmed.teqruta.domain.model.TipoDestino
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Datos maestros desde la API: destinos, vehículos, tramos oficiales y tarifas de combustible. */
class CatalogoRepository(private val datos: DatosRemotos) {

    fun observarDestinosActivos(): Flow<List<Destino>> = datos.observarCatalogo().map { catalogo ->
        catalogo.destinos.filter { it.activo }.sortedWith(compareBy({ it.ciudad }, { it.nombre }))
    }

    fun observarVehiculosActivos(): Flow<List<Vehiculo>> = datos.observarCatalogo().map { catalogo ->
        catalogo.vehiculos.filter { it.activo }.sortedBy { it.descripcion }
    }

    suspend fun obtenerSede(): Destino? =
        datos.catalogo().destinos.firstOrNull { it.tipo == TipoDestino.SEDE && it.activo }

    suspend fun obtenerDestino(id: Long): Destino? = datos.catalogo().destinos.find { it.id == id }

    suspend fun obtenerVehiculo(id: Long): Vehiculo? = datos.catalogo().vehiculos.find { it.id == id }

    suspend fun obtenerUsuario(id: Long): Usuario? = datos.catalogo().usuarios.find { it.id == id }

    suspend fun obtenerTramo(id: Long): Tramo? = datos.catalogo().tramos.find { it.id == id }

    /** Versión del tramo vigente en la fecha indicada (milisegundos). */
    suspend fun obtenerTramoVigente(origenId: Long, destinoId: Long, fecha: Long): Tramo? =
        datos.catalogo().tramos
            .filter { it.origenId == origenId && it.destinoId == destinoId }
            .filter { it.vigenteDesde <= fecha && (it.vigenteHasta == null || it.vigenteHasta > fecha) }
            .maxByOrNull { it.vigenteDesde }

    suspend fun obtenerTarifaVigente(fecha: Long): TarifaCombustible? =
        datos.catalogo().tarifas.filter { it.vigenteDesde <= fecha }.maxByOrNull { it.vigenteDesde }
}
