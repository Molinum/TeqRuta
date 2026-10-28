package com.teqmed.teqruta.ui.tecnico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.Tramo
import com.teqmed.teqruta.data.repository.CatalogoRepository
import com.teqmed.teqruta.data.repository.JornadaRepository
import com.teqmed.teqruta.device.LocationProvider
import com.teqmed.teqruta.domain.model.TipoDestino
import com.teqmed.teqruta.domain.validation.Validators
import com.teqmed.teqruta.domain.validation.mensajeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrarSalidaState(
    val origen: Destino? = null,
    val destinos: List<Destino> = emptyList(),
    val destino: Destino? = null,
    /** Tramo oficial entre origen y destino; null si no está catalogado. */
    val tramo: Tramo? = null,
    val tramoConsultado: Boolean = false,
    val destinoError: String? = null,
    val errorGeneral: String? = null,
    val guardando: Boolean = false,
    val guardado: Boolean = false
)

class RegistrarSalidaViewModel(
    private val tecnicoId: Long,
    private val jornadaRepository: JornadaRepository,
    private val catalogoRepository: CatalogoRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _estado = MutableStateFlow(RegistrarSalidaState())
    val estado: StateFlow<RegistrarSalidaState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val origen = jornadaRepository.obtenerUbicacionActual(tecnicoId)
            // La vuelta a la sede se registra desde el inicio con "Volver a la sede"
            val destinos = catalogoRepository.observarDestinosActivos().first()
                .filter { it.tipo == TipoDestino.CLINICA && it.id != origen?.id }
            _estado.update { it.copy(origen = origen, destinos = destinos) }
        }
    }

    fun onDestinoSeleccionado(destino: Destino) {
        _estado.update { it.copy(destino = destino, destinoError = null, errorGeneral = null, tramoConsultado = false) }
        val origen = _estado.value.origen ?: return
        viewModelScope.launch {
            val tramo = catalogoRepository.obtenerTramoVigente(origen.id, destino.id, System.currentTimeMillis())
            _estado.update { it.copy(tramo = tramo, tramoConsultado = true) }
        }
    }

    fun onSubmit() {
        val actual = _estado.value
        if (actual.guardando) return
        val validacion = Validators.seleccion(actual.destino, "un destino")
        _estado.update { it.copy(destinoError = validacion.mensajeError) }
        val destino = actual.destino ?: return

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, errorGeneral = null) }
            val ubicacion = locationProvider.obtenerUbicacion()
            jornadaRepository.registrarSalida(tecnicoId, destino.id, ubicacion)
                .onSuccess { _estado.update { it.copy(guardando = false, guardado = true) } }
                .onFailure { e -> _estado.update { it.copy(guardando = false, errorGeneral = e.message) } }
        }
    }
}
