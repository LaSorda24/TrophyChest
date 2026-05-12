package com.gonzalez.trophychest.ui.screens

import android.content.Context
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.gonzalez.trophychest.R
import com.gonzalez.trophychest.data.ChatValidation
import com.gonzalez.trophychest.data.FirebaseManager
import com.gonzalez.trophychest.navigation.Screen
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import kotlinx.coroutines.launch

private val LoginBackgroundTop = Color(0xFF0F0F0F)
private val LoginBackgroundMid = Color(0xFF151515)
private val LoginBackgroundBottom = Color(0xFF212121)
private val LoginSurface = Color(0xFF1A1A1A)
private val LoginAccent = Color(0xFFF6C453)
private val LoginOutline = Color(0xFFB8B8B8)
private val LoginWarning = Color(0xFFFFCC80)
private val LoginError = Color(0xFFFF8A80)

// APUNTE: LOGIN DECIDE SI EL USUARIO ENTRA CON EMAIL O CON GOOGLE Y DESPUES CREA SU PERFIL.
@Composable
fun LoginScreen(navController: NavHostController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val googleClientId = remember(context) { resolveGoogleWebClientId(context) }
    val googleSignInClient = remember(googleClientId) {
        if (googleClientId.isNotBlank()) buildGoogleSignInClient(context, googleClientId) else null
    }

    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var isRegisterMode by rememberSaveable { mutableStateOf(false) }
    var isLoading by rememberSaveable { mutableStateOf(false) }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (FirebaseManager.currentUser != null) {
            navigateToPrincipal(navController)
        }
    }

    val googleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            val account = task.getResult(ApiException::class.java)
            handleGoogleAccountResult(
                account = account,
                onError = {
                    errorMessage = it
                    isLoading = false
                },
                onSuccess = { idToken ->
                    coroutineScope.launch {
                        val authResult = if (isRegisterMode) {
                            FirebaseManager.loginWithGoogle(idToken, username)
                        } else {
                            FirebaseManager.loginWithGoogle(idToken)
                        }

                        authResult
                            .onSuccess { navigateToPrincipal(navController) }
                            .onFailure { error ->
                                errorMessage = error.message ?: "No se pudo iniciar sesion con Google."
                            }
                        isLoading = false
                    }
                }
            )
        } catch (apiException: ApiException) {
            errorMessage = googleSignInErrorMessage(apiException)
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(LoginBackgroundTop, LoginBackgroundMid, LoginBackgroundBottom)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            shape = RoundedCornerShape(28.dp),
            color = LoginSurface.copy(alpha = 0.96f),
            tonalElevation = 10.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isRegisterMode) "Crear cuenta" else "Iniciar sesion",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Black
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Correo electronico") },
                    singleLine = true
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contrasena") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation()
                )

                if (isRegisterMode) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Nombre de usuario") },
                        singleLine = true,
                        isError = username.isNotBlank() && ChatValidation.usernameError(username) != null,
                        supportingText = {
                            val usernameError = ChatValidation.usernameError(username)
                            if (usernameError != null && username.isNotBlank()) {
                                Text(usernameError)
                            } else {
                                Text("3-20 caracteres: letras, numeros y guion bajo.")
                            }
                        }
                    )
                }

                errorMessage?.let {
                    Text(
                        text = it,
                        color = LoginError,
                        fontSize = 14.sp
                    )
                }

                if (googleClientId.isBlank()) {
                    Text(
                        text = "Falta google-services.json o el Web Client ID de Firebase. El acceso por correo ya queda preparado.",
                        color = LoginWarning,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = {
                        val validationMessage = validateCredentials(email, password, username, isRegisterMode)
                        if (validationMessage != null) {
                            errorMessage = validationMessage
                            return@Button
                        }

                        isLoading = true
                        errorMessage = null
                        coroutineScope.launch {
                            val authResult = if (isRegisterMode) {
                                FirebaseManager.register(email, password, username)
                            } else {
                                FirebaseManager.login(email, password)
                            }

                            authResult
                                .onSuccess { navigateToPrincipal(navController) }
                                .onFailure { error ->
                                    errorMessage = error.message ?: "No se pudo completar la autenticacion."
                                }
                            isLoading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = LoginAccent)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(18.dp)
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) "Crear cuenta" else "Iniciar sesion",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        if (googleSignInClient == null) {
                            errorMessage = "Configura Firebase para habilitar Google Sign-In."
                            return@OutlinedButton
                        }
                        if (isRegisterMode) {
                            ChatValidation.usernameError(username)?.let {
                                errorMessage = it
                                return@OutlinedButton
                            }
                        }

                        isLoading = true
                        errorMessage = null
                        googleLauncher.launch(googleSignInClient.signInIntent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = BorderStroke(
                        1.dp,
                        LoginOutline.copy(alpha = 0.55f)
                    )
                ) {
                    Text(if (isRegisterMode) "Crear cuenta con Google" else "Continuar con Google")
                }

                HorizontalDivider(color = LoginAccent.copy(alpha = 0.28f))

                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        errorMessage = null
                    },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text(
                        text = if (isRegisterMode) {
                            "Ya tengo cuenta"
                        } else {
                            "O crear una cuenta nueva"
                        }
                    )
                }
            }
        }
    }
}

private fun validateCredentials(
    email: String,
    password: String,
    username: String,
    isRegisterMode: Boolean
): String? {
    return when {
        email.isBlank() || password.isBlank() -> "Completa el correo y la contrasena."
        !Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches() -> "Introduce un correo valido."
        password.length < 6 -> "La contrasena debe tener al menos 6 caracteres."
        isRegisterMode -> ChatValidation.usernameError(username)
        else -> null
    }
}

private fun navigateToPrincipal(navController: NavHostController) {
    navController.navigate(Screen.Principal.route) {
        popUpTo("video_splash") { inclusive = true }
        launchSingleTop = true
    }
}

private fun handleGoogleAccountResult(
    account: GoogleSignInAccount?,
    onError: (String) -> Unit,
    onSuccess: (String) -> Unit
) {
    val idToken = account?.idToken
    if (idToken.isNullOrBlank()) {
        onError("No se recibio el token de Google. Revisa la configuracion de Firebase.")
        return
    }
    onSuccess(idToken)
}

private fun buildGoogleSignInClient(context: Context, webClientId: String): GoogleSignInClient {
    val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestIdToken(webClientId)
        .build()

    return GoogleSignIn.getClient(context, googleSignInOptions)
}

private fun resolveGoogleWebClientId(context: Context): String {
    val generatedId = context.resources.getIdentifier(
        "default_web_client_id",
        "string",
        context.packageName
    )

    if (generatedId != 0) {
        return context.getString(generatedId)
    }

    return context.getString(R.string.google_web_client_id)
}

private fun googleSignInErrorMessage(exception: ApiException): String {
    return when (exception.statusCode) {
        CommonStatusCodes.CANCELED -> "Se cancelo el acceso con Google."
        CommonStatusCodes.DEVELOPER_ERROR -> {
            "Google Sign-In no esta bien configurado. Revisa SHA-1/SHA-256, proveedor Google y google-services.json."
        }
        else -> {
            val detail = exception.message
                ?: "sin detalle"
            "No se pudo completar Google Sign-In (codigo ${exception.statusCode}: $detail)."
        }
    }
}
