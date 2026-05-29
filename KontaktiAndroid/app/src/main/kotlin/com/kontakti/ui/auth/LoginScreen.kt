package com.kontakti.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.launch

private val Indigo = Color(0xFF4F46E5)

/**
 * Sign in or create a new account. Mirrors iOS `LoginView` + `RegisterView`.
 * `vm` is hoisted so the parent navigation host can observe `AuthState`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    vm: AuthViewModel = hiltViewModel()
) {
    var mode by remember { mutableStateOf(AuthMode.SignIn) }
    val scrollState = rememberScrollState()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Logo placeholder — designed for parity with iOS rounded square logo.
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("K", fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Kontakti", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(
                "Personal relationship intelligence",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(32.dp))

            when (mode) {
                AuthMode.SignIn -> SignInForm(
                    vm = vm,
                    onSwitchMode = { mode = AuthMode.SignUp }
                )
                AuthMode.SignUp -> SignUpForm(
                    vm = vm,
                    onSwitchMode = { mode = AuthMode.SignIn }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private enum class AuthMode { SignIn, SignUp }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignInForm(
    vm: AuthViewModel,
    onSwitchMode: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val errorMessage by vm.errorMessage.collectAsState()
    val scope = rememberCoroutineScope()

    val canSubmit = email.isNotBlank() && password.isNotBlank() && email.contains("@")

    fun submit() {
        if (!canSubmit || isLoading) return
        scope.launch {
            isLoading = true
            vm.login(email.trim(), password)
            isLoading = false
        }
    }

    OutlinedTextField(
        value = email,
        onValueChange = { email = it; vm.clearError() },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = password,
        onValueChange = { password = it; vm.clearError() },
        label = { Text("Password") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Go
        ),
        modifier = Modifier.fillMaxWidth()
    )

    ErrorBanner(errorMessage)

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { submit() },
        enabled = canSubmit && !isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Indigo)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text("Sign in", fontWeight = FontWeight.SemiBold)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onSwitchMode,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("Create account", color = Indigo, fontWeight = FontWeight.SemiBold)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignUpForm(
    vm: AuthViewModel,
    onSwitchMode: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val errorMessage by vm.errorMessage.collectAsState()
    val scope = rememberCoroutineScope()

    val canSubmit = name.isNotBlank() && username.isNotBlank() &&
        email.isNotBlank() && password.length >= 8 && email.contains("@")

    fun submit() {
        if (!canSubmit || isLoading) return
        scope.launch {
            isLoading = true
            vm.register(name.trim(), username.trim(), email.trim(), password)
            isLoading = false
        }
    }

    OutlinedTextField(
        value = name,
        onValueChange = { name = it; vm.clearError() },
        label = { Text("Full name") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = username,
        onValueChange = { username = it; vm.clearError() },
        label = { Text("Username") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = email,
        onValueChange = { email = it; vm.clearError() },
        label = { Text("Email") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Email,
            imeAction = ImeAction.Next
        ),
        modifier = Modifier.fillMaxWidth()
    )

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedTextField(
        value = password,
        onValueChange = { password = it; vm.clearError() },
        label = { Text("Password (8+ chars)") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Go
        ),
        modifier = Modifier.fillMaxWidth()
    )

    ErrorBanner(errorMessage)

    Spacer(modifier = Modifier.height(16.dp))

    Button(
        onClick = { submit() },
        enabled = canSubmit && !isLoading,
        modifier = Modifier.fillMaxWidth().height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Indigo)
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
            Text("Create account", fontWeight = FontWeight.SemiBold)
        }
    }

    Spacer(modifier = Modifier.height(12.dp))

    OutlinedButton(
        onClick = onSwitchMode,
        modifier = Modifier.fillMaxWidth().height(50.dp)
    ) {
        Text("I already have an account", color = Indigo, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun ErrorBanner(message: String?) {
    if (message.isNullOrBlank()) return
    Spacer(modifier = Modifier.height(12.dp))
    Surface(
        color = MaterialTheme.colorScheme.errorContainer,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

