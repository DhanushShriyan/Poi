package com.dhanushshriyan.poi.retrolab.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dhanushshriyan.poi.retrolab.data.*
import java.util.Locale

@Composable
fun MomentsScreen(state: DemoState, onComments: (String) -> Unit, onEvent: (String) -> Unit) {
    val context = LocalContext.current
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { PageHeading("04 / THE AFTERGLOW", "Good times.\nKept here.", "A photo-first social feed in the retro palette. Likes and comments work locally in this demo.") }
        items(demoMoments, key = DemoMoment::id) { moment ->
            val liked = state.likes[moment.id] == true
            val commentCount = moment.comments.size + state.comments[moment.id].orEmpty().size
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).clip(TicketShape)
                .background(LightCream).border(1.2.dp, Ink, TicketShape)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    GeometryMark(Modifier.size(29.dp), demoMoments.indexOf(moment))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Mono(moment.author)
                        Text("Sample event moment", style = MaterialTheme.typography.bodySmall, color = MutedInk)
                    }
                    Mono("0" + (demoMoments.indexOf(moment) + 1))
                }
                Rule()
                Box(Modifier.fillMaxWidth().clickable { onEvent(moment.eventId) }) {
                    RetroPhoto(moment.image, moment.caption, Modifier.fillMaxWidth().aspectRatio(1.18f))
                    Row(Modifier.align(Alignment.TopEnd).padding(12.dp).width(74.dp).height(9.dp).border(1.dp, Ink)) {
                        Box(Modifier.weight(0.55f).fillMaxHeight().background(Rust))
                        Box(Modifier.weight(0.25f).fillMaxHeight().background(Cream))
                        Box(Modifier.weight(0.2f).fillMaxHeight().background(Gold))
                    }
                }
                Rule()
                Text(moment.caption, Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Rule()
                Row(Modifier.fillMaxWidth().padding(horizontal = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                    RetroIconButton(if (liked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        if (liked) "Unlike photo" else "Like photo", { state.like(moment.id) }, if (liked) Rust else Ink)
                    Mono((moment.likes + if (liked) 1 else 0).toString())
                    Spacer(Modifier.width(10.dp))
                    RetroIconButton(Icons.AutoMirrored.Outlined.Chat, "View comments", { onComments(moment.id) })
                    Mono(commentCount.toString())
                    Spacer(Modifier.weight(1f))
                    RetroIconButton(Icons.Outlined.Share, "Share demo moment", {
                        sharePreview(context, "Poi Retro Lab · sample moment", moment.caption)
                    })
                }
            }
        }
        item { Mono("SAMPLE PHOTOS / NO REAL POSTS OR UPLOADS", Modifier.padding(horizontal = 20.dp), MutedInk) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(state: DemoState, moment: DemoMoment, onDismiss: () -> Unit) {
    var text by rememberSaveable(moment.id) { mutableStateOf("") }
    val comments = moment.comments + state.comments[moment.id].orEmpty().map { "You: $it" }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = TicketShape,
        containerColor = Cream,
        dragHandle = { Box(Modifier.padding(top = 10.dp).width(42.dp).height(4.dp).background(Ink)) },
    ) {
        Column(Modifier.fillMaxWidth().imePadding().padding(horizontal = 20.dp).padding(bottom = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("In the conversation.", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                RetroIconButton(Icons.Outlined.Close, "Close comments", onDismiss)
            }
            Mono("DEMO COMMENTS / STORED ONLY ON THIS PHONE")
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 220.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(comments.size) { index ->
                    Text(comments[index], Modifier.fillMaxWidth().background(LightCream).border(1.dp, Ink).padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
            OutlinedTextField(
                value = text,
                onValueChange = { if (it.length <= 280) text = it },
                modifier = Modifier.fillMaxWidth(), shape = TicketShape,
                placeholder = { Text("Add your test comment...") },
                minLines = 2, maxLines = 4,
                supportingText = { Mono(text.length.toString() + " / 280") },
            )
            Button(
                onClick = { if (state.addComment(moment.id, text)) text = "" },
                enabled = normalizeComment(text) != null,
                shape = TicketShape, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Ink, contentColor = Cream),
            ) { Text("Post locally") }
        }
    }
}

@Composable
fun EventDetailScreen(state: DemoState, event: DemoEvent, onBack: () -> Unit, onMoments: () -> Unit) {
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            RetroIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back to previous page", onBack)
            Mono("EVENT / " + (state.events.indexOf(event) + 1).toString().padStart(2, '0'), Modifier.weight(1f))
            RetroIconButton(Icons.Outlined.Share, "Share demo event", {
                sharePreview(context, event.title, event.dateLabel() + " · " + event.timeLabel() + "\n" + event.venue)
            })
        }
        Rule()
        Column(Modifier.fillMaxWidth().background(categoryColor(event.category)).padding(20.dp)) {
            Mono(event.category.label.uppercase(Locale.ENGLISH) + " / " + event.admission,
                color = foreground(categoryColor(event.category)))
            Spacer(Modifier.height(10.dp))
            Text(event.title, style = MaterialTheme.typography.displayMedium, color = foreground(categoryColor(event.category)))
            Spacer(Modifier.height(12.dp))
            Mono("BY " + event.host, color = foreground(categoryColor(event.category)))
        }
        Rule()
        RetroPhoto(event.image, event.title, Modifier.fillMaxWidth().aspectRatio(1.35f))
        Rule()
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Fact("WHEN", event.dateLabel() + "\n" + event.timeLabel(), Modifier.weight(1f))
            Box(Modifier.fillMaxHeight().width(1.dp).background(Ink))
            Fact("WHERE", event.venue + "\n" + event.distanceKm + " km · sample", Modifier.weight(1f))
        }
        Rule()
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("A little more about it.", style = MaterialTheme.typography.titleLarge)
            Text(event.description, style = MaterialTheme.typography.bodyLarge)
            Mono(event.note.uppercase(Locale.ENGLISH), color = MutedInk)
            Rule()
            Row(verticalAlignment = Alignment.CenterVertically) {
                GeometryMark(Modifier.size(34.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(event.going.toString() + " going", style = MaterialTheme.typography.titleMedium)
                    Text("Sample community activity", style = MaterialTheme.typography.bodySmall, color = MutedInk)
                }
            }
            RsvpControls(state.rsvps[event.id] ?: Rsvp.NONE, { state.respond(event.id, it) }, Modifier.fillMaxWidth())
            RetroAction("Browse sample moments", onMoments, Modifier.fillMaxWidth())
            Text("Preview only. This is a fictional event; choosing a response does not reserve a place or record real attendance.",
                style = MaterialTheme.typography.bodySmall, color = MutedInk)
        }
    }
}
@Composable
private fun Fact(label: String, value: String, modifier: Modifier) {
    Column(modifier.padding(18.dp)) {
        Mono(label, color = Teal)
        Spacer(Modifier.height(7.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
fun sharePreview(context: Context, title: String, body: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, title + "\n" + body + "\n\nFictional sample from Poi Retro Lab. Not a real event listing.")
    }
    try {
        context.startActivity(Intent.createChooser(intent, "Share theme demo"))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No sharing app is available on this device.", Toast.LENGTH_SHORT).show()
    }
}

