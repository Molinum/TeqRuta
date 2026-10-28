package com.teqmed.teqruta.domain.validation

import com.teqmed.teqruta.domain.validation.ValidationResult.Invalid
import com.teqmed.teqruta.domain.validation.ValidationResult.Valid

/** Reglas de negocio de los formularios de jornada y traslados (RN-05). */
object JornadaValidator {

    data class ResultadoInicio(val vehiculo: ValidationResult, val kmInicial: ValidationResult) {
        val esValido get() = vehiculo.esValido && kmInicial.esValido
    }

    /**
     * @param ultimoKmVehiculo km final de la última jornada cerrada del vehículo, si existe.
     */
    fun validarInicio(vehiculoId: Long?, kmInicial: String, ultimoKmVehiculo: Int?): ResultadoInicio {
        val km = Validators.entero(kmInicial, "El km inicial", minimo = 0)
        val kmResultado = when {
            !km.esValido -> km
            ultimoKmVehiculo != null && kmInicial.trim().toInt() < ultimoKmVehiculo ->
                Invalid("No puede ser menor al último km registrado ($ultimoKmVehiculo)")
            else -> Valid
        }
        return ResultadoInicio(
            vehiculo = Validators.seleccion(vehiculoId, "un vehículo"),
            kmInicial = kmResultado
        )
    }

    /** El km de llegada debe ser mayor que el km con que salió el traslado. */
    fun validarKmLlegada(km: String, kmSalida: Int): ValidationResult {
        val resultado = Validators.entero(km, "El km", minimo = 0)
        if (!resultado.esValido) return resultado
        return if (km.trim().toInt() > kmSalida) Valid
        else Invalid("Debe ser mayor al km de salida ($kmSalida)")
    }
}
