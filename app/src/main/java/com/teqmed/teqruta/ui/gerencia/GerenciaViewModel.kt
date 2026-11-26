package com.teqmed.teqruta.ui.gerencia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teqmed.teqruta.data.repository.ReporteRepository
import com.teqmed.teqruta.domain.reporte.AgrupadoPor
import com.teqmed.teqruta.domain.reporte.FilaReporte
import com.teqmed.teqruta.domain.reporte.LineaCosto
import com.teqmed.teqruta.domain.reporte.Mes
import com.teqmed.teqruta.domain.reporte.ReporteCostos
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

data class GerenciaState(
    val cargando: Boolean = true,
    val mes: Mes = Mes.actual(),
    /** No se puede avanzar más allá del mes en curso. */
    val puedeAvanzar: Boolean = false,
    val agrupadoPor: AgrupadoPor = AgrupadoPor.CLIENTE,
    val filas: List<FilaReporte> = emptyList(),
    val costoTotal: Double = 0.0,
    val traslados: Int = 0,
    val km: Int = 0,
    /** Fila abierta para ver los traslados que la componen (RF-GER-05). */
    val filaAbierta: Long? = null
)

/** Reporte de costo de traslados por cliente, técnico o vehículo en un mes (solo jornadas validadas). */
@OptIn(ExperimentalCoroutinesApi::class)
class GerenciaViewModel(reporteRepository: ReporteRepository) : ViewModel() {

    private val mes = MutableStateFlow(Mes.actual())
    private val criterio = MutableStateFlow(AgrupadoPor.CLIENTE)
    private val filaAbierta = MutableStateFlow<Long?>(null)

    private val lineas: StateFlow<List<LineaCosto>?> = mes
        .flatMapLatest { reporteRepository.observarLineas(it.desde, it.hasta) }
        .map<List<LineaCosto>, List<LineaCosto>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val estado: StateFlow<GerenciaState> = combine(lineas, mes, criterio, filaAbierta) { datos, mesActual, agrupado, abierta ->
        val l = datos.orEmpty()
        GerenciaState(
            cargando = datos == null,
            mes = mesActual,
            puedeAvanzar = mesActual != Mes.actual(),
            agrupadoPor = agrupado,
            filas = ReporteCostos.agrupar(l, agrupado),
            costoTotal = ReporteCostos.costoTotal(l),
            traslados = l.size,
            km = l.sumOf { it.kmDeclarados },
            filaAbierta = abierta
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GerenciaState())

    fun mesAnterior() = cambiarMes { it.anterior() }

    fun mesSiguiente() = cambiarMes { if (it != Mes.actual()) it.siguiente() else it }

    private fun cambiarMes(cambio: (Mes) -> Mes) {
        mes.update(cambio)
        filaAbierta.value = null
    }

    fun onAgrupadoChange(nuevo: AgrupadoPor) {
        criterio.value = nuevo
        filaAbierta.value = null
    }

    fun onFilaClick(clave: Long) = filaAbierta.update { if (it == clave) null else clave }
}
