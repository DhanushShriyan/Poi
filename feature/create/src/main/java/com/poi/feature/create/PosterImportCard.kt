package com.poi.feature.create

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.poi.core.poster.OnDevicePosterReader
import com.poi.core.poster.PosterDraft
import com.poi.core.poster.PosterLanguage
import com.poi.core.poster.PosterParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun PosterImportCard(enabled: Boolean, onDraft: (PosterDraft) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reader = remember { OnDevicePosterReader(context) }
    var image by remember { mutableStateOf<android.net.Uri?>(null) }
    var language by remember { mutableStateOf(PosterLanguage.AUTO) }
    var rotation by remember { mutableStateOf(0) }
    var scanning by remember { mutableStateOf(false) }
    var job by remember { mutableStateOf<Job?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var rawText by remember { mutableStateOf<String?>(null) }
    var weak by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var applied by remember { mutableStateOf(false) }
    var confirmedYear by remember { mutableStateOf("") }
    var chosenDate by remember { mutableStateOf<String?>(null) }
    var chosenEndDate by remember { mutableStateOf<String?>(null) }
    var chosenTime by remember { mutableStateOf<String?>(null) }
    val chosenYear = confirmedYear.toIntOrNull()?.takeIf { confirmedYear.length == 4 && it in 1900..2199 }
    val draft = remember(rawText) { rawText?.let(PosterParser::parse) }
    fun resetChoices() { chosenDate = null; chosenEndDate = null; chosenTime = null; confirmedYear = "" }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            image = uri; rotation = 0; rawText = null; error = null; applied = false
            resetChoices()
        }
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                Text("Create from a poster", style = MaterialTheme.typography.titleMedium)
            }
            Text("Kannada, English and Hindi. Read on your phone, then review before publishing.", style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { picker.launch("image/*") }, enabled = enabled && !scanning) {
                Text(if (image == null) "Choose poster" else "Choose another poster")
            }
            image?.let { uri ->
                AsyncImage(uri, "Selected poster", Modifier.fillMaxWidth().height(180.dp).rotate(rotation.toFloat()), contentScale = ContentScale.Fit)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(PosterLanguage.entries) { option ->
                        FilterChip(selected = language == option, enabled = !scanning && enabled,
                            onClick = { language = option; rawText = null; applied = false }, label = { Text(option.label) })
                    }
                }
                Row {
                    Button(enabled = enabled && !scanning, onClick = {
                        job = scope.launch {
                            scanning = true; error = null; rawText = null; applied = false; resetChoices()
                            try {
                                val result = reader.read(uri, language, rotation)
                                rawText = result.draft.sourceText; weak = result.weakRecognition
                            } catch (cancelled: CancellationException) {
                                throw cancelled
                            } catch (failure: Exception) {
                                error = failure.message ?: "The poster could not be read. Try another image."
                            } finally { scanning = false }
                        }
                    }) { Text("Read poster") }
                    TextButton(enabled = !scanning && enabled, onClick = {
                        rotation = (rotation + 90) % 360; rawText = null; applied = false
                    }) { Text("Rotate 90°") }
                }
            }
            if (scanning) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
                Text("Reading on this phone… Large posters may take longer.", style = MaterialTheme.typography.bodySmall)
                // Native recognition is serialised. Cancellation discards its result without publishing.
                TextButton(onClick = { job?.cancel() }) { Text("Cancel reading") }
            }
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            draft?.let { result ->
                Text(if (applied) "Draft added below" else "Suggested draft", style = MaterialTheme.typography.titleMedium)
                Text(result.title.ifBlank { "Event name needs your input" })
                Text("${result.date ?: "Date needs review"} · ${result.time ?: "Time needs review"}", style = MaterialTheme.typography.bodyMedium)
                if (result.dateSuggestions.isNotEmpty()) {
                    Text(if (result.continuousDateRange) "Detected date range — confirm to fill the form" else "Detected dates — choose this event's occurrence", style = MaterialTheme.typography.bodyMedium)
                    if (result.dateSuggestions.any { it.year == null }) {
                        OutlinedTextField(confirmedYear, onValueChange = {
                            confirmedYear = it.filter(Char::isDigit).take(4); chosenDate = null; chosenEndDate = null; applied = false
                        }, label = { Text("Confirm the year (not printed on poster)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        TextButton(onClick = { confirmedYear = java.time.LocalDate.now().year.toString(); chosenDate = null; chosenEndDate = null; applied = false }) {
                            Text("Use ${java.time.LocalDate.now().year} — only if correct")
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(result.dateSuggestions) { suggestion ->
                            val resolved = suggestion.resolve(suggestion.year ?: chosenYear)
                            FilterChip(selected = resolved != null && chosenDate == resolved, enabled = enabled && resolved != null,
                                onClick = { chosenDate = resolved; chosenEndDate = null; applied = false }, label = { Text(suggestion.label) })
                        }
                    }
                    if (result.continuousDateRange) {
                        val resolvedDates = result.dateSuggestions.mapNotNull { it.resolve(it.year ?: chosenYear) }.sorted()
                        TextButton(enabled = enabled && resolvedDates.size == result.dateSuggestions.size && resolvedDates.size > 1, onClick = {
                            chosenDate = resolvedDates.first(); chosenEndDate = resolvedDates.last(); applied = false
                        }) { Text("Use first → last day as a continuous event") }
                    }
                    chosenDate?.let { Text("Selected start: $it${chosenEndDate?.let { end -> " · End: $end" }.orEmpty()}") }
                }
                if (result.timeSuggestions.size > 1) {
                    Text("Multiple shows/times — choose a start time")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(result.timeSuggestions) { option ->
                            FilterChip(selected = chosenTime == option, enabled = enabled, onClick = { chosenTime = option; applied = false }, label = { Text(option) })
                        }
                    }
                }
                if (weak) Text("Some text was difficult to read. Compare every field with the poster.", color = MaterialTheme.colorScheme.error)
                Text("Names and places stay in the poster's original language. No translation or verification is claimed.", style = MaterialTheme.typography.bodySmall)
                result.warnings.forEach { Text("• $it", style = MaterialTheme.typography.bodySmall) }
                TextButton(onClick = { showText = !showText }) { Text(if (showText) "Hide recognized text" else "View / correct recognized text") }
                if (showText) {
                    OutlinedTextField(rawText.orEmpty(), onValueChange = { rawText = it.take(20_000); applied = false; resetChoices() },
                        modifier = Modifier.fillMaxWidth(), label = { Text("Recognized text") }, minLines = 3, maxLines = 8)
                    TextButton(onClick = {
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Poi poster reader test\nLanguage: ${language.label}\n\n${rawText.orEmpty()}\n\nPlease describe any wrong or missing fields.")
                        }, "Share scan feedback"))
                    }) { Text("Share text for feedback") }
                }
                Button(enabled = enabled, onClick = {
                    onDraft(result.copy(date = chosenDate ?: result.date, endDate = chosenEndDate ?: result.endDate, time = chosenTime ?: result.time)); applied = true
                }) { Text("Use this draft") }
            }
            Text("Offline reader · no scan fees · no image upload. The poster itself is not attached to the published event.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
