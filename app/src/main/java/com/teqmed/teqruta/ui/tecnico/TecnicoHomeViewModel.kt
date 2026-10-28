package com.teqmed.teqruta.ui.tecnico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.Jornada
import com.teqmed.teqruta.data.local.entity.TrasladoConNombres
import com.teqmed.teqruta.data.local.entity.Vehiculo
import com.teqmed.teqruta.data.repository.CatalogoRepository
import com.teqmed.teqruta.data.repository.JornadaRepository
import com.teqmed.teqruta.device.LocationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JornadaActiva(
    val jornada: Jornada,
    val vehiculo: Vehiculo?,
    val traslados: List<TrasladoConNombres>,
    val sedeNombre: String
) {
    val completados get() = traslados.filter { it.traslado.horaLlegada != null }
    val enCurso get() = traslados.lastOrNull()?.takeIf { it.traslado.horaLlegada == null }
    val ubicacionActual get() = completados.lastOrNull()?.destinoNombre ?: sedeNombre
    val enSede get() = completados.isEmpty()
    val kmRecorridos get() = completados.sumOf { (it.traslado.kmFin ?: it.traslado.kmInicio) - it.traslado.kmInicio }
    val costoTotal get() = completados.sumOf { it.traslado.costo ?: 0.0 }
}

data class TecnicoHomeState(
    val cargando: Boolean = true,
    /** Jornada abierta del técnico, o null si no tiene una. */
    val jornadaActiva: JornadaActiva? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class TecnicoHomeViewModel(
    private val tecnicoId: Long,
    private val jornadaRepository: JornadaRepository,
    private val catalogoRepository: CatalogoRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _volviendo = MutableStateFlow(false)
    val volviendo: StateFlow<Boolean> = _volviendo.asStateFlow()

    val estado: StateFlow<TecnicoHomeState> = jornadaRepository.observarJornadaAbierta(tecnicoId)
        .flatMapLatest { jornada ->
            if (jornada == null) {
                flowOf(TecnicoHomeState(cargando = false))
            } else {
                jornadaRepository.observarTraslados(jornada.id).map { traslados ->
                    TecnicoHomeState(
                        cargando = false,
                        jornadaActiva = JornadaActiva(
                            jornada = jornada,
                            vehiculo = catalogoRepository.obtenerVehiculo(jornada.vehiculoId),
                            traslados = traslados,
                            sedeNombre = catalogoRepository.obtenerSede()?.nombre ?: "Sede"
                        )
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TecnicoHomeState())

    fun tienePermisoUbicacion() = locationProvider.tienePermiso()

    /** Registra la salida de vuelta a la sede; la llegada a la sede cierra la jornada. */
    fun volverASede() {
        if (_volviendo.value) return
        viewModelScope.launch {
            _volviendo.value = true
            val sede = catalogoRepository.obtenerSede()
            if (sede != null) {
                jornadaRepository.registrarSalida(tecnicoId, sede.id, locationProvider.obtenerUbicacion())
            }
            _volviendo.value = false
        }
    }
}
