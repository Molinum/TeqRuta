package com.teqmed.teqruta.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
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
     * Ubicación actual, o null si no hay permiso, no hay señal o tarda más de 8 segundos.
     * En ese caso el registro continúa igual "sin ubicación" (RN-08).
     */
    @SuppressLint("MissingPermission")
    suspend fun obtenerUbicacion(): Coordenadas? {
        if (!tienePermiso()) return null
        return withTimeoutOrNull(8_000) {
            suspendCancellableCoroutine { continuacion ->
                cliente.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { ubicacion ->
                        continuacion.resume(ubicacion?.let { Coordenadas(it.latitude, it.longitude) })
                    }
                    .addOnFailureListener { continuacion.resume(null) }
            }
        }
    }
}
