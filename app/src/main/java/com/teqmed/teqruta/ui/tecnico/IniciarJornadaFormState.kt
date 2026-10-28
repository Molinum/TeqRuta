package com.teqmed.teqruta.ui.tecnico

import com.teqmed.teqruta.data.local.entity.Vehiculo

data class IniciarJornadaFormState(
    val vehiculos: List<Vehiculo> = emptyList(),
    val sedeNombre: String = "",
    val vehiculo: Vehiculo? = null,
    val kmInicial: String = "",
    /** Km final de la última jornada del vehículo elegido, si existe. */
    val ultimoKm: Int? = null,
    /** Ruta de la foto del odómetro, obligatoria al iniciar (RN-07). */
    val fotoPath: String? = null,
    val vehiculoError: String? = null,
    val fotoError: String? = null,
    val kmError: String? = null,
    val errorGeneral: String? = null,
    val guardando: Boolean = false,
    /** true cuando la jornada se creó; la pantalla vuelve al inicio. */
    val guardado: Boolean = false
)
