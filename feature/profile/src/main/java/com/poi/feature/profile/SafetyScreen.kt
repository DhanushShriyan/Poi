package com.poi.feature.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.designsystem.PoiSettingRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SafetyScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Safety centre") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(20.dp, 12.dp, 20.dp, 40.dp),
        ) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(22.dp),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Icon(Icons.Default.HealthAndSafety, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(10.dp))
                        Text("Safety is part of every event", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Poi never publishes your exact live location or attendee identity without an explicit choice.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
                Spacer(Modifier.height(22.dp))
                PoiSectionHeader("Controls")
                PoiSettingRow(
                    Icons.Default.Flag,
                    "Report an event",
                    "Use the flag on any event page; the event is hidden immediately on this device.",
                    trailing = {},
                )
                PoiSettingRow(
                    Icons.Default.Block,
                    "Block users and content",
                    "The production cloud profile will include clear block controls for all user-generated content.",
                    trailing = {},
                )
                PoiSettingRow(
                    Icons.Default.VerifiedUser,
                    "Trust labels",
                    "Community submitted, confirmed, organizer verified, and official are separate levels.",
                    trailing = {},
                )
                PoiSettingRow(
                    Icons.Default.Lock,
                    "Private by default",
                    "Check-ins expire after the event and use your selected audience.",
                    trailing = {},
                )
                Spacer(Modifier.height(20.dp))
                PoiSectionHeader("Community rules")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Be accurate. Get permission before listing private events. Do not post another person's location, private information, or photograph without consent. Do not impersonate organizers, sell fraudulent tickets, harass attendees, or upload illegal or unsafe content.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Poi is not an emergency service. Contact local emergency services when anyone is in immediate danger.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

