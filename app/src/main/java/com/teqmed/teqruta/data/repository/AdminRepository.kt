package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.TarifaCombustible
import com.teqmed.teqruta.data.local.entity.Tramo
import com.teqmed.teqruta.data.local.entity.TramoConNombres
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.data.local.entity.Vehiculo
import com.teqmed.teqruta.data.remote.DatosRemotos
import com.teqmed.teqruta.data.remote.TeqRutaApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Mantenedores del administrador: destinos, vehículos, tramos, tarifas y usuarios.
 * Nada se elimina: lo que tiene registros asociados se desactiva (RN-13).
 *
 * Cada cambio se envía a la API, que lo valida y asigna los ids; luego se vuelve a pedir el
 * catálogo para que todas las pantallas lo muestren.
 */
class AdminRepository(
    private val api: () -> TeqRutaApi,
    private val datos: DatosRemotos
) {

    private suspend fun escribir(remoto: suspend () -> Unit): Result<Unit> = llamarApi {
        remoto()
        datos.cargarCatalogo()
        Unit
    }

    fun observarDestinos(): Flow<List<Destino>> = datos.observarCatalogo().map { cat ->
        cat.destinos.sortedWith(compareBy({ it.ciudad }, { it.nombre }))
    }

    suspend fun guardarDestino(destino: Destino): Result<Unit> = escribir { api().guardarDestino(destino) }

    fun observarVehiculos(): Flow<List<Vehiculo>> = datos.observarCatalogo().map { cat ->
        cat.vehiculos.sortedBy { it.descripcion }
    }

    suspend fun guardarVehiculo(vehiculo: Vehiculo): Result<Unit> = escribir { api().guardarVehiculo(vehiculo) }

    fun observarUsuarios(): Flow<List<Usuario>> = datos.observarCatalogo().map { cat ->
        cat.usuarios.sortedBy { it.nombre }
    }

    suspend fun guardarUsuario(usuario: Usuario): Result<Unit> = escribir { api().guardarUsuario(usuario) }

    /** Todas las versiones de todos los tramos, con los nombres de origen y destino. */
    fun observarTramos(): Flow<List<TramoConNombres>> = datos.observarCatalogo().map { cat ->
        val nombres = cat.destinos.associate { it.id to it.nombre }
        cat.tramos
            .mapNotNull { t ->
                val origen = nombres[t.origenId] ?: return@mapNotNull null
                val destino = nombres[t.destinoId] ?: return@mapNotNull null
                TramoConNombres(t, origen, destino)
            }
            .sortedWith(
                compareBy<TramoConNombres>({ it.origenNombre }, { it.destinoNombre })
                    .thenByDescending { it.tramo.vigenteDesde }
                    .thenByDescending { it.tramo.id }
            )
    }

    /**
     * Crea el tramo, o una nueva versión si ya existe uno para el par (RF-ADM-04): el servidor
     * cierra la versión anterior en la fecha de vigencia de la nueva y la conserva.
     */
    suspend fun guardarTramo(nuevo: Tramo): Result<Unit> = escribir { api().guardarTramo(nuevo) }

    fun observarTarifas(): Flow<List<TarifaCombustible>> = datos.observarCatalogo().map { cat ->
        cat.tarifas.sortedByDescending { it.vigenteDesde }
    }

    suspend fun registrarTarifa(tarifa: TarifaCombustible): Result<Unit> = escribir { api().registrarTarifa(tarifa) }
}
