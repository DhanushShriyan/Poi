package com.dhanushshriyan.poi.retrolab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dhanushshriyan.poi.retrolab.data.*
import com.dhanushshriyan.poi.retrolab.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Cream.toArgb(), Cream.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Cream.toArgb(), Cream.toArgb()),
        )
        setContent { RetroTheme { RetroApp() } }
    }
}
private enum class Page(val label: String) {
    FEED("Feed"), SEARCH("Search"), PLANS("Plans"), MOMENTS("Moments"), PROFILE("Profile"),
}
@Composable
private fun RetroApp() {
    val context = LocalContext.current
    val state = remember { DemoState(context.applicationContext) }
    var page by rememberSaveable { mutableStateOf(Page.FEED) }
    var eventId by rememberSaveable { mutableStateOf<String?>(null) }
    var commentId by rememberSaveable { mutableStateOf<String?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<Category?>(null) }
    var radius by rememberSaveable { mutableStateOf<Int?>(25) }
    val holder = rememberSaveableStateHolder()
    val event = state.events.firstOrNull { it.id == eventId }

    BackHandler(enabled = event != null) { eventId = null }

    Column(Modifier.fillMaxSize().background(Cream).systemBarsPadding()) {
        if (event == null) {
            Row(Modifier.fillMaxWidth().padding(start = 18.dp, end = 8.dp, top = 7.dp, bottom = 7.dp),
                verticalAlignment = Alignment.CenterVertically) {
                GeometryMark(Modifier.size(29.dp))
                Spacer(Modifier.width(9.dp))
                Text("poi.", fontSize = 38.sp, lineHeight = 39.sp, letterSpacing = (-2).sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Mono("GEOMETRIC RETRO")
                    Mono("THEME LAB / NO. 02", color = MutedInk)
                }
                RetroIconButton(Icons.Outlined.GridView, "About this theme", { page = Page.PROFILE })
            }
            Rule()
        }
        Box(Modifier.weight(1f)) {
            if (event != null) {
                EventDetailScreen(state, event, { eventId = null }, {
                    eventId = null
                    page = Page.MOMENTS
                })
            } else {
                holder.SaveableStateProvider(page.name) {
                    when (page) {
                        Page.FEED, Page.SEARCH -> DiscoveryScreen(
                            state, page == Page.SEARCH, query, category, radius,
                            { query = it }, { category = it }, { radius = it }, { eventId = it },
                        )
                        Page.PLANS -> PlansScreen(state) { eventId = it }
                        Page.MOMENTS -> MomentsScreen(state, { commentId = it }, { eventId = it })
                        Page.PROFILE -> ProfileScreen(state)
                    }
                }
            }
        }
        if (event == null) ColorBlockNavigation(page) { page = it }
    }
    demoMoments.firstOrNull { it.id == commentId }?.let { moment ->
        CommentsSheet(state, moment, { commentId = null })
    }
}

@Composable
private fun ColorBlockNavigation(selected: Page, onSelect: (Page) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = 72.dp).height(IntrinsicSize.Min)) {
        Page.entries.forEach { page ->
            val fill = when (page) {
                Page.FEED -> Gold
                Page.SEARCH -> Teal
                Page.PLANS -> Rust
                Page.MOMENTS -> Cream
                Page.PROFILE -> Rust
            }
            val foreground = foreground(fill)
            Column(Modifier.weight(1f).fillMaxHeight().background(fill).border(0.75.dp, Ink)
                .selectable(selected = page == selected, role = Role.Tab, onClick = { onSelect(page) })
                .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center) {
                Icon(
                    imageVector = when (page) {
                        Page.FEED -> Icons.Outlined.Home
                        Page.SEARCH -> Icons.Outlined.Search
                        Page.PLANS -> Icons.Outlined.CalendarMonth
                        Page.MOMENTS -> Icons.Outlined.PhotoCamera
                        Page.PROFILE -> Icons.Outlined.PersonOutline
                    },
                    contentDescription = null, tint = foreground, modifier = Modifier.size(23.dp),
                )
                Spacer(Modifier.height(4.dp))
                Mono(page.label.uppercase(), color = foreground)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(22.dp).height(2.dp).background(if (selected == page) foreground else Color.Transparent))
            }
        }
    }
}

