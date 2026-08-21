package com.poi.feature.discover

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.poi.core.data.MomentRepository
import com.poi.core.designsystem.PoiInitialAvatar
import com.poi.core.designsystem.PoiSectionHeader
import com.poi.core.model.AttendanceStatus
import com.poi.core.model.EventMoment
import com.poi.core.model.MomentComment
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MAX_IMAGE_BYTES = 6 * 1024 * 1024
private val supportedImageTypes = setOf("image/jpeg", "image/png", "image/webp")

@Composable
internal fun EventMomentsSection(
    eventId: String,
    eventTitle: String,
    repository: MomentRepository,
    attendanceStatus: AttendanceStatus,
    isAuthenticated: Boolean,
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val allMoments by repository.moments.collectAsStateWithLifecycle()
    val allComments by repository.comments.collectAsStateWithLifecycle()
    val moments = allMoments.filter { it.eventId == eventId }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val canContribute = attendanceStatus == AttendanceStatus.HERE ||
        attendanceStatus == AttendanceStatus.ATTENDED
    var selectedImage by remember { mutableStateOf<SelectedImage?>(null) }
    var uploading by remember { mutableStateOf(false) }
    var readingImage by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var commentsFor by remember { mutableStateOf<EventMoment?>(null) }
    var expandedMoment by remember { mutableStateOf<EventMoment?>(null) }
    var deleteMoment by remember { mutableStateOf<EventMoment?>(null) }

    LaunchedEffect(Unit) {
        runCatching { repository.refresh() }
            .onFailure { errorMessage = it.message ?: "Moments could not be refreshed." }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            readingImage = true
            errorMessage = null
            scope.launch {
                runCatching { context.readMomentImage(uri) }
                    .onSuccess { selectedImage = it }
                    .onFailure { errorMessage = it.message ?: "That image could not be opened." }
                readingImage = false
            }
        }
    }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PoiSectionHeader("Event moments")
        Text(
            "Photos shared by people who checked in to this event.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(48.dp).clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Add your view", style = MaterialTheme.typography.titleMedium)
                    Text(
                        when {
                            !repository.isCloudBacked -> "Moments need the live cloud connection."
                            !isAuthenticated -> "Sign in and check in before sharing a photo."
                            canContribute -> "Choose a photo and add an optional caption."
                            else -> "Tap “I'm here” above before sharing a photo."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                FilledTonalButton(
                    enabled = !uploading && !readingImage && repository.isCloudBacked,
                    onClick = {
                        when {
                            !isAuthenticated -> onSignIn()
                            !canContribute -> errorMessage = "Check in to this event before adding a moment."
                            else -> picker.launch("image/*")
                        }
                    },
                ) {
                    if (readingImage) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Text("Add")
                }
            }
        }

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        }

        if (moments.isEmpty()) {
            Card(shape = RoundedCornerShape(22.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        null,
                        Modifier.size(34.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text("No moments yet", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "The first checked-in attendee can start this event's story.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            moments.forEach { moment ->
                MomentCard(
                    moment = moment,
                    comments = allComments.filter { it.momentId == moment.id },
                    onImage = { expandedMoment = moment },
                    onLike = {
                        if (!isAuthenticated) onSignIn() else scope.launch {
                            runCatching { repository.toggleLike(moment) }
                                .onFailure { errorMessage = it.message }
                        }
                    },
                    onComments = { commentsFor = moment },
                    onShare = {
                        scope.launch {
                            runCatching { repository.createShareUrl(moment) }
                                .onSuccess { url -> context.shareMoment(eventTitle, moment, url) }
                                .onFailure { errorMessage = it.message ?: "This moment could not be shared." }
                        }
                    },
                    onDelete = { deleteMoment = moment },
                    onViewed = { scope.launch { repository.recordView(moment.id) } },
                )
            }
        }
    }

    selectedImage?.let { image ->
        AddMomentDialog(
            uploading = uploading,
            onDismiss = { if (!uploading) selectedImage = null },
            onPost = { caption ->
                uploading = true
                errorMessage = null
                scope.launch {
                    runCatching {
                        repository.createMoment(eventId, image.bytes, image.mimeType, caption)
                    }.onSuccess {
                        selectedImage = null
                    }.onFailure {
                        errorMessage = it.message ?: "The moment could not be uploaded."
                    }
                    uploading = false
                }
            },
        )
    }

    commentsFor?.let { moment ->
        CommentsDialog(
            moment = moment,
            comments = allComments.filter { it.momentId == moment.id },
            isAuthenticated = isAuthenticated,
            onDismiss = { commentsFor = null },
            onSignIn = onSignIn,
            onSend = { body ->
                scope.launch {
                    runCatching { repository.addComment(moment.id, body) }
                        .onFailure { errorMessage = it.message ?: "The comment could not be posted." }
                }
            },
        )
    }

    expandedMoment?.let { moment ->
        Dialog(onDismissRequest = { expandedMoment = null }) {
            Surface(shape = RoundedCornerShape(24.dp), color = Color.Black) {
                AsyncImage(
                    model = moment.imageUrl,
                    contentDescription = moment.caption.ifBlank { "Event moment" },
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 640.dp)
                        .clickable { expandedMoment = null },
                )
            }
        }
    }

    deleteMoment?.let { moment ->
        AlertDialog(
            onDismissRequest = { deleteMoment = null },
            title = { Text("Delete this moment?") },
            text = { Text("The photo, its likes, comments, and views will be removed permanently.") },
            confirmButton = {
                TextButton(onClick = {
                    deleteMoment = null
                    scope.launch {
                        runCatching { repository.deleteMoment(moment) }
                            .onFailure { errorMessage = it.message ?: "The moment could not be deleted." }
                    }
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteMoment = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun MomentCard(
    moment: EventMoment,
    comments: List<MomentComment>,
    onImage: () -> Unit,
    onLike: () -> Unit,
    onComments: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onViewed: () -> Unit,
) {
    LaunchedEffect(moment.id) { onViewed() }
    Card(shape = RoundedCornerShape(24.dp)) {
        Column {
            Row(
                Modifier.fillMaxWidth().padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PoiInitialAvatar(moment.authorName, Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(moment.authorName, fontWeight = FontWeight.SemiBold)
                    Text(
                        momentTime(moment.createdAtMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (moment.ownedByCurrentUser) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.DeleteOutline, "Delete moment")
                    }
                }
            }
            AsyncImage(
                model = moment.imageUrl,
                contentDescription = moment.caption.ifBlank { "Photo by ${moment.authorName}" },
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.15f).clickable(onClick = onImage),
            )
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onLike) {
                    Icon(
                        if (moment.likedByCurrentUser) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        "Like",
                        tint = if (moment.likedByCurrentUser) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(moment.likeCount.toString(), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onComments) {
                    Icon(Icons.Default.ChatBubbleOutline, "Comments")
                }
                Text(moment.commentCount.toString(), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.width(12.dp))
                Icon(Icons.Default.Visibility, null, Modifier.size(19.dp))
                Spacer(Modifier.width(5.dp))
                Text(moment.viewCount.toString(), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onShare) { Icon(Icons.Default.Share, "Share moment") }
            }
            Column(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                if (moment.caption.isNotBlank()) {
                    Text(moment.caption, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                }
                comments.takeLast(2).forEach { comment ->
                    Row(Modifier.padding(vertical = 2.dp)) {
                        Text(comment.authorName, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            comment.body,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (moment.commentCount > 0) {
                    TextButton(onClick = onComments) {
                        Text("View all ${moment.commentCount} comments")
                    }
                }
            }
        }
    }
}

@Composable
private fun AddMomentDialog(
    uploading: Boolean,
    onDismiss: () -> Unit,
    onPost: (String) -> Unit,
) {
    var caption by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share this moment") },
        text = {
            Column {
                Text("Your photo will appear under this event for its permitted audience.")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = caption,
                    onValueChange = { if (it.length <= 500) caption = it },
                    label = { Text("Caption (optional)") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("${caption.length}/500", style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(enabled = !uploading, onClick = { onPost(caption) }) {
                if (uploading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else Text("Post moment")
            }
        },
        dismissButton = { TextButton(enabled = !uploading, onClick = onDismiss) { Text("Cancel") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsDialog(
    moment: EventMoment,
    comments: List<MomentComment>,
    isAuthenticated: Boolean,
    onDismiss: () -> Unit,
    onSignIn: () -> Unit,
    onSend: (String) -> Unit,
) {
    var body by remember(moment.id) { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(18.dp)) {
                Text("Comments", style = MaterialTheme.typography.titleLarge)
                Text(
                    "${moment.authorName}'s moment",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                if (comments.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        Text("Start a kind conversation", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(Modifier.heightIn(max = 320.dp)) {
                        items(comments, key = MomentComment::id) { comment ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                                PoiInitialAvatar(comment.authorName, Modifier.size(34.dp))
                                Spacer(Modifier.width(9.dp))
                                Column {
                                    Text(comment.authorName, fontWeight = FontWeight.SemiBold)
                                    Text(comment.body)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (isAuthenticated) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = body,
                            onValueChange = { if (it.length <= 500) body = it },
                            placeholder = { Text("Add a comment") },
                            maxLines = 3,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            enabled = body.isNotBlank(),
                            onClick = {
                                val message = body
                                body = ""
                                onSend(message)
                            },
                        ) { Icon(Icons.AutoMirrored.Filled.Send, "Post comment") }
                    }
                } else {
                    OutlinedButton(onClick = onSignIn, modifier = Modifier.fillMaxWidth()) {
                        Text("Sign in to comment")
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text("Close") }
            }
        }
    }
}

private data class SelectedImage(val bytes: ByteArray, val mimeType: String)

private suspend fun Context.readMomentImage(uri: Uri): SelectedImage = withContext(Dispatchers.IO) {
    val rawType = contentResolver.getType(uri)?.lowercase().orEmpty()
    val mimeType = if (rawType == "image/jpg") "image/jpeg" else rawType
    require(mimeType in supportedImageTypes) { "Choose a JPEG, PNG, or WebP photo." }

    val bytes = contentResolver.openInputStream(uri)?.use { input ->
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            require(total <= MAX_IMAGE_BYTES) { "Photos must be 6 MB or smaller." }
            output.write(buffer, 0, read)
        }
        output.toByteArray()
    } ?: error("Android could not open that photo.")

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "That file is not a readable photo." }
    require(bounds.outWidth <= 12_000 && bounds.outHeight <= 12_000) {
        "This photo's dimensions are too large. Choose a smaller image."
    }
    SelectedImage(bytes, mimeType)
}

private fun Context.shareMoment(eventTitle: String, moment: EventMoment, url: String) {
    val text = buildString {
        append("${moment.authorName} shared a moment from $eventTitle")
        if (moment.caption.isNotBlank()) append("\n${moment.caption}")
        append("\n$url")
        append("\nThis private link expires automatically.")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, "Share event moment"))
}

private fun momentTime(createdAtMillis: Long): String {
    val elapsed = (System.currentTimeMillis() - createdAtMillis).coerceAtLeast(0)
    return when {
        elapsed < TimeUnit.MINUTES.toMillis(1) -> "Just now"
        elapsed < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(elapsed)}m"
        elapsed < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(elapsed)}h"
        else -> "${TimeUnit.MILLISECONDS.toDays(elapsed)}d"
    }
}
