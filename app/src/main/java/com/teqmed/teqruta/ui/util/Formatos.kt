package com.teqmed.teqruta.ui.util

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToLong

private val LOCALE_CL: Locale = Locale.forLanguageTag("es-CL")

/** $12.345 */
fun formatearPesos(monto: Double): String =
    "$" + NumberFormat.getIntegerInstance(LOCALE_CL).format(monto.roundToLong())

/** 45.210 km */
fun formatearKm(km: Int): String = NumberFormat.getIntegerInstance(LOCALE_CL).format(km) + " km"

/** 108,5 km */
fun formatearKmDecimal(km: Double): String =
    NumberFormat.getNumberInstance(LOCALE_CL).apply { maximumFractionDigits = 1 }.format(km) + " km"

/** 14:35 */
fun formatearHora(millis: Long): String = SimpleDateFormat("HH:mm", LOCALE_CL).format(Date(millis))

/** 28-10-2026 */
fun formatearFecha(millis: Long): String = SimpleDateFormat("dd-MM-yyyy", LOCALE_CL).format(Date(millis))
