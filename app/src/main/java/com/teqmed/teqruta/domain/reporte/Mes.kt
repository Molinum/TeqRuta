package com.teqmed.teqruta.domain.reporte

import java.util.Calendar
import java.util.Locale

/** Mes calendario de un reporte. [mes] va de 0 (enero) a 11 (diciembre), como en [Calendar]. */
data class Mes(val anio: Int, val mes: Int) {

    /** Primer instante del mes, en milisegundos. */
    val desde: Long get() = calendario(anio, mes).timeInMillis

    /** Primer instante del mes siguiente (límite exclusivo del período). */
    val hasta: Long get() = siguiente().desde

    fun anterior(): Mes = if (mes == 0) Mes(anio - 1, 11) else Mes(anio, mes - 1)

    fun siguiente(): Mes = if (mes == 11) Mes(anio + 1, 0) else Mes(anio, mes + 1)

    /** "noviembre 2026" */
    val etiqueta: String get() = "${NOMBRES[mes]} $anio"

    companion object {
        private val NOMBRES = listOf(
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
        )

        fun actual(): Mes = Calendar.getInstance(Locale.US).let { Mes(it.get(Calendar.YEAR), it.get(Calendar.MONTH)) }

        private fun calendario(anio: Int, mes: Int) = Calendar.getInstance().apply {
            clear()
            set(anio, mes, 1, 0, 0, 0)
        }
    }
}
