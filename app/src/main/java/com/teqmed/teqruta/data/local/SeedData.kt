package com.teqmed.teqruta.data.local

import androidx.room.withTransaction
import com.teqmed.teqruta.data.local.entity.Destino
import com.teqmed.teqruta.data.local.entity.TarifaCombustible
import com.teqmed.teqruta.data.local.entity.Tramo
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.data.local.entity.Vehiculo
import com.teqmed.teqruta.domain.auth.PasswordHasher
import com.teqmed.teqruta.domain.model.Rol
import com.teqmed.teqruta.domain.model.TipoDestino
import com.teqmed.teqruta.domain.model.TipoVehiculo

/**
 * Datos de ejemplo para usar la app desde la primera ejecución.
 * Coordenadas y distancias son aproximadas.
 *
 * Usuarios de prueba (correo / contraseña):
 *  - admin@teqmed.cl / admin123
 *  - jefe@teqmed.cl / jefe123
 *  - gerencia@teqmed.cl / gerencia123
 *  - tecnico@teqmed.cl / tecnico123
 *  - tecnico2@teqmed.cl / tecnico123
 *  - inactivo@teqmed.cl / inactivo123 (desactivado)
 */
object SeedData {

    /** 01-01-2026 00:00 UTC, inicio de vigencia de tramos y tarifa. */
    private const val INICIO_2026 = 1_767_225_600_000L

    suspend fun poblar(db: AppDatabase) = db.withTransaction {
        poblarUsuarios(db)
        poblarVehiculos(db)
        poblarDestinosYTramos(db)
        db.tarifaDao().insertar(TarifaCombustible(precioLitro = 1290.0, vigenteDesde = INICIO_2026))
    }

    private suspend fun poblarUsuarios(db: AppDatabase) {
        val dao = db.usuarioDao()
        dao.insertar(usuario("Ana Pérez", "admin@teqmed.cl", "admin123", Rol.ADMIN))
        val jefeId = dao.insertar(usuario("Jorge Soto", "jefe@teqmed.cl", "jefe123", Rol.JEFE))
        dao.insertar(usuario("Carla Muñoz", "gerencia@teqmed.cl", "gerencia123", Rol.GERENCIA))
        dao.insertar(usuario("Diego Rojas", "tecnico@teqmed.cl", "tecnico123", Rol.TECNICO, jefeId))
        dao.insertar(usuario("Felipe Vera", "tecnico2@teqmed.cl", "tecnico123", Rol.TECNICO, jefeId))
        dao.insertar(
            usuario("Pablo Díaz", "inactivo@teqmed.cl", "inactivo123", Rol.TECNICO, jefeId)
                .copy(activo = false)
        )
    }

    private fun usuario(nombre: String, email: String, password: String, rol: Rol, jefeId: Long? = null) =
        Usuario(
            nombre = nombre,
            email = email,
            passwordHash = PasswordHasher.hash(password),
            rol = rol,
            jefeId = jefeId
        )

    private suspend fun poblarVehiculos(db: AppDatabase) {
        val dao = db.vehiculoDao()
        dao.insertar(Vehiculo(patente = "RTKJ45", descripcion = "Toyota Hilux 2.4", tipo = TipoVehiculo.EMPRESA, rendimientoKmL = 10.5))
        dao.insertar(Vehiculo(patente = "PXLV12", descripcion = "Nissan NP300", tipo = TipoVehiculo.EMPRESA, rendimientoKmL = 11.2))
        dao.insertar(Vehiculo(patente = "LKHP83", descripcion = "Suzuki Swift", tipo = TipoVehiculo.PARTICULAR, rendimientoKmL = 16.0))
    }

    private suspend fun poblarDestinosYTramos(db: AppDatabase) {
        val destinos = db.destinoDao()
        val sede = destinos.insertar(Destino(nombre = "Sede TEQMED", direccion = "Av. Arturo Prat 390", ciudad = "Concepción", lat = -36.8285, lon = -73.0605, tipo = TipoDestino.SEDE))
        val sanatorio = destinos.insertar(clinica("Clínica Sanatorio Alemán", "Av. Pedro de Valdivia 801", "Concepción", -36.8360, -73.0430))
        val regional = destinos.insertar(clinica("Hospital Regional Guillermo Grant Benavente", "San Martín 1436", "Concepción", -36.8190, -73.0390))
        val biobio = destinos.insertar(clinica("Clínica Biobío", "Av. Jorge Alessandri 3515", "Talcahuano", -36.7880, -73.0790))
        val herminda = destinos.insertar(clinica("Hospital Clínico Herminda Martín", "Francisco Ramírez 10", "Chillán", -36.6120, -72.0950))
        val clinicaChillan = destinos.insertar(clinica("Clínica Chillán", "Pedro Aguirre Cerda 35", "Chillán", -36.6060, -72.1000))
        val losAngeles = destinos.insertar(clinica("Complejo Asistencial Dr. Víctor Ríos Ruiz", "Ricardo Vicuña 147", "Los Ángeles", -37.4700, -72.3560))
        val hospitalTemuco = destinos.insertar(clinica("Hospital Dr. Hernán Henríquez Aravena", "Manuel Montt 115", "Temuco", -38.7360, -72.6000))
        val alemanaTemuco = destinos.insertar(clinica("Clínica Alemana Temuco", "Senador Estébanez 645", "Temuco", -38.7350, -72.6050))

        // Tramos de ida y vuelta (km oficiales, peaje en CLP).
        // A propósito no hay tramos entre ciudades distintas (ej. Chillán → Los Ángeles)
        // para poder probar los traslados no catalogados.
        val tramos = listOf(
            Triple(sede to sanatorio, 2.1, 0),
            Triple(sede to regional, 2.4, 0),
            Triple(sede to biobio, 9.5, 0),
            Triple(sede to herminda, 108.0, 3200),
            Triple(sede to clinicaChillan, 107.0, 3200),
            Triple(sede to losAngeles, 136.0, 3600),
            Triple(sede to hospitalTemuco, 292.0, 7400),
            Triple(sede to alemanaTemuco, 293.0, 7400),
            Triple(sanatorio to regional, 2.0, 0),
            Triple(regional to biobio, 9.0, 0),
            Triple(herminda to clinicaChillan, 1.2, 0),
            Triple(hospitalTemuco to alemanaTemuco, 0.8, 0)
        )
        val tramoDao = db.tramoDao()
        tramos.forEach { (par, km, peaje) ->
            val (a, b) = par
            tramoDao.insertar(Tramo(origenId = a, destinoId = b, kmOficial = km, peaje = peaje, vigenteDesde = INICIO_2026))
            tramoDao.insertar(Tramo(origenId = b, destinoId = a, kmOficial = km, peaje = peaje, vigenteDesde = INICIO_2026))
        }
    }

    private fun clinica(nombre: String, direccion: String, ciudad: String, lat: Double, lon: Double) =
        Destino(nombre = nombre, direccion = direccion, ciudad = ciudad, lat = lat, lon = lon, tipo = TipoDestino.CLINICA)
}
