package com.teqmed.teqruta.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = AzulTeq,
    onPrimary = Color.White,
    primaryContainer = AzulTeqContenedor,
    onPrimaryContainer = AzulTeqOscuro,
    secondary = VerdeAzulado,
    onSecondary = Color.White,
    secondaryContainer = VerdeAzuladoContenedor,
    onSecondaryContainer = VerdeAzuladoOscuro,
    tertiary = Ambar,
    onTertiary = Color.White,
    tertiaryContainer = AmbarContenedor,
    onTertiaryContainer = AmbarOscuro,
    background = FondoClaro,
    surface = FondoClaro,
    surfaceVariant = VarianteClara,
    onSurfaceVariant = TextoVarianteClaro,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = SuperficieClara1,
    surfaceContainer = SuperficieClara2,
    surfaceContainerHigh = SuperficieClara3,
    surfaceContainerHighest = SuperficieClara4,
    outline = ContornoClaro
)

private val DarkColorScheme = darkColorScheme(
    primary = AzulTeqClaro,
    onPrimary = AzulTeqProfundo,
    primaryContainer = AzulTeqContenedorOscuro,
    onPrimaryContainer = AzulTeqContenedor,
    secondary = VerdeAzuladoClaro,
    onSecondary = VerdeAzuladoProfundo,
    secondaryContainer = VerdeAzuladoContenedorOscuro,
    onSecondaryContainer = VerdeAzuladoContenedor,
    tertiary = AmbarClaro,
    onTertiary = AmbarProfundo,
    tertiaryContainer = AmbarContenedorOscuro,
    onTertiaryContainer = AmbarContenedor,
    background = FondoOscuro,
    surface = FondoOscuro,
    surfaceVariant = VarianteOscura,
    onSurfaceVariant = TextoVarianteOscuro,
    surfaceContainerLowest = SuperficieOscura0,
    surfaceContainerLow = SuperficieOscura1,
    surfaceContainer = SuperficieOscura2,
    surfaceContainerHigh = SuperficieOscura3,
    surfaceContainerHighest = SuperficieOscura4,
    outline = ContornoOscuro
)

/** Tema de la app. No usa colores dinámicos (Android 12+) para mantener la identidad de TEQMED. */
@Composable
fun TeqRutaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
