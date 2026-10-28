package com.teqmed.teqruta.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.domain.model.Rol
import com.teqmed.teqruta.ui.SessionViewModel
import com.teqmed.teqruta.ui.ViewModelFactory
import com.teqmed.teqruta.ui.auth.LoginScreen
import com.teqmed.teqruta.ui.components.ProximamenteScreen

private const val DURACION_TRANSICION = 300

@Composable
fun TeqRutaNavGraph(
    factory: ViewModelFactory,
    sesion: SessionViewModel,
    navController: NavHostController = rememberNavController()
) {
    val usuario by sesion.usuario.collectAsState()

    val cerrarSesion: () -> Unit = {
        sesion.cerrarSesion()
        navController.navigate(Routes.LOGIN) {
            popUpTo(navController.graph.id) { inclusive = true }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN,
        enterTransition = {
            slideIntoContainer(SlideDirection.Start, tween(DURACION_TRANSICION)) + fadeIn(tween(DURACION_TRANSICION))
        },
        exitTransition = {
            slideOutOfContainer(SlideDirection.Start, tween(DURACION_TRANSICION)) + fadeOut(tween(DURACION_TRANSICION))
        },
        popEnterTransition = {
            slideIntoContainer(SlideDirection.End, tween(DURACION_TRANSICION)) + fadeIn(tween(DURACION_TRANSICION))
        },
        popExitTransition = {
            slideOutOfContainer(SlideDirection.End, tween(DURACION_TRANSICION)) + fadeOut(tween(DURACION_TRANSICION))
        }
    ) {
        composable(Routes.LOGIN) {
            LoginScreen(
                viewModel = viewModel(factory = factory),
                onLoginExitoso = { usuarioLogueado ->
                    sesion.iniciarSesion(usuarioLogueado)
                    // Se quita el login del historial para que "atrás" no vuelva a él
                    navController.navigate(Routes.inicioDe(usuarioLogueado.rol)) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.TECNICO_INICIO) {
            RequiereRol(usuario, Rol.TECNICO, onNoAutorizado = cerrarSesion) { actual ->
                ProximamenteScreen(
                    titulo = "Mis traslados",
                    subtitulo = subtituloDe(actual),
                    descripcion = "Aquí podrás iniciar tu jornada y registrar tus traslados.",
                    onCerrarSesion = cerrarSesion
                )
            }
        }

        composable(Routes.JEFE_INICIO) {
            RequiereRol(usuario, Rol.JEFE, onNoAutorizado = cerrarSesion) { actual ->
                ProximamenteScreen(
                    titulo = "Validación",
                    subtitulo = subtituloDe(actual),
                    descripcion = "Aquí podrás revisar y validar las jornadas de tu equipo.",
                    onCerrarSesion = cerrarSesion
                )
            }
        }

        composable(Routes.GERENCIA_INICIO) {
            RequiereRol(usuario, Rol.GERENCIA, onNoAutorizado = cerrarSesion) { actual ->
                ProximamenteScreen(
                    titulo = "Costos de traslado",
                    subtitulo = subtituloDe(actual),
                    descripcion = "Aquí podrás consultar el costo de traslado por cliente, técnico y vehículo.",
                    onCerrarSesion = cerrarSesion
                )
            }
        }

        composable(Routes.ADMIN_INICIO) {
            RequiereRol(usuario, Rol.ADMIN, onNoAutorizado = cerrarSesion) { actual ->
                ProximamenteScreen(
                    titulo = "Administración",
                    subtitulo = subtituloDe(actual),
                    descripcion = "Aquí podrás mantener destinos, vehículos, tramos, tarifas y usuarios.",
                    onCerrarSesion = cerrarSesion
                )
            }
        }
    }
}

/**
 * Muestra [contenido] solo si hay sesión y el usuario tiene el [rol] requerido; si no,
 * ejecuta [onNoAutorizado] (RF-AUT-03).
 */
@Composable
private fun RequiereRol(
    usuario: Usuario?,
    rol: Rol,
    onNoAutorizado: () -> Unit,
    contenido: @Composable (Usuario) -> Unit
) {
    if (usuario != null && usuario.rol == rol) {
        contenido(usuario)
    } else {
        LaunchedEffect(Unit) { onNoAutorizado() }
    }
}

private fun subtituloDe(usuario: Usuario) = "${usuario.nombre} · ${usuario.rol.etiqueta}"
