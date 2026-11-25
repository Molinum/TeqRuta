package com.teqmed.teqruta.data.remote

/**
 * Configuración del servidor. Para conectar la app a la API de Flask cambia solo [API_URL]:
 *
 *  - ngrok:      "https://xxxx-xx-xx.ngrok-free.app"  (la URL que muestra `server/run.sh --ngrok`)
 *  - emulador:   "http://10.0.2.2:5000"               (Flask corriendo en tu computador)
 *
 * Con [API_URL] vacío la app funciona solo con la base de datos local (Room), sin red.
 */
object ApiConfig {

    const val API_URL = ""

    /** Debe coincidir con TEQRUTA_API_KEY del servidor (por defecto "teqruta-dev-key"). */
    const val API_KEY = "teqruta-dev-key"

    val habilitada: Boolean get() = API_URL.isNotBlank()

    val baseUrl: String get() = API_URL.trim().trimEnd('/') + "/api/"
}
