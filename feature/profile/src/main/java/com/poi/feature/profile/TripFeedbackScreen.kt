package com.poi.feature.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.poi.core.data.TripFeedback
import com.poi.core.data.TripFeedbackRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripFeedbackScreen(repository: TripFeedbackRepository, appVersion: String, onBack: () -> Unit) {
 var category by rememberSaveable { mutableStateOf("other") }
 var rating by rememberSaveable { mutableStateOf(4) }
 var message by rememberSaveable { mutableStateOf("") }
 var status by remember { mutableStateOf<String?>(null) }
 var saving by remember { mutableStateOf(false) }
 var history by remember { mutableStateOf(emptyList<TripFeedback>()) }
 val scope = rememberCoroutineScope()
 LaunchedEffect(repository) { runCatching { repository.history() }.onSuccess { history = it }.onFailure { status = "Could not load feedback. Check your connection." } }
 Scaffold(topBar = { TopAppBar(title = { Text("Trip testing feedback") }, navigationIcon = { TextButton(onClick = onBack) { Text("Back") } }) }) { padding ->
  LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
   item {
    Text("Help us improve Poi", style = MaterialTheme.typography.headlineMedium)
    Text("Tell us what happened, what you expected, and how to repeat it. Do not include passwords, OTPs or payment details.")
   }
   item { Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) { listOf("login", "photos", "expenses").forEach { option -> FilterChip(category == option, onClick = { category = option }, label = { Text(option.replaceFirstChar(Char::uppercase)) }) } } }
   item { Row { listOf("events", "other").forEach { option -> FilterChip(category == option, onClick = { category = option }, label = { Text(option.replaceFirstChar(Char::uppercase)) }) } } }
   item { Text("Your experience · 1 = difficult, 5 = great"); Row { (1..5).forEach { value -> FilterChip(rating == value, onClick = { rating = value }, label = { Text("$value ★") }) } } }
   item { OutlinedTextField(message, onValueChange = { message = it.take(2000) }, label = { Text("What worked or went wrong?") }, modifier = Modifier.fillMaxWidth(), minLines = 5) }
   item {
    Button(enabled = !saving && message.trim().length >= 10, modifier = Modifier.fillMaxWidth(), onClick = {
     saving = true; status = null
     scope.launch {
      runCatching { repository.submit(category, rating, message, appVersion) }
       .onSuccess { message = ""; status = "Saved to Poi's database. Thank you!"; runCatching { repository.history() }.onSuccess { history = it } }
       .onFailure { status = "Feedback was not saved. Check your connection and retry; your text is still here." }
      saving = false
     }
    }) { Text(if (saving) "Saving…" else "Send feedback") }
    status?.let { Text(it, Modifier.padding(top = 10.dp)) }
   }
   item { Text("Your submitted feedback", style = MaterialTheme.typography.titleLarge) }
   items(history, key = { it.id }) { feedback -> Card { Column(Modifier.padding(16.dp)) { Text("${feedback.category} · ${feedback.rating}/5 · ${feedback.appVersion}"); Text(feedback.message); Text(feedback.createdAt.take(10), style = MaterialTheme.typography.bodySmall) } } }
  }
 }
}
