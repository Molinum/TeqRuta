package com.teqmed.teqruta.data.repository

/**
 * Lo que los repositorios necesitan saber de la sincronización con el servidor. Con la API
 * sin configurar se usa [SinSincronizacion] y la app funciona solo con Room.
 */
interface Sincronizador {

    /** Usuario con sesión iniciada: define qué jornadas se descargan (RN-15). */
    var usuarioId: Long?

    /** Pide una sincronización en segundo plano; no bloquea ni falla si no hay conexión. */
    fun solicitar()

    /** Descarga el catálogo del servidor. Falla si no hay conexión o la API no está configurada. */
    suspend fun descargarCatalogos(): Result<Unit>
}

object SinSincronizacion : Sincronizador {
    override var usuarioId: Long? = null
    override fun solicitar() = Unit
    override suspend fun descargarCatalogos(): Result<Unit> =
        Result.failure(IllegalStateException("La API no está configurada"))
}
