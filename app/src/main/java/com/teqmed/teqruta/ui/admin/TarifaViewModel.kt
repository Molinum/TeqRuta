package com.teqmed.teqruta.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.TarifaCombustible
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

enum class CampoTarifa { PRECIO, VIGENCIA }

data class TarifaForm(
    val precio: String = "",
    val vigenteDesde: String = "",
    val errores: Map<CampoTarifa, String> = emptyMap(),
    val errorGeneral: String? = null,
    val guardando: Boolean = false
)

data class TarifaState(
    /** Historial de precios, del más reciente al más antiguo. */
    val tarifas: List<TarifaCombustible> = emptyList(),
    /** Tarifa que rige hoy; las posteriores a hoy están programadas. */
    val vigenteId: Long? = null,
    val form: TarifaForm? = null
)

/** Registro del precio del combustible por litro con fecha de vigencia (RF-ADM-05). */
class TarifaViewModel(private val adminRepository: AdminRepository) : ViewModel() {

    private val form = MutableStateFlow<TarifaForm?>(null)

    val estado: StateFlow<TarifaState> = combine(adminRepository.observarTarifas(), form) { tarifas, formActual ->
        val ahora = System.currentTimeMillis()
        TarifaState(
            tarifas = tarifas,
            vigenteId = tarifas.firstOrNull { it.vigenteDesde <= ahora }?.id,
            form = formActual
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TarifaState())

    fun nuevo() {
        form.value = TarifaForm(vigenteDesde = formatearFecha(System.currentTimeMillis()))
    }

    fun cerrarForm() { form.value = null }

    fun onPrecioChange(v: String) =
        form.update { f -> f?.copy(precio = v, errores = f.errores - CampoTarifa.PRECIO) }

    fun onVigenciaChange(v: String) =
        form.update { f -> f?.copy(vigenteDesde = v, errores = f.errores - CampoTarifa.VIGENCIA) }

    fun guardar() {
        val f = form.value ?: return
        if (f.guardando) return
        val ultima = estado.value.tarifas.firstOrNull()
        val errores = buildMap {
            agregar(CampoTarifa.PRECIO, Validators.decimalPositivo(f.precio, "El precio"))
            agregar(
                CampoTarifa.VIGENCIA,
                Validators.fechaNoAnterior(
                    f.vigenteDesde, ultima?.vigenteDesde,
                    "la tarifa anterior (${ultima?.let { formatearFecha(it.vigenteDesde) }})"
                )
            )
        }
        if (errores.isNotEmpty()) {
            form.update { it?.copy(errores = errores) }
            return
        }
        viewModelScope.launch {
            form.update { it?.copy(guardando = true, errorGeneral = null) }
            adminRepository.registrarTarifa(
                TarifaCombustible(
                    precioLitro = Validators.parsearDecimal(f.precio) ?: 0.0,
                    vigenteDesde = Validators.parsearFecha(f.vigenteDesde) ?: System.currentTimeMillis()
                )
            )
                .onSuccess { form.value = null }
                .onFailure { e -> form.update { it?.copy(guardando = false, errorGeneral = e.message) } }
        }
    }
}
