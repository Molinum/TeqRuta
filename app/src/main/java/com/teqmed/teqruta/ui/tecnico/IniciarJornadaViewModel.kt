package com.teqmed.teqruta.ui.tecnico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.Vehiculo
import com.teqmed.teqruta.data.repository.CatalogoRepository
import com.teqmed.teqruta.data.repository.JornadaRepository
import com.teqmed.teqruta.domain.validation.JornadaValidator
import com.teqmed.teqruta.domain.validation.mensajeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class IniciarJornadaViewModel(
    private val tecnicoId: Long,
    private val jornadaRepository: JornadaRepository,
    private val catalogoRepository: CatalogoRepository
) : ViewModel() {

    private val _estado = MutableStateFlow(IniciarJornadaFormState())
    val estado: StateFlow<IniciarJornadaFormState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val sede = catalogoRepository.obtenerSede()
            _estado.update { it.copy(sedeNombre = sede?.nombre ?: "Sede") }
        }
        viewModelScope.launch {
            catalogoRepository.observarVehiculosActivos().collect { vehiculos ->
                _estado.update { it.copy(vehiculos = vehiculos) }
            }
        }
    }

    fun onVehiculoSeleccionado(vehiculo: Vehiculo) {
        _estado.update { it.copy(vehiculo = vehiculo, vehiculoError = null, errorGeneral = null) }
        viewModelScope.launch {
            val ultimoKm = jornadaRepository.obtenerUltimoKmVehiculo(vehiculo.id)
            _estado.update {
                it.copy(
                    ultimoKm = ultimoKm,
                    // Se sugiere el último km conocido para que el técnico solo lo confirme
                    kmInicial = if (it.kmInicial.isBlank() && ultimoKm != null) ultimoKm.toString() else it.kmInicial
                )
            }
        }
    }

    fun onKmInicialChange(valor: String) {
        _estado.update { it.copy(kmInicial = valor.filter(Char::isDigit), kmError = null, errorGeneral = null) }
    }

    fun onSubmit() {
        val actual = _estado.value
        if (actual.guardando) return

        val validacion = JornadaValidator.validarInicio(actual.vehiculo?.id, actual.kmInicial, actual.ultimoKm)
        _estado.update {
            it.copy(
                vehiculoError = validacion.vehiculo.mensajeError,
                kmError = validacion.kmInicial.mensajeError
            )
        }
        if (!validacion.esValido) return
        val vehiculo = actual.vehiculo ?: return

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, errorGeneral = null) }
            jornadaRepository.iniciarJornada(tecnicoId, vehiculo.id, actual.kmInicial.trim().toInt())
                .onSuccess { _estado.update { it.copy(guardando = false, guardado = true) } }
                .onFailure { e -> _estado.update { it.copy(guardando = false, errorGeneral = e.message) } }
        }
    }
}
