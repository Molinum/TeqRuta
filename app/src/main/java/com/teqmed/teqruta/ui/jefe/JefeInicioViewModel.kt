package com.teqmed.teqruta.ui.jefe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.JornadaResumen
import com.teqmed.teqruta.data.repository.JornadaRepository
import com.teqmed.teqruta.domain.model.EstadoJornada
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class FiltroJornadas(val etiqueta: String, val estado: EstadoJornada?) {
    POR_VALIDAR("Por validar", EstadoJornada.CERRADA),
    VALIDADAS("Validadas", EstadoJornada.VALIDADA),
    RECHAZADAS("Rechazadas", EstadoJornada.RECHAZADA),
    TODAS("Todas", null)
}

data class JefeInicioState(
    val cargando: Boolean = true,
    val filtro: FiltroJornadas = FiltroJornadas.POR_VALIDAR,
    val jornadas: List<JornadaResumen> = emptyList(),
    val porValidar: Int = 0
)

/** Jornadas de los técnicos a cargo del jefe (RN-15), con las que tienen alertas primero (RF-JEF-02). */
class JefeInicioViewModel(jefeId: Long, jornadaRepository: JornadaRepository) : ViewModel() {

    private val filtro = MutableStateFlow(FiltroJornadas.POR_VALIDAR)

    val estado: StateFlow<JefeInicioState> = combine(
        jornadaRepository.observarResumenesDeJefe(jefeId),
        filtro
    ) { todas, filtroActual ->
        val visibles = todas
            .filter { filtroActual.estado == null || it.jornada.estado == filtroActual.estado }
            .sortedWith(
                compareByDescending<JornadaResumen> { it.tieneAlerta && it.jornada.estado == EstadoJornada.CERRADA }
                    .thenByDescending { it.jornada.inicio }
            )
        JefeInicioState(
            cargando = false,
            filtro = filtroActual,
            jornadas = visibles,
            porValidar = todas.count { it.jornada.estado == EstadoJornada.CERRADA }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JefeInicioState())

    fun onFiltroChange(nuevo: FiltroJornadas) = filtro.update { nuevo }
}
