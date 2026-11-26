package com.teqmed.teqruta.data.remote

import okhttp3.OkHttpClient
import org.json.JSONObject
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Cliente HTTP hacia la API. */
object ApiClient {

    val api: TeqRutaApi by lazy {
        val http = OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { cadena ->
                cadena.proceed(
                    cadena.request().newBuilder()
                        .header("X-API-Key", ApiConfig.API_KEY)
                        // ngrok gratis intercala una página de aviso en las llamadas que no traen este encabezado
                        .header("ngrok-skip-browser-warning", "true")
                        .build()
                )
            }
            .build()
        Retrofit.Builder()
            .baseUrl(ApiConfig.baseUrl)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TeqRutaApi::class.java)
    }
}

/** Mensaje apto para mostrar al usuario a partir de un error de red o de la API. */
fun mensajeDeError(e: Throwable): String = when (e) {
    is HttpException -> runCatching {
        JSONObject(e.response()?.errorBody()?.string().orEmpty()).getString("error")
    }.getOrNull() ?: "Error del servidor (${e.code()})"
    is IOException -> "Sin conexión con el servidor"
    else -> e.message ?: "Error desconocido"
}
