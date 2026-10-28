package com.teqmed.teqruta

import android.app.Application
import com.teqmed.teqruta.data.local.AppDatabase
import com.teqmed.teqruta.data.repository.AuthRepository
import com.teqmed.teqruta.data.repository.CatalogoRepository
import com.teqmed.teqruta.data.repository.JornadaRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Punto único donde se crean la base de datos y los repositorios (inyección de
 * dependencias manual). Los ViewModels los reciben a través de ViewModelFactory.
 */
class TeqRutaApp : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: AppDatabase by lazy { AppDatabase.obtener(this) }

    val authRepository by lazy { AuthRepository(database.usuarioDao()) }

    val catalogoRepository by lazy {
        CatalogoRepository(
            database.destinoDao(),
            database.vehiculoDao(),
            database.tramoDao(),
            database.tarifaDao()
        )
    }

    val jornadaRepository by lazy {
        JornadaRepository(database.jornadaDao(), database.trasladoDao())
    }

    override fun onCreate() {
        super.onCreate()
        // Room abre la BD de forma diferida; se abre al inicio para que la precarga
        // de datos ocurra antes de que el usuario intente iniciar sesión.
        appScope.launch { database.openHelper.writableDatabase }
    }
}
