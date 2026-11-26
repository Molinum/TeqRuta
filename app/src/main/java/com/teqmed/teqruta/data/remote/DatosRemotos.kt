package com.teqmed.teqruta.data.remote

import android.util.Log
import com.teqmed.teqruta.data.local.entity.Jornada
import com.teqmed.teqruta.data.local.entity.Traslado
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlin.coroutines.cancellation.CancellationException

/** Estado de la última comunicación con la API, para el botón de actualizar de la barra superior. */
sealed interface EstadoConexion {
    data object Inactiva : EstadoConexion
    data object Cargando : EstadoConexion
    data class Ok(val hora: Long) : EstadoConexion
    data class Error(val mensaje: String) : EstadoConexion
}

/** Jornada con sus traslados, tal como la devuelve la API. */
data class JornadaConTraslados(val jornada: Jornada, val traslados: List<Traslado>)

/**
 * Única fuente de datos de la app: todo se lee y se escribe a través de la API REST.
 *
 * Guarda en memoria la última respuesta de cada consulta solo para que varias pantallas
 * observen los mismos datos. Cada vez que una pantalla empieza a observar, se vuelve a pedir
 * al servidor, y cada escritura actualiza estos datos con lo que el servidor respondió.
 */
class DatosRemotos(private val api: () -> TeqRutaApi) {

    private companion object {
        const val TAG = "TeqRutaApi"

        /** Evita repetir la misma consulta cuando varias pantallas empiezan a observar a la vez. */
        const val VIGENCIA_MS = 2_000L
    }

    private val _catalogo = MutableStateFlow<CatalogosDto?>(null)
    private val _jornadas = MutableStateFlow<List<JornadaConTraslados>>(emptyList())
    private var catalogoCargadoEn = 0L
    private var jornadasCargadasEn = 0L

    private val _estado = MutableStateFlow<EstadoConexion>(EstadoConexion.Inactiva)
    val estado: StateFlow<EstadoConexion> = _estado.asStateFlow()

    /** Usuario con sesión iniciada: define qué jornadas devuelve el servidor (RN-15). */
    @Volatile
    var usuarioId: Long? = null
        set(valor) {
            if (valor != field) {
                _jornadas.value = emptyList()
                jornadasCargadasEn = 0L
            }
            field = valor
        }

    // ------------------------------------------------------------------ catálogo

    fun observarCatalogo(): Flow<CatalogosDto> = _catalogo.asStateFlow().filterNotNull().onStart {
        if (System.currentTimeMillis() - catalogoCargadoEn > VIGENCIA_MS) intentar { cargarCatalogo() }
    }

    suspend fun cargarCatalogo(): CatalogosDto =
        api().catalogos().also {
            _catalogo.value = it
            catalogoCargadoEn = System.currentTimeMillis()
        }

    /** Último catálogo recibido; si aún no hay uno, lo pide. Vacío si el servidor no responde. */
    suspend fun catalogo(): CatalogosDto =
        _catalogo.value ?: intentar { cargarCatalogo() } ?: CatalogosDto(
            emptyList(), emptyList(), emptyList(), emptyList(), emptyList()
        )

    // ------------------------------------------------------------------ jornadas

    fun observarJornadas(): Flow<List<JornadaConTraslados>> = _jornadas.asStateFlow().onStart {
        if (System.currentTimeMillis() - jornadasCargadasEn > VIGENCIA_MS) intentar { cargarJornadas() }
    }

    suspend fun cargarJornadas(): List<JornadaConTraslados> {
        val id = usuarioId ?: return emptyList()
        return api().jornadas(id).jornadas.map { it.aModelo() }.also {
            _jornadas.value = it
            jornadasCargadasEn = System.currentTimeMillis()
        }
    }

    /** Jornadas recién pedidas al servidor, para validar una operación antes de escribir. */
    suspend fun jornadas(): List<JornadaConTraslados> = intentar { cargarJornadas() } ?: _jornadas.value

    /** Envía la jornada con sus traslados y devuelve cómo quedó en el servidor (con ids y costos). */
    suspend fun guardarJornada(jornada: Jornada, traslados: List<Traslado>): JornadaConTraslados {
        val guardada = api().subirJornada(jornada.uuid, jornada.aDto(traslados)).aModelo()
        _jornadas.update { lista -> lista.filter { it.jornada.uuid != guardada.jornada.uuid } + guardada }
        return guardada
    }

    suspend fun ultimoKm(vehiculoId: Long): Int? = intentar { api().ultimoKm(vehiculoId).kmFinal }

    /** Vuelve a pedir el catálogo y las jornadas (botón de actualizar). */
    suspend fun actualizarTodo(): Result<Unit> = try {
        _estado.value = EstadoConexion.Cargando
        cargarCatalogo()
        cargarJornadas()
        _estado.value = EstadoConexion.Ok(System.currentTimeMillis())
        Result.success(Unit)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        _estado.value = EstadoConexion.Error(mensajeDeError(e))
        Result.failure(e)
    }

    /** Ejecuta una lectura; si falla deja el error en [estado] y devuelve null. */
    private suspend fun <T> intentar(bloque: suspend () -> T): T? = try {
        bloque().also { _estado.value = EstadoConexion.Ok(System.currentTimeMillis()) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Falló la consulta a la API", e)
        _estado.value = EstadoConexion.Error(mensajeDeError(e))
        null
    }

    private fun JornadaDto.aModelo() = JornadaConTraslados(aEntidad(), traslados.map { it.aEntidad(id) })
}
