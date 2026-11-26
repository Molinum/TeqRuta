package com.teqmed.teqruta.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.Tramo
import com.teqmed.teqruta.data.local.entity.TramoConNombres
import com.teqmed.teqruta.data.repository.AdminRepository
import com.teqmed.teqruta.domain.validation.Validators
import com.teqmed.teqruta.ui.util.formatearFecha
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CampoTramo { ORIGEN, DESTINO, KM, PEAJE, VIGENCIA }

data class TramoForm(
    /** Versión vigente que se está editando; null si se crea un tramo nuevo. */
    val anterior: TramoConNombres? = null,
    val origen: Destino? = null,
    val destino: Destino? = null,
    val km: String = "",
    val peaje: String = "",
    val vigenteDesde: String = "",
    /** Versiones del tramo, de la más nueva a la más antigua (RF-ADM-07). */
    val historial: List<TramoConNombres> = emptyList(),
    val errores: Map<CampoTramo, String> = emptyMap(),
    val errorGeneral: String? = null,
    val guardando: Boolean = false
)

data class TramosState(
    /** Última versión de cada tramo. */
    val tramos: List<TramoConNombres> = emptyList(),
    /** Todas las versiones de todos los tramos. */
    val versiones: List<TramoConNombres> = emptyList(),
    val destinos: List<Destino> = emptyList(),
    val form: TramoForm? = null
)

/** Mantenedor de tramos oficiales: editar crea una nueva versión con fecha de vigencia (RF-ADM-03, RF-ADM-04). */
class TramosViewModel(private val adminRepository: AdminRepository) : ViewModel() {

    private val form = MutableStateFlow<TramoForm?>(null)

    val estado: StateFlow<TramosState> = combine(
        adminRepository.observarTramos(), adminRepository.observarDestinos(), form
    ) { todas, destinos, formActual ->
        TramosState(
            // La lista viene ordenada de la versión más nueva a la más antigua de cada par
            tramos = todas.distinctBy { it.tramo.origenId to it.tramo.destinoId },
            versiones = todas,
            destinos = destinos.filter { it.activo },
            form = formActual
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TramosState())

    fun nuevo() {
        form.value = TramoForm(vigenteDesde = formatearFecha(System.currentTimeMillis()))
    }

    fun editar(t: TramoConNombres) {
        val destinos = estado.value.destinos
        form.value = TramoForm(
            anterior = t,
            origen = destinos.firstOrNull { it.id == t.tramo.origenId },
            destino = destinos.firstOrNull { it.id == t.tramo.destinoId },
            km = t.tramo.kmOficial.toString(),
            peaje = t.tramo.peaje.toString(),
            vigenteDesde = formatearFecha(System.currentTimeMillis()),
            historial = estado.value.versiones.filter {
                it.tramo.origenId == t.tramo.origenId && it.tramo.destinoId == t.tramo.destinoId
            }
        )
    }

    fun cerrarForm() { form.value = null }

    private fun cambiar(vararg campos: CampoTramo, cambio: (TramoForm) -> TramoForm) =
        form.update { f -> f?.let { cambio(it).copy(errores = it.errores - campos.toSet()) } }

    // Cambiar el origen también limpia el error del destino (que puede ser "igual al origen")
    fun onOrigenChange(v: Destino) = cambiar(CampoTramo.ORIGEN, CampoTramo.DESTINO) { it.copy(origen = v) }
    fun onDestinoChange(v: Destino) = cambiar(CampoTramo.DESTINO) { it.copy(destino = v) }
    fun onKmChange(v: String) = cambiar(CampoTramo.KM) { it.copy(km = v) }
    fun onPeajeChange(v: String) = cambiar(CampoTramo.PEAJE) { it.copy(peaje = v.filter(Char::isDigit)) }
    fun onVigenciaChange(v: String) = cambiar(CampoTramo.VIGENCIA) { it.copy(vigenteDesde = v) }

    fun guardar() {
        val f = form.value ?: return
        if (f.guardando) return
        val anterior = f.anterior?.tramo
        val errores = buildMap {
            agregar(CampoTramo.ORIGEN, Validators.seleccion(f.origen, "un origen"))
            agregar(CampoTramo.DESTINO, Validators.seleccion(f.destino, "un destino"))
            if (f.origen != null && f.destino != null) {
                if (f.origen.id == f.destino.id) {
                    put(CampoTramo.DESTINO, "El destino debe ser distinto del origen")
                } else if (anterior == null && estado.value.tramos.any {
                        it.tramo.origenId == f.origen.id && it.tramo.destinoId == f.destino.id
                    }
                ) {
                    put(CampoTramo.DESTINO, "Ya existe este tramo: edítalo para crear una nueva versión")
                }
            }
            agregar(CampoTramo.KM, Validators.decimalPositivo(f.km, "Los km oficiales"))
            agregar(CampoTramo.PEAJE, Validators.entero(f.peaje, "El peaje", minimo = 0))
            agregar(
                CampoTramo.VIGENCIA,
                Validators.fechaNoAnterior(
                    f.vigenteDesde, anterior?.vigenteDesde,
                    "la versión vigente (${anterior?.let { formatearFecha(it.vigenteDesde) }})"
                )
            )
        }
        if (errores.isNotEmpty()) {
            form.update { it?.copy(errores = errores) }
            return
        }
        val origen = f.origen ?: return
        val destino = f.destino ?: return
        viewModelScope.launch {
            form.update { it?.copy(guardando = true, errorGeneral = null) }
            adminRepository.guardarTramo(
                nuevo = Tramo(
                    origenId = origen.id,
                    destinoId = destino.id,
                    kmOficial = Validators.parsearDecimal(f.km) ?: 0.0,
                    peaje = f.peaje.trim().toIntOrNull() ?: 0,
                    vigenteDesde = Validators.parsearFecha(f.vigenteDesde) ?: System.currentTimeMillis()
                )
            )
                .onSuccess { form.value = null }
                .onFailure { e -> form.update { it?.copy(guardando = false, errorGeneral = e.message) } }
        }
    }
}
