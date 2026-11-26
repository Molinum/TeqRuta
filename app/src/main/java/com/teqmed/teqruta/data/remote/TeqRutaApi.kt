package com.teqmed.teqruta.data.remote

import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.TarifaCombustible
import com.teqmed.teqruta.data.local.entity.Tramo
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.data.local.entity.Vehiculo
import com.teqmed.teqruta.domain.model.EstadoJornada
import com.teqmed.teqruta.domain.model.MarcaTraslado
import com.teqmed.teqruta.domain.model.MotivoDesvio
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/** Endpoints de la API Flask (carpeta `server/`). Los nombres de campo coinciden con las entidades de Room. */
interface TeqRutaApi {

    @GET("catalogos")
    suspend fun catalogos(): CatalogosDto

    @POST("destinos")
    suspend fun guardarDestino(@Body destino: Destino): Destino

    @POST("vehiculos")
    suspend fun guardarVehiculo(@Body vehiculo: Vehiculo): Vehiculo

    @POST("usuarios")
    suspend fun guardarUsuario(@Body usuario: Usuario): Usuario

    /** Crea el tramo o, si ya existe para ese par, una nueva versión. */
    @POST("tramos")
    suspend fun guardarTramo(@Body tramo: Tramo): Tramo

    @POST("tarifas")
    suspend fun registrarTarifa(@Body tarifa: TarifaCombustible): TarifaCombustible

    /** Jornadas que el usuario puede ver, modificadas después de [desde] (milisegundos). */
    @GET("jornadas")
    suspend fun jornadas(@Query("usuarioId") usuarioId: Long, @Query("desde") desde: Long): JornadasDto

    @PUT("jornadas/{uuid}")
    suspend fun subirJornada(@Path("uuid") uuid: String, @Body jornada: JornadaDto): JornadaDto
}

data class CatalogosDto(
    val usuarios: List<Usuario>,
    val vehiculos: List<Vehiculo>,
    val destinos: List<Destino>,
    val tramos: List<Tramo>,
    val tarifas: List<TarifaCombustible>
)

data class JornadasDto(
    /** Hora del servidor al responder: la próxima descarga pide solo lo posterior. */
    val ahora: Long,
    val jornadas: List<JornadaDto>
)

/** Jornada con sus traslados, identificada por uuid (los ids locales no sirven entre dispositivos). */
data class JornadaDto(
    val uuid: String,
    val tecnicoId: Long,
    val vehiculoId: Long,
    val inicio: Long,
    val fin: Long?,
    val kmInicial: Int,
    val kmFinal: Int?,
    val fotoInicioUri: String?,
    val fotoCierreUri: String?,
    val latInicio: Double?,
    val lonInicio: Double?,
    val estado: EstadoJornada,
    val comentarioJefe: String?,
    val validacionAutomatica: Boolean,
    val actualizadoEn: Long?,
    val traslados: List<TrasladoDto>
)

data class TrasladoDto(
    val uuid: String,
    val orden: Int,
    val origenId: Long,
    val destinoId: Long,
    val tramoId: Long?,
    val horaSalida: Long,
    val horaLlegada: Long?,
    val kmInicio: Int,
    val kmFin: Int?,
    val latSalida: Double?,
    val lonSalida: Double?,
    val latLlegada: Double?,
    val lonLlegada: Double?,
    val fotoUri: String?,
    val kmOficialUsado: Double?,
    val peajeUsado: Int?,
    val precioLitroUsado: Double?,
    val rendimientoUsado: Double?,
    val costo: Double?,
    val desviacionPct: Double?,
    val marca: MarcaTraslado?,
    val motivoTipo: MotivoDesvio?,
    val motivoDetalle: String?
)
