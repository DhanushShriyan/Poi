package com.dhanushshriyan.poi

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.poi.core.data.EventRepository
import com.poi.core.designsystem.PoiTheme
import com.poi.feature.create.CreateEventScreen
import com.poi.feature.discover.DiscoverScreen
import com.poi.feature.discover.EventDetailScreen
import com.poi.feature.plans.PlansScreen
import com.poi.feature.profile.ProfileScreen
import com.poi.feature.profile.SafetyScreen
import com.poi.feature.profile.SettingsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = (application as PoiApplication).eventRepository
        setContent {
            PoiTheme {
                PoiApp(repository)
                PoiUpdatePrompt()
            }
        }
    }
}

private object Routes {
    const val Discover = "discover"
    const val Plans = "plans"
    const val Create = "create"
    const val Profile = "profile"
    const val Event = "event/{eventId}"
    const val Settings = "settings"
    const val Safety = "safety"

    fun event(eventId: String) = "event/$eventId"
}

private data class TopDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)

private val topDestinations = listOf(
    TopDestination(Routes.Discover, "Discover", Icons.Default.Explore, Icons.Outlined.Explore),
    TopDestination(Routes.Plans, "Plans", Icons.Default.CalendarMonth, Icons.Outlined.CalendarMonth),
    TopDestination(Routes.Create, "Create", Icons.Default.AddCircle, Icons.Outlined.AddCircleOutline),
    TopDestination(Routes.Profile, "Profile", Icons.Default.Person, Icons.Outlined.Person),
)

@Composable
private fun PoiApp(repository: EventRepository) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topDestinations.any { it.route == currentRoute }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                PoiBottomBar(navController, currentRoute)
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Discover,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.Discover) {
                DiscoverScreen(repository, onEventClick = { navController.navigate(Routes.event(it)) })
            }
            composable(Routes.Plans) {
                PlansScreen(
                    repository = repository,
                    onEventClick = { navController.navigate(Routes.event(it)) },
                    onDiscover = { navController.navigateTopLevel(Routes.Discover) },
                )
            }
            composable(Routes.Create) {
                CreateEventScreen(
                    repository = repository,
                    onCreated = { eventId -> navController.navigate(Routes.event(eventId)) },
                )
            }
            composable(Routes.Profile) {
                ProfileScreen(
                    repository = repository,
                    onSettings = { navController.navigate(Routes.Settings) },
                    onSafety = { navController.navigate(Routes.Safety) },
                )
            }
            composable(
                route = Routes.Event,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
            ) { entry ->
                EventDetailScreen(
                    eventId = entry.arguments?.getString("eventId").orEmpty(),
                    repository = repository,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.Settings) {
                SettingsScreen(repository, onBack = { navController.popBackStack() })
            }
            composable(Routes.Safety) {
                SafetyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun PoiBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar {
        topDestinations.forEach { destination ->
            val selected = destination.route == currentRoute
            NavigationBarItem(
                selected = selected,
                onClick = { navController.navigateTopLevel(destination.route) },
                icon = {
                    Icon(
                        imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = destination.label,
                    )
                },
                label = { Text(destination.label) },
            )
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
