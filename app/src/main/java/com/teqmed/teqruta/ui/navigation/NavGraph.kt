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
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.domain.model.Rol
import com.teqmed.teqruta.ui.SessionViewModel
import com.teqmed.teqruta.ui.ViewModelFactory
import com.teqmed.teqruta.ui.auth.LoginScreen
import com.teqmed.teqruta.ui.admin.AdminInicioScreen
import com.teqmed.teqruta.ui.admin.DestinosScreen
import com.teqmed.teqruta.ui.admin.TarifaScreen
import com.teqmed.teqruta.ui.admin.TramosScreen
import com.teqmed.teqruta.ui.admin.UsuariosScreen
import com.teqmed.teqruta.ui.admin.VehiculosScreen
import com.teqmed.teqruta.ui.gerencia.GerenciaScreen
import com.teqmed.teqruta.ui.jefe.JefeInicioScreen
import com.teqmed.teqruta.ui.jornada.JornadaDetalleScreen
import com.teqmed.teqruta.ui.tecnico.HistorialScreen
import com.teqmed.teqruta.ui.tecnico.IniciarJornadaScreen
import com.teqmed.teqruta.ui.tecnico.RegistrarLlegadaScreen
import com.teqmed.teqruta.ui.tecnico.RegistrarSalidaScreen
import com.teqmed.teqruta.ui.tecnico.TecnicoHomeScreen

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
                TecnicoHomeScreen(
                    viewModel = viewModel(factory = factory.conUsuario(actual.id)),
                    subtitulo = subtituloDe(actual),
                    onIniciarJornada = { navController.navigate(Routes.TECNICO_INICIAR_JORNADA) },
                    onSalir = { navController.navigate(Routes.TECNICO_SALIDA) },
                    onRegistrarLlegada = { navController.navigate(Routes.TECNICO_LLEGADA) },
                    onHistorial = { navController.navigate(Routes.TECNICO_HISTORIAL) },
                    onCerrarSesion = cerrarSesion
                )
            }
        }

        composable(Routes.TECNICO_INICIAR_JORNADA) {
            RequiereRol(usuario, Rol.TECNICO, onNoAutorizado = cerrarSesion) { actual ->
                IniciarJornadaScreen(
                    viewModel = viewModel(factory = factory.conUsuario(actual.id)),
                    onVolver = { navController.popBackStack() },
                    onJornadaIniciada = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.TECNICO_SALIDA) {
            RequiereRol(usuario, Rol.TECNICO, onNoAutorizado = cerrarSesion) { actual ->
                RegistrarSalidaScreen(
                    viewModel = viewModel(factory = factory.conUsuario(actual.id)),
                    onVolver = { navController.popBackStack() },
                    onSalidaRegistrada = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.TECNICO_LLEGADA) {
            RequiereRol(usuario, Rol.TECNICO, onNoAutorizado = cerrarSesion) { actual ->
                RegistrarLlegadaScreen(
                    viewModel = viewModel(factory = factory.conUsuario(actual.id)),
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.TECNICO_HISTORIAL) {
            RequiereRol(usuario, Rol.TECNICO, onNoAutorizado = cerrarSesion) { actual ->
                HistorialScreen(
                    viewModel = viewModel(factory = factory.conUsuario(actual.id)),
                    onVolver = { navController.popBackStack() },
                    onAbrirJornada = { navController.navigate(Routes.tecnicoJornada(it)) }
                )
            }
        }

        composable(Routes.TECNICO_JORNADA, arguments = argumentoJornada) { entrada ->
            RequiereRol(usuario, Rol.TECNICO, onNoAutorizado = cerrarSesion) { actual ->
                JornadaDetalleScreen(
                    viewModel = viewModel(factory = factory.conUsuario(actual.id).conJornada(entrada.jornadaId())),
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.JEFE_INICIO) {
            RequiereRol(usuario, Rol.JEFE, onNoAutorizado = cerrarSesion) { actual ->
                JefeInicioScreen(
                    viewModel = viewModel(factory = factory.conUsuario(actual.id)),
                    subtitulo = subtituloDe(actual),
                    onAbrirJornada = { navController.navigate(Routes.jefeJornada(it)) },
                    onCerrarSesion = cerrarSesion
                )
            }
        }

        composable(Routes.JEFE_JORNADA, arguments = argumentoJornada) { entrada ->
            RequiereRol(usuario, Rol.JEFE, onNoAutorizado = cerrarSesion) { actual ->
                JornadaDetalleScreen(
                    viewModel = viewModel(factory = factory.conUsuario(actual.id).conJornada(entrada.jornadaId())),
                    onVolver = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.GERENCIA_INICIO) {
            RequiereRol(usuario, Rol.GERENCIA, onNoAutorizado = cerrarSesion) { actual ->
                GerenciaScreen(
                    viewModel = viewModel(factory = factory),
                    subtitulo = subtituloDe(actual),
                    onCerrarSesion = cerrarSesion
                )
            }
        }

        composable(Routes.ADMIN_INICIO) {
            RequiereRol(usuario, Rol.ADMIN, onNoAutorizado = cerrarSesion) { actual ->
                AdminInicioScreen(
                    subtitulo = subtituloDe(actual),
                    onDestinos = { navController.navigate(Routes.ADMIN_DESTINOS) },
                    onVehiculos = { navController.navigate(Routes.ADMIN_VEHICULOS) },
                    onTramos = { navController.navigate(Routes.ADMIN_TRAMOS) },
                    onTarifa = { navController.navigate(Routes.ADMIN_TARIFA) },
                    onUsuarios = { navController.navigate(Routes.ADMIN_USUARIOS) },
                    onCerrarSesion = cerrarSesion
                )
            }
        }

        composable(Routes.ADMIN_DESTINOS) {
            RequiereRol(usuario, Rol.ADMIN, onNoAutorizado = cerrarSesion) { actual ->
                DestinosScreen(viewModel(factory = factory.conUsuario(actual.id))) { navController.popBackStack() }
            }
        }

        composable(Routes.ADMIN_VEHICULOS) {
            RequiereRol(usuario, Rol.ADMIN, onNoAutorizado = cerrarSesion) { actual ->
                VehiculosScreen(viewModel(factory = factory.conUsuario(actual.id))) { navController.popBackStack() }
            }
        }

        composable(Routes.ADMIN_TRAMOS) {
            RequiereRol(usuario, Rol.ADMIN, onNoAutorizado = cerrarSesion) { actual ->
                TramosScreen(viewModel(factory = factory.conUsuario(actual.id))) { navController.popBackStack() }
            }
        }

        composable(Routes.ADMIN_TARIFA) {
            RequiereRol(usuario, Rol.ADMIN, onNoAutorizado = cerrarSesion) { actual ->
                TarifaScreen(viewModel(factory = factory.conUsuario(actual.id))) { navController.popBackStack() }
            }
        }

        composable(Routes.ADMIN_USUARIOS) {
            RequiereRol(usuario, Rol.ADMIN, onNoAutorizado = cerrarSesion) { actual ->
                UsuariosScreen(viewModel(factory = factory.conUsuario(actual.id))) { navController.popBackStack() }
            }
        }
    }
}

private val argumentoJornada = listOf(navArgument(ARG_JORNADA_ID) { type = NavType.LongType })

private fun NavBackStackEntry.jornadaId(): Long = requireNotNull(arguments?.getLong(ARG_JORNADA_ID))

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
