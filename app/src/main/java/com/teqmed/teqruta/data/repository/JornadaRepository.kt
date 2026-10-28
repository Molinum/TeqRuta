package com.teqmed.teqruta.data.repository

import com.teqmed.teqruta.data.local.dao.JornadaDao
import com.teqmed.teqruta.data.local.dao.TrasladoDao
import com.teqmed.teqruta.data.local.entity.Jornada
import com.teqmed.teqruta.data.local.entity.Traslado
import kotlinx.coroutines.flow.Flow

/** Jornadas de los técnicos y los traslados que las componen. */
class JornadaRepository(
    private val jornadaDao: JornadaDao,
    private val trasladoDao: TrasladoDao
) {
    fun observarJornadaAbierta(tecnicoId: Long): Flow<Jornada?> = jornadaDao.observarAbierta(tecnicoId)

    fun observarTraslados(jornadaId: Long): Flow<List<Traslado>> = trasladoDao.observarPorJornada(jornadaId)

    suspend fun obtenerJornada(id: Long): Jornada? = jornadaDao.obtenerPorId(id)

    suspend fun obtenerTraslado(id: Long): Traslado? = trasladoDao.obtenerPorId(id)
}
