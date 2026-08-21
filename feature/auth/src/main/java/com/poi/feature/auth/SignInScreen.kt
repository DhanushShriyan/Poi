package com.poi.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.auth.AuthRepository
import com.poi.core.designsystem.PoiHeroPanel
import kotlinx.coroutines.launch

private enum class SignInMethod { CHOOSE, EMAIL, PHONE }

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun SignInScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onSignedIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var method by remember { mutableStateOf(SignInMethod.CHOOSE) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var createAccount by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("+91 ") }
    var code by remember { mutableStateOf("") }
    var phoneCodeSent by remember { mutableStateOf(false) }
    var previewCode by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val session by authRepository.session.collectAsStateWithLifecycle()
    LaunchedEffect(session.isAuthenticated) {
        if (session.isAuthenticated) onSignedIn()
    }

    fun complete(block: suspend () -> Result<*>) {
        loading = true
        error = null
        status = null
        scope.launch {
            block().fold(
                onSuccess = {
                    if (authRepository.session.value.isAuthenticated) onSignedIn()
                },
                onFailure = { error = it.message ?: "Sign-in could not be completed" },
            )
            loading = false
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Welcome to Poi") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (method == SignInMethod.CHOOSE) onBack() else method = SignInMethod.CHOOSE
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                PoiHeroPanel {
                    Text("Make every plan count.", style = MaterialTheme.typography.headlineLarge, color = androidx.compose.ui.graphics.Color.White)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Save events, coordinate with friends, and keep private moments private.",
                        color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f),
                    )
                }
            }

            if (authRepository.usesPreviewIdentity) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)) {
                        Text(
                            "Account preview: this offline build demonstrates the complete flow. Google and SMS verification will switch to the cloud identity provider before public onboarding.",
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            when (method) {
                SignInMethod.CHOOSE -> {
                    item {
                        OutlinedButton(
                            enabled = !loading && authRepository.supportsGoogleSignIn,
                            onClick = {
                                loading = true
                                error = null
                                status = null
                                scope.launch {
                                    authRepository.signInWithGoogle().fold(
                                        onSuccess = {
                                            status = "Complete Google sign-in in the secure browser window."
                                        },
                                        onFailure = {
                                            error = it.message ?: "Google sign-in could not be started"
                                        },
                                    )
                                    loading = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                        ) {
                            Icon(Icons.Default.AlternateEmail, null)
                            Spacer(Modifier.padding(5.dp))
                            Text("Continue with Google")
                        }
                        if (!authRepository.supportsGoogleSignIn) {
                            Text(
                                "Google sign-in will activate when its OAuth provider is connected.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { method = SignInMethod.EMAIL },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                        ) {
                            Icon(Icons.Default.Lock, null)
                            Spacer(Modifier.padding(5.dp))
                            Text("Continue with email")
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { method = SignInMethod.PHONE },
                            enabled = authRepository.supportsPhoneSignIn,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                        ) {
                            Icon(Icons.Default.PhoneAndroid, null)
                            Spacer(Modifier.padding(5.dp))
                            Text("Continue with phone")
                        }
                        if (!authRepository.supportsPhoneSignIn) {
                            Text(
                                "Phone verification will appear when the SMS provider is enabled.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                SignInMethod.EMAIL -> {
                    item {
                        Text(
                            if (createAccount) "Create your account" else "Email account",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        if (createAccount) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it.take(60) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Your name") },
                                singleLine = true,
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it.take(120) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email address") },
                            singleLine = true,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it.take(80) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            enabled = !loading,
                            onClick = {
                                complete {
                                    if (createAccount) {
                                        authRepository.createAccountWithEmail(email, password, name)
                                            .onSuccess { result ->
                                                if (result.requiresEmailConfirmation) {
                                                    status = "Check your email to confirm your account, then return to Poi."
                                                }
                                            }
                                    } else {
                                        authRepository.signInWithEmail(email, password)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                        ) {
                            Text(
                                when {
                                    loading -> "Please wait…"
                                    createAccount -> "Create account"
                                    else -> "Sign in"
                                },
                            )
                        }
                        TextButton(
                            onClick = { createAccount = !createAccount },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                if (createAccount) "Already have an account? Sign in"
                                else "New to Poi? Create an account",
                            )
                        }
                    }
                }
                SignInMethod.PHONE -> {
                    item {
                        Text("Phone number", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = phone,
                            onValueChange = { phone = it.take(18) },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Country code and number") },
                            singleLine = true,
                        )
                        if (phoneCodeSent) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it.filter(Char::isDigit).take(6) },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("6-digit code") },
                                singleLine = true,
                                supportingText = { previewCode?.let { Text("Offline preview code: $it") } },
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            enabled = !loading,
                            onClick = {
                                if (phoneCodeSent) {
                                    complete { authRepository.verifyPhoneCode(phone, code) }
                                } else {
                                    loading = true
                                    scope.launch {
                                        authRepository.requestPhoneCode(phone).fold(
                                            onSuccess = {
                                                phoneCodeSent = true
                                                previewCode = it.previewCode
                                            },
                                            onFailure = { error = it.message },
                                        )
                                        loading = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                        ) { Text(if (phoneCodeSent) "Verify and continue" else "Send verification code") }
                    }
                }
            }

            error?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            status?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.primary) }
            }

            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    TextButton(onClick = onBack) { Text("Continue browsing as guest") }
                }
            }
        }
    }
}
