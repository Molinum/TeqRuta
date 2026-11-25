package com.teqmed.teqruta.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.repository.AdminRepository
import com.teqmed.teqruta.domain.model.TipoDestino
import com.teqmed.teqruta.domain.validation.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CampoDestino { NOMBRE, DIRECCION, CIUDAD, LAT, LON, TIPO }

data class DestinoForm(
    val id: Long = 0,
    val nombre: String = "",
    val direccion: String = "",
    val ciudad: String = "",
    val lat: String = "",
    val lon: String = "",
    val tipo: TipoDestino? = null,
    val activo: Boolean = true,
    val errores: Map<CampoDestino, String> = emptyMap(),
    val errorGeneral: String? = null,
    val guardando: Boolean = false
)

data class DestinosState(
    val destinos: List<Destino> = emptyList(),
    val form: DestinoForm? = null,
    /** Destino que se va a activar o desactivar, pendiente de confirmación. */
    val porCambiar: Destino? = null,
    val mensaje: String? = null
)

/** Mantenedor de destinos: la sede y las clínicas u hospitales (RF-ADM-01). */
class DestinosViewModel(private val adminRepository: AdminRepository) : ViewModel() {

    private val form = MutableStateFlow<DestinoForm?>(null)
    private val porCambiar = MutableStateFlow<Destino?>(null)
    private val mensaje = MutableStateFlow<String?>(null)

    val estado: StateFlow<DestinosState> = combine(
        adminRepository.observarDestinos(), form, porCambiar, mensaje, ::DestinosState
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DestinosState())

    fun nuevo() {
        mensaje.value = null
        form.value = DestinoForm()
    }

    fun editar(d: Destino) {
        mensaje.value = null
        form.value = DestinoForm(
            id = d.id, nombre = d.nombre, direccion = d.direccion, ciudad = d.ciudad,
            lat = d.lat.toString(), lon = d.lon.toString(), tipo = d.tipo, activo = d.activo
        )
    }

    fun cerrarForm() { form.value = null }

    private fun cambiar(campo: CampoDestino, cambio: (DestinoForm) -> DestinoForm) =
        form.update { f -> f?.let { cambio(it).copy(errores = it.errores - campo) } }

    fun onNombreChange(v: String) = cambiar(CampoDestino.NOMBRE) { it.copy(nombre = v) }
    fun onDireccionChange(v: String) = cambiar(CampoDestino.DIRECCION) { it.copy(direccion = v) }
    fun onCiudadChange(v: String) = cambiar(CampoDestino.CIUDAD) { it.copy(ciudad = v) }
    fun onLatChange(v: String) = cambiar(CampoDestino.LAT) { it.copy(lat = v) }
    fun onLonChange(v: String) = cambiar(CampoDestino.LON) { it.copy(lon = v) }
    fun onTipoChange(v: TipoDestino) = cambiar(CampoDestino.TIPO) { it.copy(tipo = v) }

    fun guardar() {
        val f = form.value ?: return
        if (f.guardando) return
        val otros = estado.value.destinos.filter { it.id != f.id }
        val errores = buildMap {
            agregar(
                CampoDestino.NOMBRE,
                Validators.primerError(
                    Validators.requerido(f.nombre, "Nombre"),
                    Validators.sinDuplicado(f.nombre, otros.map { it.nombre }, "Ya existe un destino con ese nombre")
                )
            )
            agregar(CampoDestino.DIRECCION, Validators.requerido(f.direccion, "Dirección"))
            agregar(CampoDestino.CIUDAD, Validators.requerido(f.ciudad, "Ciudad"))
            agregar(CampoDestino.LAT, Validators.coordenada(f.lat, "La latitud", -90.0, 90.0))
            agregar(CampoDestino.LON, Validators.coordenada(f.lon, "La longitud", -180.0, 180.0))
            agregar(CampoDestino.TIPO, Validators.seleccion(f.tipo, "un tipo"))
            // La app necesita exactamente una sede activa para registrar jornadas (RN-09)
            if (f.tipo == TipoDestino.SEDE && f.activo && otros.any { it.tipo == TipoDestino.SEDE && it.activo }) {
                put(CampoDestino.TIPO, "Ya existe una sede activa")
            }
            if (f.tipo == TipoDestino.CLINICA && f.id != 0L &&
                estado.value.destinos.any { it.id == f.id && it.tipo == TipoDestino.SEDE && it.activo } &&
                otros.none { it.tipo == TipoDestino.SEDE && it.activo }
            ) {
                put(CampoDestino.TIPO, "Debe existir una sede activa")
            }
        }
        if (errores.isNotEmpty()) {
            form.update { it?.copy(errores = errores) }
            return
        }
        viewModelScope.launch {
            form.update { it?.copy(guardando = true, errorGeneral = null) }
            adminRepository.guardarDestino(
                Destino(
                    id = f.id, nombre = f.nombre.trim(), direccion = f.direccion.trim(), ciudad = f.ciudad.trim(),
                    lat = Validators.parsearDecimal(f.lat) ?: 0.0, lon = Validators.parsearDecimal(f.lon) ?: 0.0,
                    tipo = f.tipo ?: TipoDestino.CLINICA, activo = f.activo
                )
            )
                .onSuccess { form.value = null }
                .onFailure { e -> form.update { it?.copy(guardando = false, errorGeneral = e.message) } }
        }
    }

    fun pedirCambioActivo(d: Destino) {
        mensaje.value = null
        porCambiar.value = d
    }

    fun cancelarCambioActivo() { porCambiar.value = null }

    fun confirmarCambioActivo() {
        val d = porCambiar.value ?: return
        porCambiar.value = null
        val otrasSedesActivas = estado.value.destinos.any { it.id != d.id && it.tipo == TipoDestino.SEDE && it.activo }
        when {
            d.tipo == TipoDestino.SEDE && d.activo && !otrasSedesActivas ->
                mensaje.value = "No puedes desactivar la única sede activa"
            d.tipo == TipoDestino.SEDE && !d.activo && otrasSedesActivas ->
                mensaje.value = "Ya existe una sede activa"
            else -> viewModelScope.launch {
                adminRepository.guardarDestino(d.copy(activo = !d.activo)).onFailure { mensaje.value = it.message }
            }
        }
    }
}
