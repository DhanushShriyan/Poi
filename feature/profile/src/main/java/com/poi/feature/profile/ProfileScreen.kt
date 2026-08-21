package com.poi.feature.profile

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.poi.core.data.EventRepository
import com.poi.core.designsystem.PoiInitialAvatar
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.designsystem.PoiStatusPill
import com.poi.core.model.AuthUser
import com.poi.core.model.UserRole

@Composable
fun ProfileScreen(
    repository: EventRepository,
    authUser: AuthUser,
    darkMode: Boolean,
    versionName: String,
    onThemeChange: (Boolean) -> Unit,
    onAdmin: () -> Unit,
    onSignOut: () -> Unit,
    onSettings: () -> Unit,
    onSafety: () -> Unit,
    onAppUpdates: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile by repository.profile.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                PoiInitialAvatar(authUser.displayName, Modifier.size(78.dp))
                Spacer(Modifier.padding(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(authUser.displayName, style = MaterialTheme.typography.headlineMedium)
                    Text(
                        authUser.email ?: authUser.phone ?: profile.handle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(6.dp))
                    PoiStatusPill("${authUser.provider.label} · ${profile.homeArea}")
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.padding(7.dp))
                    Column(Modifier.weight(1f)) {
                        Text(if (darkMode) "Dark appearance" else "Light appearance", style = MaterialTheme.typography.titleMedium)
                        Text("Choose the look that feels comfortable", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = darkMode, onCheckedChange = onThemeChange)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StatCard("${profile.attendedCount}", "Attended", Icons.Default.CalendarMonth, Modifier.weight(1f))
                StatCard("${profile.hostedCount}", "Hosted", Icons.Default.AddCircle, Modifier.weight(1f))
                StatCard("${profile.contributionPoints}", "Trust pts", Icons.Default.EmojiEvents, Modifier.weight(1f))
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(22.dp),
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudOff, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.padding(6.dp))
                    Column {
                        Text("Private offline test mode", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Your test data stays on this device until cloud sync is connected.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        item {
            PoiSectionHeader("Account & safety")
            Spacer(Modifier.height(8.dp))
            MenuCard(
                icon = Icons.Default.SystemUpdateAlt,
                title = "App updates",
                supporting = "Installed $versionName · check the latest Poi release",
                onClick = onAppUpdates,
            )
            Spacer(Modifier.height(10.dp))
            MenuCard(
                icon = Icons.Default.PrivacyTip,
                title = "Privacy & notifications",
                supporting = "Check-ins, plans and reminder controls",
                onClick = onSettings,
            )
            Spacer(Modifier.height(10.dp))
            MenuCard(
                icon = Icons.Default.Shield,
                title = "Safety centre",
                supporting = "Reporting, blocking and community rules",
                onClick = onSafety,
            )
            if (authUser.role == UserRole.ADMIN) {
                Spacer(Modifier.height(10.dp))
                MenuCard(
                    icon = Icons.Default.AdminPanelSettings,
                    title = "Administration",
                    supporting = "Events, verification and moderation",
                    onClick = onAdmin,
                )
            }
            Spacer(Modifier.height(10.dp))
            MenuCard(
                icon = Icons.AutoMirrored.Filled.Help,
                title = "Help & feedback",
                supporting = "Email the Poi test support address",
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_SENDTO,
                        Uri.parse("mailto:mjshriyan8@gmail.com?subject=Poi%20test%20feedback"),
                    )
                    runCatching { context.startActivity(intent) }
                },
            )
        }

        item {
            PoiSectionHeader("Trust profile")
            Spacer(Modifier.height(8.dp))
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(Modifier.padding(16.dp)) {
                    TrustRow(Icons.Default.VerifiedUser, "Phone", "Connect during cloud setup")
                    TrustRow(Icons.Default.People, "Community contributions", "3 helpful confirmations")
                    TrustRow(
                        Icons.Default.AdminPanelSettings,
                        "Account role",
                        if (authUser.role == UserRole.ADMIN) "Poi administrator" else "Community member",
                    )
                }
            }
        }

        item {
            FilledTonalButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.AutoMirrored.Filled.Logout, null)
                Text("  Sign out")
            }
        }

        item {
            Text(
                "Poi $versionName · Preview build\nSupport: mjshriyan8@gmail.com",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun StatCard(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier,
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(12.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge)
            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MenuCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    supporting: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.padding(7.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(supporting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null)
        }
    }
}

@Composable
private fun TrustRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.padding(6.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
