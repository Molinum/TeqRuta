package com.teqmed.teqruta.ui.tecnico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.Jornada
import com.teqmed.teqruta.data.local.entity.Vehiculo
import com.teqmed.teqruta.data.repository.CatalogoRepository
import com.teqmed.teqruta.data.repository.JornadaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class JornadaActiva(
    val jornada: Jornada,
    val vehiculo: Vehiculo?
)

data class TecnicoHomeState(
    val cargando: Boolean = true,
    /** Jornada abierta del técnico, o null si no tiene una. */
    val jornadaActiva: JornadaActiva? = null
)

class TecnicoHomeViewModel(
    tecnicoId: Long,
    jornadaRepository: JornadaRepository,
    private val catalogoRepository: CatalogoRepository
) : ViewModel() {

    val estado: StateFlow<TecnicoHomeState> = jornadaRepository.observarJornadaAbierta(tecnicoId)
        .map { jornada ->
            TecnicoHomeState(
                cargando = false,
                jornadaActiva = jornada?.let {
                    JornadaActiva(it, catalogoRepository.obtenerVehiculo(it.vehiculoId))
                }
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TecnicoHomeState())
}
