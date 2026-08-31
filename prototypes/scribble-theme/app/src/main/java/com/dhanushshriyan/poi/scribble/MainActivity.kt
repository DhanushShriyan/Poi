package com.dhanushshriyan.poi.scribble

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.IosShare
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.cos
import kotlin.math.sin

private val Paper = Color(0xFFF7F1E3)
private val CardPaper = Color(0xFFFFFBF1)
private val Ink = Color(0xFF1B1B19)
private val SoftInk = Color(0xFF59564F)
private val Cobalt = Color(0xFF496EDB)
private val Coral = Color(0xFFF46F5E)
private val Mustard = Color(0xFFF4C44F)
private val Sage = Color(0xFF9DB7A0)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScribbleTheme {
                ScribbleApp()
            }
        }
    }
}

@Composable
private fun ScribbleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Cobalt,
            onPrimary = Color.White,
            secondary = Coral,
            background = Paper,
            surface = CardPaper,
            onBackground = Ink,
            onSurface = Ink,
        ),
        typography = MaterialTheme.typography.copy(
            displayLarge = TextStyle(
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Normal,
                fontSize = 44.sp,
                lineHeight = 43.sp,
                letterSpacing = (-0.8).sp,
            ),
            headlineMedium = TextStyle(
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = 30.sp,
                lineHeight = 31.sp,
            ),
            titleLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Black,
                fontSize = 23.sp,
                lineHeight = 26.sp,
            ),
            titleMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                lineHeight = 20.sp,
            ),
            bodyLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
                lineHeight = 23.sp,
            ),
            bodyMedium = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            ),
            labelLarge = TextStyle(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
            ),
        ),
        content = content,
    )
}

private enum class DemoTab(val label: String) {
    DISCOVER("Discover"),
    PLANS("Plans"),
    PROFILE("My page"),
}

@Composable
private fun ScribbleApp() {
    var selectedTab by rememberSaveable { mutableStateOf(DemoTab.DISCOVER) }
    var selectedDay by rememberSaveable { mutableStateOf(26) }
    var selectedEventId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var maxDistance by rememberSaveable { mutableStateOf<Int?>(25) }
    val statusByEvent = remember {
        mutableStateMapOf("rooftop-radio" to CrowdIntent.INTERESTED)
    }
    val likedEvents = remember { mutableStateMapOf<String, Boolean>() }
    val selectedEvent = selectedEventId?.let { id -> demoEvents.firstOrNull { it.id == id } }

    PaperTexture {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding(),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                if (selectedEvent != null) {
                    EventDetailScreen(
                        event = selectedEvent,
                        status = statusByEvent[selectedEvent.id] ?: CrowdIntent.NONE,
                        isLiked = likedEvents[selectedEvent.id] == true,
                        onBack = { selectedEventId = null },
                        onStatus = { statusByEvent[selectedEvent.id] = it },
                        onLike = { likedEvents[selectedEvent.id] = likedEvents[selectedEvent.id] != true },
                    )
                } else {
                    when (selectedTab) {
                        DemoTab.DISCOVER -> DiscoverScreen(
                            selectedDay = selectedDay,
                            query = query,
                            maxDistance = maxDistance,
                            statusByEvent = statusByEvent,
                            likedEvents = likedEvents,
                            onDaySelected = { selectedDay = it },
                            onQueryChanged = { query = it },
                            onRadiusClicked = { maxDistance = nextRadius(maxDistance) },
                            onEventSelected = { selectedEventId = it.id },
                            onStatus = { event, status -> statusByEvent[event.id] = status },
                            onLike = { event -> likedEvents[event.id] = likedEvents[event.id] != true },
                        )

                        DemoTab.PLANS -> PlansScreen(
                            statusByEvent = statusByEvent,
                            onEventSelected = { selectedEventId = it.id },
                        )

                        DemoTab.PROFILE -> ProfileScreen(statusByEvent)
                    }
                }
            }

            if (selectedEvent == null) {
                ScribbleBottomBar(
                    selected = selectedTab,
                    onSelected = { selectedTab = it },
                )
            }
        }
    }
}

@Composable
private fun PaperTexture(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            repeat(90) { index ->
                val x = ((index * 83) % 997) / 997f * size.width
                val y = ((index * 137) % 991) / 991f * size.height
                drawCircle(
                    color = Ink.copy(alpha = if (index % 5 == 0) 0.055f else 0.025f),
                    radius = if (index % 7 == 0) 1.5.dp.toPx() else 0.8.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, y),
                )
            }
        }
        content()
    }
}

@Composable
private fun DiscoverScreen(
    selectedDay: Int,
    query: String,
    maxDistance: Int?,
    statusByEvent: Map<String, CrowdIntent>,
    likedEvents: Map<String, Boolean>,
    onDaySelected: (Int) -> Unit,
    onQueryChanged: (String) -> Unit,
    onRadiusClicked: () -> Unit,
    onEventSelected: (DemoEvent) -> Unit,
    onStatus: (DemoEvent, CrowdIntent) -> Unit,
    onLike: (DemoEvent) -> Unit,
) {
    val visibleEvents = filterDemoEvents(
        events = demoEvents,
        selectedDay = selectedDay,
        query = query,
        maxDistanceKm = maxDistance,
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            ScribbleHeader(
                query = query,
                maxDistance = maxDistance,
                onQueryChanged = onQueryChanged,
                onRadiusClicked = onRadiusClicked,
            )
        }
        item {
            DateNavigation(
                selectedDay = selectedDay,
                onDaySelected = onDaySelected,
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (selectedDay == 26) "Today, worth circling" else "Page for Aug $selectedDay",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        text = "${visibleEvents.size} hand-picked plan${if (visibleEvents.size == 1) "" else "s"}",
                        color = SoftInk,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                DoodleArrow(modifier = Modifier.size(width = 62.dp, height = 34.dp))
            }
        }

        if (visibleEvents.isEmpty()) {
            item {
                EmptyPage(
                    title = "Nothing inked here yet",
                    body = "Try another date, distance or search. The blank page is part of the charm.",
                )
            }
        } else {
            items(visibleEvents, key = DemoEvent::id) { event ->
                EventCard(
                    event = event,
                    status = statusByEvent[event.id] ?: CrowdIntent.NONE,
                    isLiked = likedEvents[event.id] == true,
                    onOpen = { onEventSelected(event) },
                    onStatus = { onStatus(event, it) },
                    onLike = { onLike(event) },
                )
            }
        }
    }
}

@Composable
private fun ScribbleHeader(
    query: String,
    maxDistance: Int?,
    onQueryChanged: (String) -> Unit,
    onRadiusClicked: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stamp(text = "POI / THE SCRIBBLE CUT", color = Cobalt)
            Spacer(modifier = Modifier.weight(1f))
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Ink)
                    .roughBorder(CardPaper, 1.dp)
                    .clickable { },
                contentAlignment = Alignment.Center,
            ) {
                Text("D", color = CardPaper, fontWeight = FontWeight.Black)
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Plans worth\ncircling.",
            style = MaterialTheme.typography.displayLarge,
        )
        Text(
            text = "A rough-around-the-edges visual experiment for finding your next story.",
            modifier = Modifier.widthIn(max = 330.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = SoftInk,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SearchBox(
                value = query,
                onValueChange = onQueryChanged,
                modifier = Modifier.weight(1f),
            )
            InkChip(
                text = maxDistance?.let { "$it km" } ?: "Any km",
                fill = Mustard,
                onClick = onRadiusClicked,
            )
        }
    }
}

@Composable
private fun SearchBox(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .height(48.dp)
            .background(CardPaper)
            .roughBorder()
            .padding(horizontal = 14.dp),
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = Ink),
        decorationBox = { inner ->
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Explore, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Box {
                    if (value.isBlank()) {
                        Text("Search the page...", color = SoftInk)
                    }
                    inner()
                }
            }
        },
    )
}

@Composable
private fun DateNavigation(
    selectedDay: Int,
    onDaySelected: (Int) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DoodleNavArrow(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Previous date",
                onClick = {
                    val index = datePages.indexOfFirst { it.day == selectedDay }
                    if (index > 0) onDaySelected(datePages[index - 1].day)
                },
            )
            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 7.dp),
            ) {
                items(datePages, key = DatePage::day) { page ->
                    DateTicket(
                        page = page,
                        selected = selectedDay == page.day,
                        onClick = { onDaySelected(page.day) },
                    )
                }
            }
            DoodleNavArrow(
                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = "Next date",
                onClick = {
                    val index = datePages.indexOfFirst { it.day == selectedDay }
                    if (index in 0 until datePages.lastIndex) onDaySelected(datePages[index + 1].day)
                },
            )
        }
        DoodleDivider(modifier = Modifier.padding(horizontal = 18.dp))
    }
}

@Composable
private fun DateTicket(page: DatePage, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(78.dp)
            .height(92.dp)
            .graphicsLayer(rotationZ = if (page.day % 2 == 0) -1.0f else 0.8f)
            .background(if (selected) Mustard else CardPaper)
            .roughBorder(width = if (selected) 2.dp else 1.4.dp)
            .clickable(onClick = onClick)
            .padding(7.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(page.month, fontWeight = FontWeight.Black, fontSize = 12.sp)
            Text(
                text = page.day.toString(),
                fontSize = 31.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = page.note,
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EventCard(
    event: DemoEvent,
    status: CrowdIntent,
    isLiked: Boolean,
    onOpen: () -> Unit,
    onStatus: (CrowdIntent) -> Unit,
    onLike: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .graphicsLayer(rotationZ = event.cardTilt)
            .background(CardPaper)
            .roughBorder(width = 1.6.dp)
            .clickable(onClick = onOpen)
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Stamp(text = event.category, color = categoryColor(event.category))
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${event.month} ${event.day} · ${event.time}",
                style = MaterialTheme.typography.labelLarge,
            )
        }
        Spacer(modifier = Modifier.height(9.dp))
        Text(event.title, style = MaterialTheme.typography.titleLarge)
        Text(
            text = "by ${event.organizer}",
            color = SoftInk,
            fontFamily = FontFamily.Cursive,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(10.dp))

        Box {
            Image(
                painter = painterResource(event.imageRes),
                contentDescription = event.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.48f)
                    .graphicsLayer(rotationZ = -event.cardTilt)
                    .roughBorder(width = 1.4.dp),
                contentScale = ContentScale.Crop,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 18.dp)
                    .width(54.dp)
                    .height(16.dp)
                    .rotate(-4f)
                    .background(Mustard.copy(alpha = 0.88f)),
            )
        }

        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = "${event.place} · ${event.distanceKm} km",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
            Icon(
                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) Coral else Ink,
                modifier = Modifier
                    .size(26.dp)
                    .clickable(onClick = onLike),
            )
        }
        Text(
            text = event.description,
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium,
            color = SoftInk,
        )
        DoodleDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "♡ ${event.interestedCount + if (isLiked) 1 else 0}   ◉ ${event.goingCount}",
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
            Icon(
                Icons.Outlined.IosShare,
                contentDescription = "Share event",
                modifier = Modifier
                    .size(24.dp)
                    .clickable { shareEvent(context, event) },
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IntentButton(
                text = "Interested",
                active = status == CrowdIntent.INTERESTED,
                color = Mustard,
                modifier = Modifier.weight(1f),
                onClick = { onStatus(toggleStatus(status, CrowdIntent.INTERESTED)) },
            )
            IntentButton(
                text = "Going",
                active = status == CrowdIntent.GOING,
                color = Cobalt,
                modifier = Modifier.weight(1f),
                onClick = { onStatus(toggleStatus(status, CrowdIntent.GOING)) },
            )
            IntentButton(
                text = "I'm here",
                active = status == CrowdIntent.HERE,
                color = Coral,
                modifier = Modifier.weight(1f),
                onClick = { onStatus(toggleStatus(status, CrowdIntent.HERE)) },
            )
        }
        Text(
            text = "tap the card for the full page  ↗",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 9.dp),
            textAlign = TextAlign.End,
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Bold,
            color = Cobalt,
        )
    }
}

@Composable
private fun EventDetailScreen(
    event: DemoEvent,
    status: CrowdIntent,
    isLiked: Boolean,
    onBack: () -> Unit,
    onStatus: (CrowdIntent) -> Unit,
    onLike: () -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            InkIconButton(
                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text("event page / ${event.month.lowercase()} ${event.day}", fontFamily = FontFamily.Cursive, fontSize = 19.sp)
            Spacer(modifier = Modifier.weight(1f))
            InkIconButton(
                icon = Icons.Outlined.IosShare,
                contentDescription = "Share",
                onClick = { shareEvent(context, event) },
            )
        }
        Spacer(modifier = Modifier.height(14.dp))
        Stamp(text = event.category, color = categoryColor(event.category))
        Text(event.title, style = MaterialTheme.typography.displayLarge)
        Text(
            text = "Hosted by ${event.organizer}",
            fontFamily = FontFamily.Cursive,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Cobalt,
        )
        Spacer(modifier = Modifier.height(14.dp))
        Image(
            painter = painterResource(event.imageRes),
            contentDescription = event.title,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.33f)
                .graphicsLayer(rotationZ = -0.6f)
                .roughBorder(width = 1.8.dp),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DetailFact(
                icon = Icons.Outlined.Schedule,
                title = "WHEN",
                value = "${event.month} ${event.day}\n${event.time}",
                modifier = Modifier.weight(1f),
            )
            DetailFact(
                icon = Icons.Outlined.LocationOn,
                title = "WHERE",
                value = "${event.place}\n${event.distanceKm} km away",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("The note in the margin", style = MaterialTheme.typography.headlineMedium)
        Text(event.description, style = MaterialTheme.typography.bodyLarge)
        DoodleDivider()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${event.interestedCount} curious · ${event.goingCount} going",
                modifier = Modifier.weight(1f),
                fontFamily = FontFamily.Cursive,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
            )
            Icon(
                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = if (isLiked) "Unlike" else "Like",
                tint = if (isLiked) Coral else Ink,
                modifier = Modifier
                    .size(30.dp)
                    .clickable(onClick = onLike),
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Text("Circle your plan", style = MaterialTheme.typography.headlineMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            IntentButton(
                text = "Interested",
                active = status == CrowdIntent.INTERESTED,
                color = Mustard,
                onClick = { onStatus(toggleStatus(status, CrowdIntent.INTERESTED)) },
            )
            IntentButton(
                text = "Going",
                active = status == CrowdIntent.GOING,
                color = Cobalt,
                onClick = { onStatus(toggleStatus(status, CrowdIntent.GOING)) },
            )
            IntentButton(
                text = "I'm here",
                active = status == CrowdIntent.HERE,
                color = Coral,
                onClick = { onStatus(toggleStatus(status, CrowdIntent.HERE)) },
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Sage.copy(alpha = 0.28f))
                .roughBorder()
                .padding(16.dp),
        ) {
            Text(
                text = "Theme lab note: these actions stay on this phone. This demo never touches your Poi account or database.",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun PlansScreen(
    statusByEvent: Map<String, CrowdIntent>,
    onEventSelected: (DemoEvent) -> Unit,
) {
    val plans = demoEvents.filter { statusByEvent[it.id] != null && statusByEvent[it.id] != CrowdIntent.NONE }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Stamp(text = "YOUR MARGIN", color = Coral)
            Text("Things you circled", style = MaterialTheme.typography.displayLarge)
            Text(
                "A tiny plans page to judge how the scribble system holds up beyond discovery.",
                color = SoftInk,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            DoodleDivider()
        }
        if (plans.isEmpty()) {
            item {
                EmptyPage("No plans circled", "Return to Discover and mark an event Interested, Going or I'm here.")
            }
        } else {
            items(plans, key = DemoEvent::id) { event ->
                MiniPlanCard(
                    event = event,
                    status = statusByEvent[event.id] ?: CrowdIntent.NONE,
                    onClick = { onEventSelected(event) },
                )
            }
        }
    }
}

@Composable
private fun MiniPlanCard(event: DemoEvent, status: CrowdIntent, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardPaper)
            .roughBorder()
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(event.imageRes),
            contentDescription = null,
            modifier = Modifier
                .size(86.dp)
                .graphicsLayer(rotationZ = event.cardTilt)
                .roughBorder(),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(event.title, style = MaterialTheme.typography.titleMedium)
            Text("${event.month} ${event.day} · ${event.time}", color = SoftInk)
            Text(event.place, color = SoftInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(modifier = Modifier.height(6.dp))
            Stamp(
                text = status.label.uppercase(),
                color = when (status) {
                    CrowdIntent.INTERESTED -> Mustard
                    CrowdIntent.GOING -> Cobalt
                    CrowdIntent.HERE -> Coral
                    CrowdIntent.NONE -> Sage
                },
            )
        }
        Text("↗", fontFamily = FontFamily.Cursive, fontSize = 28.sp)
    }
}

@Composable
private fun ProfileScreen(statusByEvent: Map<String, CrowdIntent>) {
    val activePlans = statusByEvent.count { it.value != CrowdIntent.NONE }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(Cobalt)
                    .roughBorder(Ink, 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("D", color = Color.White, fontSize = 38.sp, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Stamp(text = "THEME TESTER", color = Mustard)
                Text("Dhanush's page", style = MaterialTheme.typography.headlineMedium)
                Text("@$activePlans-plans-circled", color = SoftInk)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text("The visual recipe", style = MaterialTheme.typography.displayLarge)
        Text(
            "Warm paper, imperfect ink, useful colour and real event photography. Scribbles should guide the eye—not fight the content.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(modifier = Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PaletteSwatch("PAPER", Paper, Modifier.weight(1f))
            PaletteSwatch("INK", Ink, Modifier.weight(1f))
            PaletteSwatch("BLUE", Cobalt, Modifier.weight(1f))
            PaletteSwatch("CORAL", Coral, Modifier.weight(1f))
        }
        Spacer(modifier = Modifier.height(24.dp))
        DoodleDivider()
        Text("Judge these five things", style = MaterialTheme.typography.headlineMedium)
        JudgeItem("01", "Does it still feel easy to scan?")
        JudgeItem("02", "Do the scribbles add character or become noise?")
        JudgeItem("03", "Do photos and hand-drawn borders belong together?")
        JudgeItem("04", "Would you use this more than a polished minimal theme?")
        JudgeItem("05", "Which one screen should influence the real Poi app?")
        Spacer(modifier = Modifier.height(22.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Mustard.copy(alpha = 0.4f))
                .roughBorder(width = 1.8.dp)
                .padding(16.dp),
        ) {
            Text(
                "Safe experiment · offline only · separate package · no Supabase · no production credentials",
                fontWeight = FontWeight.Black,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun JudgeItem(number: String, text: String) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            number,
            modifier = Modifier
                .size(38.dp)
                .background(CardPaper)
                .roughBorder()
                .wrapContentHeight(Alignment.CenterVertically),
            textAlign = TextAlign.Center,
            fontFamily = FontFamily.Cursive,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun PaletteSwatch(label: String, color: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .graphicsLayer(rotationZ = if (label.length % 2 == 0) -1f else 1f)
                .background(color)
                .roughBorder(Ink),
        )
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
private fun ScribbleBottomBar(selected: DemoTab, onSelected: (DemoTab) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardPaper),
    ) {
        DoodleDivider(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DemoTab.entries.forEach { tab ->
                val active = tab == selected
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelected(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .then(if (active) Modifier.background(Mustard, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = when (tab) {
                                DemoTab.DISCOVER -> Icons.Outlined.Explore
                                DemoTab.PLANS -> Icons.Outlined.CalendarMonth
                                DemoTab.PROFILE -> Icons.Outlined.PersonOutline
                            },
                            contentDescription = tab.label,
                            tint = if (active) Cobalt else Ink,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                    Text(
                        tab.label,
                        fontFamily = if (active) FontFamily.Cursive else FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (active) Cobalt else Ink,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailFact(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .background(CardPaper)
            .roughBorder()
            .padding(11.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(title, fontSize = 10.sp, fontWeight = FontWeight.Black, color = Cobalt)
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun IntentButton(
    text: String,
    active: Boolean,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .height(43.dp)
            .background(if (active) color else Color.Transparent)
            .roughBorder(width = if (active) 2.dp else 1.2.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            textAlign = TextAlign.Center,
            fontFamily = if (active) FontFamily.Cursive else FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun InkChip(text: String, fill: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .height(48.dp)
            .background(fill)
            .roughBorder(width = 1.6.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, fontWeight = FontWeight.Black, fontSize = 12.sp)
    }
}

@Composable
private fun Stamp(text: String, color: Color) {
    Box(
        modifier = Modifier
            .graphicsLayer(rotationZ = -0.7f)
            .background(color)
            .roughBorder(Ink, 1.dp)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            color = if (color == Ink || color == Cobalt) Color.White else Ink,
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.7.sp,
            maxLines = 1,
        )
    }
}

@Composable
private fun DoodleNavArrow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier
            .size(36.dp)
            .clickable(onClick = onClick),
        tint = Ink,
    )
}

@Composable
private fun InkIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .background(CardPaper)
            .roughBorder()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun DoodleDivider(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp),
    ) {
        val path = Path().apply {
            moveTo(0f, size.height * 0.58f)
            val pieces = 18
            repeat(pieces) { index ->
                val x = size.width * (index + 1) / pieces
                val y = size.height * 0.58f + sin(index * 1.7f) * 1.8.dp.toPx()
                lineTo(x, y)
            }
        }
        drawPath(path, Ink.copy(alpha = 0.7f), style = Stroke(1.1.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun DoodleArrow(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.05f, size.height * 0.68f)
            quadraticTo(
                size.width * 0.45f,
                size.height * 0.06f,
                size.width * 0.86f,
                size.height * 0.34f,
            )
        }
        drawPath(path, Ink, style = Stroke(1.8.dp.toPx(), cap = StrokeCap.Round))
        drawLine(Ink, start = androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height * 0.34f), end = androidx.compose.ui.geometry.Offset(size.width * 0.68f, size.height * 0.27f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(Ink, start = androidx.compose.ui.geometry.Offset(size.width * 0.86f, size.height * 0.34f), end = androidx.compose.ui.geometry.Offset(size.width * 0.77f, size.height * 0.54f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
private fun EmptyPage(title: String, body: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .background(CardPaper)
            .roughBorder()
            .padding(24.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("~ ~ ~", fontFamily = FontFamily.Cursive, color = Coral, fontSize = 28.sp)
            Text(title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center, color = SoftInk)
        }
    }
}

private fun Modifier.roughBorder(
    color: Color = Ink,
    width: Dp = 1.4.dp,
): Modifier = drawWithContent {
    drawContent()
    drawRoughFrame(color = color, strokeWidth = width.toPx())
}

private fun DrawScope.drawRoughFrame(color: Color, strokeWidth: Float) {
    val inset = strokeWidth * 1.2f
    val left = inset
    val top = inset
    val right = size.width - inset
    val bottom = size.height - inset
    if (right <= left || bottom <= top) return

    val path = Path().apply {
        moveTo(left, top + wobble(0, strokeWidth))
        val horizontalSegments = 14
        repeat(horizontalSegments) { index ->
            val progress = (index + 1f) / horizontalSegments
            lineTo(left + (right - left) * progress, top + wobble(index + 1, strokeWidth))
        }
        val verticalSegments = 12
        repeat(verticalSegments) { index ->
            val progress = (index + 1f) / verticalSegments
            lineTo(right + wobble(index + 20, strokeWidth), top + (bottom - top) * progress)
        }
        repeat(horizontalSegments) { index ->
            val progress = (index + 1f) / horizontalSegments
            lineTo(right - (right - left) * progress, bottom + wobble(index + 40, strokeWidth))
        }
        repeat(verticalSegments) { index ->
            val progress = (index + 1f) / verticalSegments
            lineTo(left + wobble(index + 60, strokeWidth), bottom - (bottom - top) * progress)
        }
        close()
    }
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawLine(
        color = color.copy(alpha = 0.26f),
        start = androidx.compose.ui.geometry.Offset(left + 4.dp.toPx(), bottom - 1.dp.toPx()),
        end = androidx.compose.ui.geometry.Offset(right - 8.dp.toPx(), bottom + 1.dp.toPx()),
        strokeWidth = (strokeWidth * 0.65f).coerceAtLeast(0.7f),
        cap = StrokeCap.Round,
    )
}

private fun wobble(index: Int, strokeWidth: Float): Float =
    (sin(index * 1.71f) + cos(index * 0.87f)) * strokeWidth * 0.34f

private fun categoryColor(category: String): Color = when (category) {
    "LIVE MUSIC" -> Cobalt
    "MARKET" -> Mustard
    "COMMUNITY" -> Sage
    else -> Coral
}

private fun toggleStatus(current: CrowdIntent, tapped: CrowdIntent): CrowdIntent =
    if (current == tapped) CrowdIntent.NONE else tapped

private fun shareEvent(context: Context, event: DemoEvent) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(
            Intent.EXTRA_TEXT,
            "${event.title} · ${event.month} ${event.day}, ${event.time}\n${event.place}\nShared from the Poi Scribble Theme Lab",
        )
    }
    context.startActivity(Intent.createChooser(shareIntent, "Share this demo event"))
}

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun ScribblePreview() {
    ScribbleTheme {
        ScribbleApp()
    }
}
