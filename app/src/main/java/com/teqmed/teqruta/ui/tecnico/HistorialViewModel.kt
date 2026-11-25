package com.teqmed.teqruta.ui.tecnico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.JornadaResumen
import com.teqmed.teqruta.data.repository.JornadaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HistorialState(
    val cargando: Boolean = true,
    val jornadas: List<JornadaResumen> = emptyList()
)

/** Historial de jornadas del técnico, con su estado y el comentario del jefe si fue rechazada (RF-TEC-13). */
class HistorialViewModel(tecnicoId: Long, jornadaRepository: JornadaRepository) : ViewModel() {

    val estado: StateFlow<HistorialState> = jornadaRepository.observarResumenesDeTecnico(tecnicoId)
        .map { HistorialState(cargando = false, jornadas = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistorialState())
}
