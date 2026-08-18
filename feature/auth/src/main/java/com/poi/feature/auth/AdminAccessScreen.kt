package com.poi.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.poi.core.auth.AuthRepository
import kotlinx.coroutines.launch

@androidx.compose.material3.ExperimentalMaterial3Api
@Composable
fun AdminAccessScreen(
    authRepository: AuthRepository,
    onBack: () -> Unit,
    onAuthenticated: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var email by remember { mutableStateOf("") }
    var accessCode by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Restricted access") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Icon(Icons.Default.AdminPanelSettings, null, tint = MaterialTheme.colorScheme.primary)
                Text("Poi administration", style = MaterialTheme.typography.headlineLarge)
                Text(
                    "This console is restricted to the configured owner identity and high-entropy access code.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.take(120) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Administrator email") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = accessCode,
                    onValueChange = { accessCode = it.take(80) },
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    label = { Text("Private access code") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            }
            error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            item {
                Button(
                    enabled = !loading,
                    onClick = {
                        loading = true
                        error = null
                        scope.launch {
                            authRepository.signInAsAdmin(email, accessCode).fold(
                                onSuccess = { onAuthenticated() },
                                onFailure = { error = it.message },
                            )
                            loading = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) { Text(if (loading) "Verifying…" else "Open administration") }
            }
        }
    }
}
