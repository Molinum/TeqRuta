package com.teqmed.teqruta.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.teqmed.teqruta.data.local.entity.Usuario
import com.teqmed.teqruta.ui.components.ValidatedTextField
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

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.Route,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp)
            )
            Spacer(Modifier.height(12.dp))
            Text("TeqRuta", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Registro de traslados en terreno",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(32.dp))

            Card(
                modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
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

                    AnimatedVisibility(
                        visible = estado.errorGeneral != null,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        ErrorGeneral(estado.errorGeneral.orEmpty())
                    }

                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            onSubmit()
                        },
                        enabled = !estado.cargando,
                        modifier = Modifier.fillMaxWidth().height(48.dp)
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

@Composable
private fun ErrorGeneral(mensaje: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
            Text(mensaje, style = MaterialTheme.typography.bodyMedium)
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
