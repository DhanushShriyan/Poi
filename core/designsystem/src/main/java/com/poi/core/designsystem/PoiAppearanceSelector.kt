package com.poi.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.poi.core.model.PoiVisualTheme

@Composable
fun PoiAppearanceSelector(
    selectedTheme: PoiVisualTheme,
    darkMode: Boolean,
    onThemeSelected: (PoiVisualTheme) -> Unit,
    onDarkModeChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("App style", style = MaterialTheme.typography.titleLarge)
            Text(
                "Classic stays the default. Your choice is saved on this device.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PoiVisualTheme.entries.forEach { theme ->
                ThemeChoice(
                    theme = theme,
                    selected = selectedTheme == theme,
                    onClick = { onThemeSelected(theme) },
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    if (darkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.size(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (darkMode) "Dark appearance" else "Light appearance",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "Available for every app style",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = darkMode, onCheckedChange = onDarkModeChanged)
            }
        }
    }
}

@Composable
private fun ThemeChoice(
    theme: PoiVisualTheme,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            },
        ),
        border = if (selected) {
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else {
            CardDefaults.outlinedCardBorder()
        },
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ThemePalette(theme)
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(theme.label, fontWeight = FontWeight.Bold)
                Text(
                    theme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ThemePalette(theme: PoiVisualTheme) {
    val colors = when (theme) {
        PoiVisualTheme.CLASSIC -> listOf(
            Color(0xFF5B5FEF),
            Color(0xFFF16F54),
            Color(0xFF19B6A5),
            Color(0xFFF7F8FC),
        )
        PoiVisualTheme.PULSE -> listOf(
            Color(0xFF181C19),
            Color(0xFFE5F35B),
            Color(0xFFF2987C),
            Color(0xFF90CEB2),
        )
        PoiVisualTheme.RETRO -> listOf(
            Color(0xFFE6B83D),
            Color(0xFF2F6974),
            Color(0xFFB64C29),
            Color(0xFFF2EACE),
        )
    }
    Row(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.outlineVariant, CircleShape)
            .padding(2.dp),
    ) {
        colors.forEach { color ->
            Box(
                Modifier
                    .size(width = 11.dp, height = 34.dp)
                    .background(color),
            )
        }
    }
}
