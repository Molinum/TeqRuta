package com.teqmed.teqruta.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.teqmed.teqruta.data.repository.Coordenadas
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Obtiene la ubicación actual del dispositivo con el GPS (RF-TEC-03). */
class LocationProvider(private val context: Context) {

    private val cliente = LocationServices.getFusedLocationProviderClient(context)

    fun tienePermiso(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Ubicación actual, o null si no hay permiso, no hay señal o tarda más de 3 segundos.
     * En ese caso el registro continúa igual "sin ubicación" (RN-08). Basta con una ubicación
     * de hasta 30 segundos de antigüedad: es evidencia de presencia, no navegación, y así el
     * técnico no espera un fix de GPS en cada registro.
     */
    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacion(): Coordenadas? {
        if (!tienePermiso()) return null
        val solicitud = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
            .setMaxUpdateAgeMillis(MAX_ANTIGUEDAD_MS)
            .setDurationMillis(ESPERA_MAXIMA_MS)
            .build()
        return withTimeoutOrNull(ESPERA_MAXIMA_MS + 500) {
            suspendCancellableCoroutine { continuacion ->
                cliente.getCurrentLocation(solicitud, null)
                    .addOnSuccessListener { ubicacion ->
                        continuacion.resume(ubicacion?.let { Coordenadas(it.latitude, it.longitude) })
                    }
                    .addOnFailureListener { continuacion.resume(null) }
            }
        }
    }

    private companion object {
        const val MAX_ANTIGUEDAD_MS = 30_000L
        const val ESPERA_MAXIMA_MS = 3_000L
    }
}
