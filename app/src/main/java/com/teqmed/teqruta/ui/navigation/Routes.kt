package com.teqmed.teqruta.ui.navigation

import com.teqmed.teqruta.domain.model.Rol

/** Nombre del argumento con el id de la jornada en las rutas de detalle. */
const val ARG_JORNADA_ID = "jornadaId"

/** Rutas de navegación. Cada perfil tiene sus rutas bajo su propio prefijo. */
object Routes {
    const val LOGIN = "login"

    const val TECNICO_INICIO = "tecnico/inicio"
    const val TECNICO_INICIAR_JORNADA = "tecnico/iniciar-jornada"
    const val TECNICO_SALIDA = "tecnico/salida"
    const val TECNICO_LLEGADA = "tecnico/llegada"
    const val TECNICO_HISTORIAL = "tecnico/historial"
    const val TECNICO_JORNADA = "tecnico/jornada/{$ARG_JORNADA_ID}"
    const val JEFE_INICIO = "jefe/inicio"
    const val JEFE_JORNADA = "jefe/jornada/{$ARG_JORNADA_ID}"
    const val GERENCIA_INICIO = "gerencia/inicio"
    const val ADMIN_INICIO = "admin/inicio"
    const val ADMIN_DESTINOS = "admin/destinos"
    const val ADMIN_VEHICULOS = "admin/vehiculos"
    const val ADMIN_TRAMOS = "admin/tramos"
    const val ADMIN_TARIFA = "admin/tarifa"
    const val ADMIN_USUARIOS = "admin/usuarios"

    fun tecnicoJornada(id: Long) = "tecnico/jornada/$id"
    fun jefeJornada(id: Long) = "jefe/jornada/$id"

    /** Pantalla de inicio que corresponde a cada rol después del login (RF-AUT-02). */
    fun inicioDe(rol: Rol): String = when (rol) {
        Rol.TECNICO -> TECNICO_INICIO
        Rol.JEFE -> JEFE_INICIO
        Rol.GERENCIA -> GERENCIA_INICIO
        Rol.ADMIN -> ADMIN_INICIO
    }
}
