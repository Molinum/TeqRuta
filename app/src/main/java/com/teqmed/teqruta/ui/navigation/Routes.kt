package com.teqmed.teqruta.ui.navigation

import com.teqmed.teqruta.domain.model.Rol

/** Rutas de navegación. Cada perfil tiene sus rutas bajo su propio prefijo. */
object Routes {
    const val LOGIN = "login"

    const val TECNICO_INICIO = "tecnico/inicio"
    const val TECNICO_INICIAR_JORNADA = "tecnico/iniciar-jornada"
    const val TECNICO_SALIDA = "tecnico/salida"
    const val TECNICO_LLEGADA = "tecnico/llegada"
    const val JEFE_INICIO = "jefe/inicio"
    const val GERENCIA_INICIO = "gerencia/inicio"
    const val ADMIN_INICIO = "admin/inicio"

    /** Pantalla de inicio que corresponde a cada rol después del login (RF-AUT-02). */
    fun inicioDe(rol: Rol): String = when (rol) {
        Rol.TECNICO -> TECNICO_INICIO
        Rol.JEFE -> JEFE_INICIO
        Rol.GERENCIA -> GERENCIA_INICIO
        Rol.ADMIN -> ADMIN_INICIO
    }
}
