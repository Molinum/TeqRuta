package com.teqmed.teqruta.data.remote

/**
 * Configuración del servidor. La app lee y escribe todos sus datos a través de la API de Flask
 * (carpeta `server/`); para apuntarla a otro servidor cambia solo [API_URL]:
 *
 *  - emulador:   "http://10.0.2.2:5000"               (Flask corriendo en tu computador, valor por defecto)
 *  - ngrok:      "https://xxxx-xx-xx.ngrok-free.app"  (la URL que muestra `server/run.sh --ngrok`)
 */
object ApiConfig {

    const val API_URL = "http://10.0.2.2:5000"

    /** Debe coincidir con TEQRUTA_API_KEY del servidor (por defecto "teqruta-dev-key"). */
    const val API_KEY = "teqruta-dev-key"

    val baseUrl: String get() = API_URL.trim().trimEnd('/') + "/api/"
}
