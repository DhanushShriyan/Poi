package com.poi.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.poi.core.designsystem.PoiHeroPanel
import com.poi.core.designsystem.PoiWordmark

@Composable
fun GuestGateScreen(
    title: String,
    message: String,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(76.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.Lock, null, Modifier.size(34.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(22.dp))
            Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            Button(onClick = onSignIn, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                Text("Sign in to continue")
            }
        }
    }
}

@Composable
fun GuestProfileScreen(
    darkMode: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onSignIn: () -> Unit,
    onAdminAccess: () -> Unit,
    onAppUpdates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp, 22.dp, 20.dp, 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PoiWordmark(Modifier.weight(1f))
                Icon(if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode, null)
                Spacer(Modifier.size(8.dp))
                Switch(checked = darkMode, onCheckedChange = onThemeChange)
            }
        }
        item {
            PoiHeroPanel {
                Text(
                    "Your city, in one beautiful place.",
                    color = androidx.compose.ui.graphics.Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Browse freely. Sign in when you want to save plans, join friends, and create events.",
                    color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.88f),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(20.dp))
                Button(onClick = onSignIn) { Text("Create your Poi account") }
            }
        }
        item {
            Text("More useful with an account", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(10.dp))
            GuestBenefit(Icons.Default.CalendarMonth, "Never miss a plan", "Save events and keep every date together.")
            GuestBenefit(Icons.Default.Groups, "Know who's joining", "See friend activity only when people choose to share.")
            GuestBenefit(Icons.Default.NotificationsActive, "Timely reminders", "Get useful updates without notification noise.")
            GuestBenefit(Icons.Default.AutoAwesome, "Build your event story", "Host events and collect trusted contributions.")
        }
        item {
            OutlinedButton(onClick = onAppUpdates, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.SystemUpdateAlt, null)
                Text("  Check app updates")
            }
        }
        item {
            Text(
                "Poi protects private events and check-ins with audience controls. Guest browsing never requires contacts or precise location.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        item {
            Text(
                "Poi · account access",
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(onLongPress = { onAdminAccess() })
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun GuestBenefit(icon: ImageVector, title: String, supporting: String) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(46.dp).clip(RoundedCornerShape(15.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.size(13.dp))
            Column {
                Text(title, fontWeight = FontWeight.SemiBold)
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
