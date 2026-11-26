package com.teqmed.teqruta.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.ui.components.ErrorCard
import com.teqmed.teqruta.ui.components.ValidatedTextField
import com.teqmed.teqruta.ui.theme.AzulTeqDegradado
import com.teqmed.teqruta.ui.theme.TeqRutaTheme

@Composable
fun LoginScreen(
    viewModel: LoginViewModel,
    onLoginExitoso: (Usuario) -> Unit
) {
    val estado by viewModel.estado.collectAsState()

    LaunchedEffect(estado.usuarioAutenticado) {
        estado.usuarioAutenticado?.let {
            onLoginExitoso(it)
            viewModel.onLoginConsumido()
        }
    }

    LoginContent(
        estado = estado,
        onEmailChange = viewModel::onEmailChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::onSubmit
    )
}

@Composable
private fun LoginContent(
    estado: LoginFormState,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Encabezado con degradado detrás de la barra de estado
        Box(
            Modifier
                .fillMaxWidth()
                .height(340.dp)
                .background(
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, AzulTeqDegradado)),
                    RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                )
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(84.dp).background(Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Route,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text("TeqRuta", style = MaterialTheme.typography.headlineMedium, color = Color.White)
            Text(
                "Registro de traslados en terreno",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(32.dp))

            ElevatedCard(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Iniciar sesión", style = MaterialTheme.typography.titleLarge)

                    ValidatedTextField(
                        value = estado.email,
                        onValueChange = onEmailChange,
                        label = "Correo",
                        leadingIcon = Icons.Outlined.Email,
                        error = estado.emailError,
                        keyboardType = KeyboardType.Email,
                        enabled = !estado.cargando
                    )
                    ValidatedTextField(
                        value = estado.password,
                        onValueChange = onPasswordChange,
                        label = "Contraseña",
                        leadingIcon = Icons.Outlined.Lock,
                        error = estado.passwordError,
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done,
                        onImeAction = {
                            focusManager.clearFocus()
                            onSubmit()
                        },
                        isPassword = true,
                        enabled = !estado.cargando
                    )

                    ErrorCard(estado.errorGeneral)

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onSubmit()
                        },
                        enabled = !estado.cargando,
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        AnimatedContent(targetState = estado.cargando, label = "boton-login") { cargando ->
                            if (cargando) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            } else {
                                Text("Ingresar")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "TEQMED SpA",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginPreview() {
    TeqRutaTheme {
        LoginContent(
            estado = LoginFormState(
                email = "tecnico@",
                emailError = "Ingresa un correo válido",
                errorGeneral = "Correo o contraseña incorrectos"
            ),
            onEmailChange = {},
            onPasswordChange = {},
            onSubmit = {}
        )
    }
}
