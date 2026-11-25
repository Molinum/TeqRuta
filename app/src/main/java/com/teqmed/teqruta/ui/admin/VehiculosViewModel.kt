package com.teqmed.teqruta.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.Vehiculo
import com.teqmed.teqruta.data.repository.AdminRepository
import com.teqmed.teqruta.domain.model.TipoVehiculo
import com.teqmed.teqruta.domain.validation.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CampoVehiculo { PATENTE, DESCRIPCION, TIPO, RENDIMIENTO }

data class VehiculoForm(
    val id: Long = 0,
    val patente: String = "",
    val descripcion: String = "",
    val tipo: TipoVehiculo? = null,
    val rendimiento: String = "",
    val activo: Boolean = true,
    val errores: Map<CampoVehiculo, String> = emptyMap(),
    val errorGeneral: String? = null,
    val guardando: Boolean = false
)

data class VehiculosState(
    val vehiculos: List<Vehiculo> = emptyList(),
    val form: VehiculoForm? = null,
    val porCambiar: Vehiculo? = null,
    val mensaje: String? = null
)

/** Mantenedor de vehículos (RF-ADM-02). */
class VehiculosViewModel(private val adminRepository: AdminRepository) : ViewModel() {

    private val form = MutableStateFlow<VehiculoForm?>(null)
    private val porCambiar = MutableStateFlow<Vehiculo?>(null)
    private val mensaje = MutableStateFlow<String?>(null)

    val estado: StateFlow<VehiculosState> = combine(
        adminRepository.observarVehiculos(), form, porCambiar, mensaje, ::VehiculosState
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), VehiculosState())

    fun nuevo() { form.value = VehiculoForm() }

    fun editar(v: Vehiculo) {
        form.value = VehiculoForm(
            id = v.id, patente = v.patente, descripcion = v.descripcion, tipo = v.tipo,
            rendimiento = v.rendimientoKmL.toString(), activo = v.activo
        )
    }

    fun cerrarForm() { form.value = null }

    private fun cambiar(campo: CampoVehiculo, cambio: (VehiculoForm) -> VehiculoForm) =
        form.update { f -> f?.let { cambio(it).copy(errores = it.errores - campo) } }

    fun onPatenteChange(v: String) = cambiar(CampoVehiculo.PATENTE) { it.copy(patente = v.uppercase()) }
    fun onDescripcionChange(v: String) = cambiar(CampoVehiculo.DESCRIPCION) { it.copy(descripcion = v) }
    fun onTipoChange(v: TipoVehiculo) = cambiar(CampoVehiculo.TIPO) { it.copy(tipo = v) }
    fun onRendimientoChange(v: String) = cambiar(CampoVehiculo.RENDIMIENTO) { it.copy(rendimiento = v) }

    fun guardar() {
        val f = form.value ?: return
        if (f.guardando) return
        val patentes = estado.value.vehiculos.filter { it.id != f.id }.map { it.patente }
        val errores = buildMap {
            agregar(
                CampoVehiculo.PATENTE,
                Validators.primerError(
                    Validators.patente(f.patente),
                    Validators.sinDuplicado(
                        Validators.normalizarPatente(f.patente), patentes, "Ya existe un vehículo con esa patente"
                    )
                )
            )
            agregar(CampoVehiculo.DESCRIPCION, Validators.requerido(f.descripcion, "Descripción"))
            agregar(CampoVehiculo.TIPO, Validators.seleccion(f.tipo, "un tipo"))
            agregar(CampoVehiculo.RENDIMIENTO, Validators.decimalPositivo(f.rendimiento, "El rendimiento"))
        }
        if (errores.isNotEmpty()) {
            form.update { it?.copy(errores = errores) }
            return
        }
        viewModelScope.launch {
            form.update { it?.copy(guardando = true, errorGeneral = null) }
            adminRepository.guardarVehiculo(
                Vehiculo(
                    id = f.id, patente = Validators.normalizarPatente(f.patente), descripcion = f.descripcion.trim(),
                    tipo = f.tipo ?: TipoVehiculo.EMPRESA,
                    rendimientoKmL = Validators.parsearDecimal(f.rendimiento) ?: 1.0, activo = f.activo
                )
            )
                .onSuccess { form.value = null }
                .onFailure { e -> form.update { it?.copy(guardando = false, errorGeneral = e.message) } }
        }
    }

    fun pedirCambioActivo(v: Vehiculo) {
        mensaje.value = null
        porCambiar.value = v
    }

    fun cancelarCambioActivo() { porCambiar.value = null }

    fun confirmarCambioActivo() {
        val v = porCambiar.value ?: return
        porCambiar.value = null
        viewModelScope.launch {
            adminRepository.guardarVehiculo(v.copy(activo = !v.activo)).onFailure { mensaje.value = it.message }
        }
    }
}
