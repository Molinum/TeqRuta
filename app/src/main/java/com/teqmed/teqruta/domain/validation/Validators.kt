package com.teqmed.teqruta.domain.validation

import com.teqmed.teqruta.domain.validation.ValidationResult.Invalid
import com.teqmed.teqruta.domain.validation.ValidationResult.Valid

/**
 * Reglas de validación reutilizables por todos los formularios. Kotlin puro: los ViewModels
 * las usan y los Composables solo muestran el mensaje resultante.
 */
object Validators {

    const val CAMPO_OBLIGATORIO = "Este campo es obligatorio"

    private val REGEX_CORREO = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /** Formato antiguo (AB1234) y nuevo (ABCD12) de patentes chilenas. */
    private val REGEX_PATENTE = Regex("^([A-Z]{2}\\d{4}|[A-Z]{4}\\d{2})$")

    fun requerido(valor: String, campo: String): ValidationResult =
        if (valor.isBlank()) Invalid(CAMPO_OBLIGATORIO) else Valid

    fun seleccion(valor: Any?, campo: String): ValidationResult =
        if (valor == null) Invalid("Selecciona $campo") else Valid

    fun correo(valor: String): ValidationResult = when {
        valor.isBlank() -> Invalid("El correo es obligatorio")
        !REGEX_CORREO.matches(valor.trim()) -> Invalid("Ingresa un correo válido")
        else -> Valid
    }

    fun longitudMinima(valor: String, campo: String, minimo: Int): ValidationResult = when {
        valor.isBlank() -> Invalid(CAMPO_OBLIGATORIO)
        valor.length < minimo -> Invalid("$campo debe tener al menos $minimo caracteres")
        else -> Valid
    }

    /** Número entero, opcionalmente con un valor mínimo (inclusive). */
    fun entero(valor: String, campo: String, minimo: Int? = null): ValidationResult {
        if (valor.isBlank()) return Invalid(CAMPO_OBLIGATORIO)
        val numero = valor.trim().toIntOrNull() ?: return Invalid("Ingresa un número entero")
        if (minimo != null && numero < minimo) return Invalid("$campo debe ser al menos $minimo")
        return Valid
    }

    /** Número decimal estrictamente mayor a 0. Acepta coma o punto como separador. */
    fun decimalPositivo(valor: String, campo: String): ValidationResult {
        if (valor.isBlank()) return Invalid(CAMPO_OBLIGATORIO)
        val numero = valor.trim().replace(',', '.').toDoubleOrNull()
            ?: return Invalid("Ingresa un número válido")
        return if (numero > 0) Valid else Invalid("$campo debe ser mayor a 0")
    }

    fun patente(valor: String): ValidationResult {
        if (valor.isBlank()) return Invalid("La patente es obligatoria")
        val normalizada = normalizarPatente(valor)
        return if (REGEX_PATENTE.matches(normalizada)) Valid
        else Invalid("Formato inválido (ej. AB1234 o ABCD12)")
    }

    /** Quita guiones, puntos y espacios y pasa a mayúsculas: "ab-cd·12" → "ABCD12". */
    fun normalizarPatente(valor: String): String =
        valor.uppercase().filter { it.isLetterOrDigit() }

    /** Devuelve el primer resultado inválido, o Valid si todos lo son. */
    fun primerError(vararg resultados: ValidationResult): ValidationResult =
        resultados.firstOrNull { it is Invalid } ?: Valid
}
