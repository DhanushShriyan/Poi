package com.poi.feature.localcalendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.format.DateTimeFormatter

@Composable
fun LocalCalendarEntryCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val datedEntries = January2026Calendar.days.count { it.observances.isNotEmpty() }
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        ),
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.padding(10.dp).size(22.dp),
                        tint = MaterialTheme.colorScheme.secondary,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "LOCAL CALENDAR",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        letterSpacing = 1.sp,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("Explore January 2026", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "$datedEntries dates · ${January2026Calendar.SOURCE}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "Open local calendar",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                ObservanceDot(LocalObservanceKind.LOCAL)
                ObservanceDot(LocalObservanceKind.FESTIVAL, alpha = 0.55f)
                ObservanceDot(LocalObservanceKind.CIVIC, alpha = 0.55f)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalCalendarScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedDay by remember { mutableIntStateOf(25) }
    var showDetails by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val preferences = remember(context) {
        context.getSharedPreferences(LOCAL_CALENDAR_PREFERENCES, 0)
    }
    var language by remember(preferences) {
        mutableStateOf(
            LocalCalendarLanguage.fromStorage(
                preferences.getString(LOCAL_CALENDAR_LANGUAGE, null),
            ),
        )
    }
    val strings = language.strings
    val selected = January2026Calendar.day(selectedDay)

    fun selectLanguage(selectedLanguage: LocalCalendarLanguage) {
        language = selectedLanguage
        preferences.edit().putString(LOCAL_CALENDAR_LANGUAGE, selectedLanguage.storageValue).apply()
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(strings.screenTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = strings.back)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Column {
                    Text(strings.monthTitle, style = MaterialTheme.typography.headlineLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        strings.dateHint,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    LanguageSelector(
                        language = language,
                        onLanguageSelected = ::selectLanguage,
                    )
                }
            }
            item {
                MonthGrid(
                    selectedDay = selectedDay,
                    language = language,
                    onDaySelected = { selectedDay = it },
                )
            }
            item { CalendarLegend(language) }
            item {
                DayPreviewCard(
                    day = selected,
                    language = language,
                    onViewAll = { showDetails = true },
                )
            }
        }
    }

    if (showDetails) {
        DayDetailsSheet(
            day = selected,
            language = language,
            onDismiss = { showDetails = false },
        )
    }
}

@Composable
private fun LanguageSelector(
    language: LocalCalendarLanguage,
    onLanguageSelected: (LocalCalendarLanguage) -> Unit,
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
            selected = language == LocalCalendarLanguage.ENGLISH,
            onClick = { onLanguageSelected(LocalCalendarLanguage.ENGLISH) },
            label = { Text("English") },
        )
        FilterChip(
            selected = language == LocalCalendarLanguage.KANNADA,
            onClick = { onLanguageSelected(LocalCalendarLanguage.KANNADA) },
            label = { Text("ಕನ್ನಡ") },
        )
    }
}

@Composable
private fun MonthGrid(
    selectedDay: Int,
    language: LocalCalendarLanguage,
    onDaySelected: (Int) -> Unit,
) {
    val leadingEmptyCells = January2026Calendar.month.atDay(1).dayOfWeek.value % 7
    val cells = buildList<LocalCalendarDay?> {
        repeat(leadingEmptyCells) { add(null) }
        addAll(January2026Calendar.days)
        while (size % 7 != 0) add(null)
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth()) {
            language.strings.weekdays.forEach { weekday ->
                Text(
                    text = weekday,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f).aspectRatio(0.9f))
                    } else {
                        MonthDayCell(
                            day = day,
                            selected = selectedDay == day.date.dayOfMonth,
                            onClick = { onDaySelected(day.date.dayOfMonth) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MonthDayCell(
    day: LocalCalendarDay,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kinds = day.observances.map { it.kind }.distinct().take(3)
    Box(
        modifier = modifier.aspectRatio(0.9f).padding(2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize().clickable(onClick = onClick),
            shape = CircleShape,
            color = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.height(6.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    kinds.forEach { ObservanceDot(it, size = 5) }
                }
            }
        }
    }
}

@Composable
private fun CalendarLegend(language: LocalCalendarLanguage) {
    val strings = language.strings
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LegendItem(LocalObservanceKind.LOCAL, strings.local)
        LegendItem(LocalObservanceKind.FESTIVAL, strings.festival)
        LegendItem(LocalObservanceKind.CIVIC, strings.civic)
    }
}

@Composable
private fun LegendItem(kind: LocalObservanceKind, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ObservanceDot(kind, size = 8)
        Spacer(Modifier.width(7.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DayPreviewCard(
    day: LocalCalendarDay,
    language: LocalCalendarLanguage,
    onViewAll: () -> Unit,
) {
    val strings = language.strings
    val formatter = remember(language) {
        DateTimeFormatter.ofPattern("EEE · d MMM", language.locale)
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Text(
                day.date.format(formatter).uppercase(language.locale),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                if (day.observances.isEmpty()) strings.noObservances else strings.observanceCount(day.observances.size),
                style = MaterialTheme.typography.titleLarge,
            )
            if (day.observances.isEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.emptyDayHint,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Spacer(Modifier.height(12.dp))
                day.observances.take(2).forEachIndexed { index, observance ->
                    PreviewRow(observance, language)
                    if (index == 0 && day.observances.size > 1) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onViewAll, contentPadding = PaddingValues(0.dp)) {
                    Text(strings.viewAll(day.observances.size))
                    Spacer(Modifier.width(4.dp))
                    Icon(Icons.Default.ChevronRight, contentDescription = null, Modifier.size(18.dp))
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(17.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    January2026Calendar.SOURCE,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PreviewRow(
    observance: LocalObservance,
    language: LocalCalendarLanguage,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        ObservanceDot(observance.kind, size = 8)
        Spacer(Modifier.width(12.dp))
        Text(
            observance.displayTitle(language),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(19.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DayDetailsSheet(
    day: LocalCalendarDay,
    language: LocalCalendarLanguage,
    onDismiss: () -> Unit,
) {
    var expanded by remember(day.date) { mutableStateOf(false) }
    val featured = day.observances.firstOrNull { it.featured }
    val regular = day.observances.filterNot { it.featured }
    val visibleRegular = if (expanded) regular else regular.take(3)
    val strings = language.strings
    val dateFormatter = remember(language) {
        DateTimeFormatter.ofPattern("EEEE, d MMMM", language.locale)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Text(day.date.format(dateFormatter), style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.observanceCount(day.observances.size),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            featured?.let { item { FeaturedObservance(it, language) } }
            if (regular.isNotEmpty()) {
                item {
                    Text(
                        strings.localObservances,
                        style = MaterialTheme.typography.labelLarge,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                items(visibleRegular.size) { index ->
                    DetailObservanceRow(visibleRegular[index], language)
                }
                if (regular.size > 3) {
                    item {
                        TextButton(
                            onClick = { expanded = !expanded },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (expanded) strings.showLess else strings.showMore(regular.size - 3))
                            Spacer(Modifier.width(4.dp))
                            Icon(
                                Icons.Default.ExpandMore,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    SunTimeCard(strings.sunrise, day.sunrise.formatTime(language), Modifier.weight(1f))
                    SunTimeCard(strings.sunset, day.sunset.formatTime(language), Modifier.weight(1f))
                }
            }
            item {
                Text(
                    strings.source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FeaturedObservance(
    observance: LocalObservance,
    language: LocalCalendarLanguage,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.13f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        shape = MaterialTheme.shapes.medium,
        border = CardDefaults.outlinedCardBorder(),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    language.strings.featured,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                )
                Text(observance.displayTitle(language), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun DetailObservanceRow(
    observance: LocalObservance,
    language: LocalCalendarLanguage,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.Top,
        ) {
            ObservanceDot(observance.kind, size = 8)
            Spacer(Modifier.width(12.dp))
            Text(
                observance.displayTitle(language),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}

@Composable
private fun SunTimeCard(label: String, time: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.WbSunny,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Spacer(Modifier.width(10.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(time, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ObservanceDot(kind: LocalObservanceKind, size: Int = 7, alpha: Float = 1f) {
    Box(
        Modifier
            .size(size.dp)
            .background(kind.dotColor().copy(alpha = alpha), CircleShape),
    )
}

@Composable
private fun LocalObservanceKind.dotColor(): Color = when (this) {
    LocalObservanceKind.LOCAL -> MaterialTheme.colorScheme.tertiary
    LocalObservanceKind.FESTIVAL -> MaterialTheme.colorScheme.secondary
    LocalObservanceKind.CIVIC -> MaterialTheme.colorScheme.primary
}

private fun java.time.LocalTime.formatTime(language: LocalCalendarLanguage): String =
    format(DateTimeFormatter.ofPattern("h:mm a", language.locale))

private const val LOCAL_CALENDAR_PREFERENCES = "poi_local_calendar"
private const val LOCAL_CALENDAR_LANGUAGE = "language"
