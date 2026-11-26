package com.teqmed.teqruta.ui.tecnico

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.Traslado
import com.teqmed.teqruta.data.local.entity.TrasladoConNombres
import com.teqmed.teqruta.data.repository.JornadaRepository
import com.teqmed.teqruta.device.LocationProvider
import com.teqmed.teqruta.domain.calculo.DetectorDesviacion
import com.teqmed.teqruta.domain.calculo.PoliticaValidacion
import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.domain.model.MotivoDesvio
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
    /** Km oficiales del tramo, solo para decidir si se pide un motivo; nunca se muestran al técnico. */
    val kmOficial: Double? = null,
    val tramoCargado: Boolean = false,
    val km: String = "",
    val fotoPath: String? = null,
    val motivoTipo: MotivoDesvio? = null,
    val motivoDetalle: String = "",
    val kmError: String? = null,
    val fotoError: String? = null,
    val motivoError: String? = null,
    val detalleError: String? = null,
    val errorGeneral: String? = null,
    val guardando: Boolean = false,
    /** Traslado ya registrado; la pantalla muestra la confirmación. */
    val resultado: Traslado? = null,
    /** Estado con que quedó la jornada si esta llegada la cerró. */
    val estadoJornada: EstadoJornada? = null,
    val conUbicacion: Boolean = false
) {
    /** Llegar a la sede cierra la jornada: la foto pasa a ser obligatoria (RN-07). */
    val esCierre get() = enCurso?.destinoTipo == TipoDestino.SEDE

    /** Sin tramo oficial siempre se pide el motivo; con tramo, solo si el km ingresado lo exige. */
    val sinTramo get() = enCurso?.traslado?.tramoId == null

    val requiereMotivo: Boolean
        get() {
            val traslado = enCurso?.traslado ?: return false
            if (sinTramo) return true
            if (!tramoCargado) return false
            val declarados = (km.toIntOrNull() ?: return false) - traslado.kmInicio
            if (declarados <= 0) return false
            return PoliticaValidacion.requiereMotivo(DetectorDesviacion.marcar(declarados, kmOficial))
        }
}

class RegistrarLlegadaViewModel(
    tecnicoId: Long,
    private val jornadaRepository: JornadaRepository,
    private val locationProvider: LocationProvider
) : ViewModel() {

    private val _estado = MutableStateFlow(RegistrarLlegadaState())
    val estado: StateFlow<RegistrarLlegadaState> = _estado.asStateFlow()

    init {
        // Calienta el GPS mientras el técnico ingresa el km, para no esperarlo al guardar
        viewModelScope.launch { locationProvider.obtenerUbicacion() }
        viewModelScope.launch {
            val enCurso = jornadaRepository.obtenerTrasladoEnCurso(tecnicoId)
            val kmOficial = enCurso?.traslado?.tramoId?.let { jornadaRepository.obtenerKmOficial(it) }
            _estado.update { it.copy(enCurso = enCurso, kmOficial = kmOficial, tramoCargado = true) }
        }
    }

    fun onKmChange(valor: String) {
        _estado.update { it.copy(km = valor.filter(Char::isDigit), kmError = null, errorGeneral = null) }
    }

    fun onFotoTomada(path: String) {
        _estado.update { it.copy(fotoPath = path, fotoError = null) }
    }

    fun onMotivoChange(motivo: MotivoDesvio) {
        _estado.update { it.copy(motivoTipo = motivo, motivoError = null, detalleError = null) }
    }

    fun onDetalleChange(valor: String) {
        _estado.update { it.copy(motivoDetalle = valor, detalleError = null) }
    }

    fun onSubmit() {
        val actual = _estado.value
        val enCurso = actual.enCurso ?: return
        if (actual.guardando) return

        val km = JornadaValidator.validarKmLlegada(actual.km, enCurso.traslado.kmInicio)
        val faltaFoto = actual.esCierre && actual.fotoPath == null
        val pideMotivo = actual.requiereMotivo
        val motivo = if (pideMotivo) JornadaValidator.validarMotivo(actual.motivoTipo, actual.motivoDetalle) else null
        _estado.update {
            it.copy(
                kmError = km.mensajeError,
                fotoError = if (faltaFoto) "La foto del odómetro es obligatoria al cerrar la jornada" else null,
                motivoError = motivo?.tipo?.mensajeError,
                detalleError = motivo?.detalle?.mensajeError
            )
        }
        if (!km.esValido || faltaFoto || motivo?.esValido == false) return

        viewModelScope.launch {
            _estado.update { it.copy(guardando = true, errorGeneral = null) }
            val ubicacion = locationProvider.obtenerUbicacion()
            jornadaRepository.registrarLlegada(
                trasladoId = enCurso.traslado.id,
                kmFin = actual.km.trim().toInt(),
                fotoUri = actual.fotoPath,
                ubicacion = ubicacion,
                motivoTipo = actual.motivoTipo.takeIf { pideMotivo },
                motivoDetalle = actual.motivoDetalle.takeIf { pideMotivo }
            )
                .onSuccess { r ->
                    _estado.update {
                        it.copy(
                            guardando = false,
                            resultado = r.traslado,
                            estadoJornada = r.estadoJornada,
                            conUbicacion = ubicacion != null
                        )
                    }
                }
                .onFailure { e -> _estado.update { it.copy(guardando = false, errorGeneral = e.message) } }
        }
    }
}
