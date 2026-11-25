package com.teqmed.teqruta.ui.jornada

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.JornadaResumen
import com.teqmed.teqruta.data.local.entity.TrasladoConNombres
import com.teqmed.teqruta.data.repository.JornadaRepository
import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.domain.validation.Validators
import com.teqmed.teqruta.domain.validation.esValido
import com.teqmed.teqruta.domain.validation.mensajeError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Parte del estado que viene de la interacción del jefe (diálogos y envío), no de la base de datos. */
private data class RevisionUi(
    val confirmandoValidar: Boolean = false,
    val rechazando: Boolean = false,
    val comentario: String = "",
    val comentarioError: String? = null,
    val procesando: Boolean = false,
    val errorGeneral: String? = null,
    val revisada: Boolean = false
)

data class JornadaDetalleState(
    val cargando: Boolean = true,
    val resumen: JornadaResumen? = null,
    val traslados: List<TrasladoConNombres> = emptyList(),
    /** El usuario no es el técnico de la jornada ni su jefe (RN-15). */
    val noAutorizado: Boolean = false,
    /** El jefe del técnico ve costos, km oficiales y desviación; el técnico no. */
    val esJefe: Boolean = false,
    /** Solo se puede revisar una jornada Cerrada (RN-10). */
    val puedeRevisar: Boolean = false,
    val confirmandoValidar: Boolean = false,
    val rechazando: Boolean = false,
    val comentario: String = "",
    val comentarioError: String? = null,
    val procesando: Boolean = false,
    val errorGeneral: String? = null,
    /** La jornada se validó o rechazó; la pantalla vuelve a la lista. */
    val revisada: Boolean = false
)

/** Detalle de una jornada para su técnico (solo lectura) o para su jefe (con validar y rechazar). */
class JornadaDetalleViewModel(
    private val usuarioId: Long,
    private val jornadaId: Long,
    private val jornadaRepository: JornadaRepository
) : ViewModel() {

    private val ui = MutableStateFlow(RevisionUi())

    val estado: StateFlow<JornadaDetalleState> = combine(
        jornadaRepository.observarResumen(jornadaId),
        jornadaRepository.observarTraslados(jornadaId),
        ui
    ) { resumen, traslados, revision ->
        val esJefe = resumen?.jefeId == usuarioId
        val esTecnico = resumen?.jornada?.tecnicoId == usuarioId
        JornadaDetalleState(
            cargando = false,
            resumen = resumen,
            traslados = traslados,
            noAutorizado = resumen != null && !esJefe && !esTecnico,
            esJefe = esJefe,
            puedeRevisar = esJefe && resumen?.jornada?.estado == EstadoJornada.CERRADA,
            confirmandoValidar = revision.confirmandoValidar,
            rechazando = revision.rechazando,
            comentario = revision.comentario,
            comentarioError = revision.comentarioError,
            procesando = revision.procesando,
            errorGeneral = revision.errorGeneral,
            revisada = revision.revisada
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), JornadaDetalleState())

    fun pedirValidar() = ui.update { it.copy(confirmandoValidar = true, errorGeneral = null) }

    fun cancelarValidar() = ui.update { it.copy(confirmandoValidar = false) }

    fun pedirRechazo() = ui.update { it.copy(rechazando = true, comentario = "", comentarioError = null, errorGeneral = null) }

    fun cancelarRechazo() = ui.update { it.copy(rechazando = false) }

    fun onComentarioChange(valor: String) = ui.update { it.copy(comentario = valor, comentarioError = null) }

    fun validar() {
        if (ui.value.procesando) return
        ui.update { it.copy(confirmandoValidar = false) }
        enviar { jornadaRepository.validar(jornadaId, usuarioId) }
    }

    fun rechazar() {
        val actual = ui.value
        if (actual.procesando) return
        val validacion = Validators.longitudMinima(actual.comentario.trim(), "El comentario", MIN_COMENTARIO)
        ui.update { it.copy(comentarioError = validacion.mensajeError) }
        if (!validacion.esValido) return
        enviar { jornadaRepository.rechazar(jornadaId, usuarioId, actual.comentario) }
    }

    private fun enviar(accion: suspend () -> Result<Unit>) {
        viewModelScope.launch {
            ui.update { it.copy(procesando = true, errorGeneral = null) }
            accion()
                .onSuccess { ui.update { it.copy(procesando = false, rechazando = false, revisada = true) } }
                .onFailure { e ->
                    ui.update { it.copy(procesando = false, rechazando = false, errorGeneral = e.message) }
                }
        }
    }

    private companion object {
        const val MIN_COMENTARIO = 10
    }
}
