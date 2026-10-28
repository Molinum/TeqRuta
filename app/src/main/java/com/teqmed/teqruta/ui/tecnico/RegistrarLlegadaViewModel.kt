package com.teqmed.teqruta.ui.tecnico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.Traslado
import com.teqmed.teqruta.data.local.entity.TrasladoConNombres
import com.teqmed.teqruta.data.repository.JornadaRepository
import com.teqmed.teqruta.device.LocationProvider
import com.teqmed.teqruta.domain.model.TipoDestino
import com.teqmed.teqruta.domain.validation.JornadaValidator
import com.teqmed.teqruta.domain.validation.esValido
import com.teqmed.teqruta.domain.validation.mensajeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegistrarLlegadaState(
    val enCurso: TrasladoConNombres? = null,
    val km: String = "",
    val fotoPath: String? = null,
    val kmError: String? = null,
    val fotoError: String? = null,
    val errorGeneral: String? = null,
    val guardando: Boolean = false,
    /** Traslado ya calculado; la pantalla muestra el resultado. */
    val resultado: Traslado? = null,
    val conUbicacion: Boolean = false
) {
    /** Llegar a la sede cierra la jornada: la foto pasa a ser obligatoria (RN-07). */
    val esCierre get() = enCurso?.destinoTipo == TipoDestino.SEDE
}

class RegistrarLlegadaViewModel(
    tecnicoId: Long,
    private val jornadaRepository: JornadaRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _estado = MutableStateFlow(RegistrarLlegadaState())
    val estado: StateFlow<RegistrarLlegadaState> = _estado.asStateFlow()

    init {
        viewModelScope.launch {
            val enCurso = jornadaRepository.obtenerTrasladoEnCurso(tecnicoId)
            _estado.update { it.copy(enCurso = enCurso) }
        }
    }

    fun onKmChange(valor: String) {
        _estado.update { it.copy(km = valor.filter(Char::isDigit), kmError = null, errorGeneral = null) }
    }

    fun onFotoTomada(path: String) {
        _estado.update { it.copy(fotoPath = path, fotoError = null) }
    }

    fun onSubmit() {
        val actual = _estado.value
        val enCurso = actual.enCurso ?: return
        if (actual.guardando) return

        val km = JornadaValidator.validarKmLlegada(actual.km, enCurso.traslado.kmInicio)
        val faltaFoto = actual.esCierre && actual.fotoPath == null
        _estado.update {
            it.copy(
                kmError = km.mensajeError,
                fotoError = if (faltaFoto) "La foto del odómetro es obligatoria al cerrar la jornada" else null
            )
        }
        if (!km.esValido || faltaFoto) return

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, errorGeneral = null) }
            val ubicacion = locationProvider.obtenerUbicacion()
            jornadaRepository.registrarLlegada(enCurso.traslado.id, actual.km.trim().toInt(), actual.fotoPath, ubicacion)
                .onSuccess { traslado ->
                    _estado.update { it.copy(guardando = false, resultado = traslado, conUbicacion = ubicacion != null) }
                }
                .onFailure { e -> _estado.update { it.copy(guardando = false, errorGeneral = e.message) } }
        }
    }
}
