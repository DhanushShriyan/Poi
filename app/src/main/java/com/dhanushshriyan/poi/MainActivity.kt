package com.dhanushshriyan.poi

import android.content.Intent
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.poi.core.auth.AuthRepository
import com.poi.core.data.EventRepository
import com.poi.core.designsystem.PoiTheme
import com.poi.core.model.ThemeMode
import com.poi.feature.admin.AdminDashboardScreen
import com.poi.feature.admin.AdminEventEditorScreen
import com.poi.feature.auth.AdminAccessScreen
import com.poi.feature.auth.GuestGateScreen
import com.poi.feature.auth.GuestProfileScreen
import com.poi.feature.auth.SignInScreen
import com.poi.feature.create.CreateEventScreen
import com.poi.feature.discover.DiscoverScreen
import com.poi.feature.discover.EventDetailScreen
import com.poi.feature.plans.PlansScreen
import com.poi.feature.profile.ProfileScreen
import com.poi.feature.profile.SafetyScreen
import com.poi.feature.profile.SettingsScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as PoiApplication
        application.authRepository.handleAuthCallback(intent)
        setContent {
            val settings by application.eventRepository.settings.collectAsStateWithLifecycle()
            PoiTheme(darkTheme = settings.themeMode == ThemeMode.DARK) {
                PoiApp(application.eventRepository, application.authRepository)
                PoiUpdatePrompt()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        (application as PoiApplication).authRepository.handleAuthCallback(intent)
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
    const val SignIn = "sign-in"
    const val AdminAccess = "restricted-account-access"
    const val Admin = "admin"
    const val AdminEdit = "admin/event/{eventId}"

    fun event(eventId: String) = "event/$eventId"
    fun adminEdit(eventId: String) = "admin/event/$eventId"
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
@OptIn(ExperimentalMaterial3Api::class)
private fun PoiApp(repository: EventRepository, authRepository: AuthRepository) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = topDestinations.any { it.route == currentRoute }
    val session by authRepository.session.collectAsStateWithLifecycle()
    val settings by repository.settings.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val signIn = { navController.navigate(Routes.SignIn) }
    val setDarkMode: (Boolean) -> Unit = { dark ->
        scope.launch {
            repository.updateSettings(settings.copy(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT))
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { if (showBottomBar) PoiBottomBar(navController, currentRoute) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.Discover,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.Discover) {
                DiscoverScreen(
                    repository = repository,
                    isGuest = !session.isAuthenticated,
                    displayName = session.user?.displayName,
                    onSignIn = signIn,
                    onEventClick = { navController.navigate(Routes.event(it)) },
                )
            }
            composable(Routes.Plans) {
                if (session.isAuthenticated) {
                    PlansScreen(
                        repository = repository,
                        onEventClick = { navController.navigate(Routes.event(it)) },
                        onDiscover = { navController.navigateTopLevel(Routes.Discover) },
                    )
                } else {
                    GuestGateScreen(
                        title = "Your plans begin here",
                        message = "Sign in to save events, coordinate with friends, and keep a private history of the moments you attended.",
                        onSignIn = signIn,
                    )
                }
            }
            composable(Routes.Create) {
                if (session.isAuthenticated) {
                    CreateEventScreen(
                        repository = repository,
                        organizerName = session.user?.displayName ?: "Poi member",
                        onCreated = { eventId -> navController.navigate(Routes.event(eventId)) },
                    )
                } else {
                    GuestGateScreen(
                        title = "Bring people together",
                        message = "Sign in to publish a community event or privately invite the people you choose.",
                        onSignIn = signIn,
                    )
                }
            }
            composable(Routes.Profile) {
                val user = session.user
                if (user == null) {
                    GuestProfileScreen(
                        darkMode = settings.themeMode == ThemeMode.DARK,
                        onThemeChange = setDarkMode,
                        onSignIn = signIn,
                        onAdminAccess = { navController.navigate(Routes.AdminAccess) },
                    )
                } else {
                    ProfileScreen(
                        repository = repository,
                        authUser = user,
                        darkMode = settings.themeMode == ThemeMode.DARK,
                        versionName = BuildConfig.VERSION_NAME,
                        onThemeChange = setDarkMode,
                        onAdmin = { navController.navigate(Routes.Admin) },
                        onSignOut = {
                            scope.launch {
                                authRepository.signOut()
                                navController.navigateTopLevel(Routes.Profile)
                            }
                        },
                        onSettings = { navController.navigate(Routes.Settings) },
                        onSafety = { navController.navigate(Routes.Safety) },
                    )
                }
            }
            composable(
                route = Routes.Event,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
            ) { entry ->
                EventDetailScreen(
                    eventId = entry.arguments?.getString("eventId").orEmpty(),
                    repository = repository,
                    isAuthenticated = session.isAuthenticated,
                    onSignIn = signIn,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.SignIn) {
                SignInScreen(
                    authRepository = authRepository,
                    onBack = { navController.popBackStack() },
                    onSignedIn = { navController.popBackStack() },
                )
            }
            composable(Routes.AdminAccess) {
                AdminAccessScreen(
                    authRepository = authRepository,
                    onBack = { navController.popBackStack() },
                    onAuthenticated = {
                        navController.navigate(Routes.Admin) {
                            popUpTo(Routes.AdminAccess) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.Admin) {
                if (session.isAdmin) {
                    AdminDashboardScreen(
                        repository = repository,
                        onBack = { navController.navigateTopLevel(Routes.Profile) },
                        onCreateEvent = { navController.navigateTopLevel(Routes.Create) },
                        onEditEvent = { navController.navigate(Routes.adminEdit(it)) },
                    )
                } else {
                    GuestGateScreen(
                        title = "Restricted area",
                        message = "This console is available only to the configured Poi administrator.",
                        onSignIn = { navController.navigateTopLevel(Routes.Profile) },
                    )
                }
            }
            composable(
                route = Routes.AdminEdit,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType }),
            ) { entry ->
                if (session.isAdmin) {
                    AdminEventEditorScreen(
                        eventId = entry.arguments?.getString("eventId").orEmpty(),
                        repository = repository,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                } else {
                    GuestGateScreen(
                        title = "Restricted area",
                        message = "Administrator privileges are required to edit this listing.",
                        onSignIn = { navController.navigateTopLevel(Routes.Profile) },
                    )
                }
            }
            composable(Routes.Settings) {
                if (session.isAuthenticated) SettingsScreen(repository, onBack = { navController.popBackStack() })
            }
            composable(Routes.Safety) {
                SafetyScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun PoiBottomBar(navController: NavHostController, currentRoute: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
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
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
