package com.teqmed.teqruta

import android.app.Application
import com.teqmed.teqruta.data.remote.ApiClient
import com.teqmed.teqruta.data.remote.DatosRemotos
import com.teqmed.teqruta.data.repository.AdminRepository
import com.teqmed.teqruta.data.repository.AuthRepository
import com.teqmed.teqruta.data.repository.CatalogoRepository
import com.teqmed.teqruta.data.repository.JornadaRepository
import com.teqmed.teqruta.data.repository.ReporteRepository
import com.teqmed.teqruta.device.LocationProvider

/**
 * Punto único donde se crean los repositorios (inyección de dependencias manual). Los
 * ViewModels los reciben a través de ViewModelFactory.
 *
 * Todos los datos se leen y escriben a través de la API REST ([DatosRemotos]). La base local
 * Room (data/local) queda en el proyecto pero la app no la usa por ahora.
 */
class TeqRutaApp : Application() {

    val datos by lazy { DatosRemotos { ApiClient.api } }

    val authRepository by lazy { AuthRepository { ApiClient.api } }

    val catalogoRepository by lazy { CatalogoRepository(datos) }

    val jornadaRepository by lazy { JornadaRepository(datos, catalogoRepository) }

    val reporteRepository by lazy { ReporteRepository(datos) }

    val adminRepository by lazy { AdminRepository({ ApiClient.api }, datos) }

    val locationProvider by lazy { LocationProvider(this) }
}
