package com.teqmed.teqruta.domain.validation

/** Resultado de validar un campo: válido o inválido con el mensaje a mostrar al usuario. */
sealed interface ValidationResult {
    data object Valid : ValidationResult
    data class Invalid(val mensaje: String) : ValidationResult
}

val ValidationResult.esValido: Boolean get() = this is ValidationResult.Valid

/** Mensaje de error para la UI, o null si el campo es válido. */
val ValidationResult.mensajeError: String? get() = (this as? ValidationResult.Invalid)?.mensaje
