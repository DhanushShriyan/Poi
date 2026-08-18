package com.poi.core.model

enum class EventCategory(val label: String, val symbol: String) {
    ALL("All", "✦"),
    FESTIVAL("Festivals", "✺"),
    SALE("Sales", "%"),
    CONCERT("Concerts", "♫"),
    COMMUNITY("Community", "◎"),
    SPORTS("Sports", "◉"),
    WORKSHOP("Workshops", "◇"),
    PRIVATE("Private", "⌂"),
}

enum class EventVisibility(val label: String) {
    PUBLIC("Public"),
    CIRCLE("Selected circle"),
    INVITE_ONLY("Invite only"),
}

enum class VerificationLevel(val label: String) {
    COMMUNITY("Community submitted"),
    CONFIRMED("Community confirmed"),
    ORGANIZER("Organizer verified"),
    OFFICIAL("Official listing"),
}

enum class AttendanceStatus(val label: String) {
    NONE("Not selected"),
    INTERESTED("Interested"),
    GOING("Going"),
    HERE("I'm here"),
    ATTENDED("Attended"),
}

enum class CheckInVisibility(val label: String) {
    PRIVATE("Only me"),
    FRIENDS("My friends"),
    ATTENDEES("People at this event"),
}

enum class ThemeMode(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
}

enum class AuthProvider(val label: String) {
    GOOGLE("Google"),
    EMAIL("Email"),
    PHONE("Phone"),
    ADMIN("Administrator"),
}

enum class UserRole {
    MEMBER,
    ADMIN,
}

data class Organizer(
    val name: String,
    val isVerified: Boolean,
)

data class Event(
    val id: String,
    val title: String,
    val summary: String,
    val description: String,
    val category: EventCategory,
    val startsAtMillis: Long,
    val endsAtMillis: Long,
    val venue: String,
    val address: String,
    val distanceKm: Double,
    val organizer: Organizer,
    val visibility: EventVisibility,
    val verification: VerificationLevel,
    val attendeeCount: Int,
    val friendNames: List<String>,
    val themeKey: String,
    val featured: Boolean = false,
    val createdByCurrentUser: Boolean = false,
    val isCancelled: Boolean = false,
    val updatedAtMillis: Long? = null,
)

data class NewEvent(
    val title: String,
    val summary: String,
    val category: EventCategory,
    val startsAtMillis: Long,
    val endsAtMillis: Long,
    val venue: String,
    val address: String,
    val visibility: EventVisibility,
    val organizerName: String = "Community organizer",
)

data class EventReport(
    val eventId: String,
    val reason: String,
    val reportedAtMillis: Long,
)

data class AuthUser(
    val id: String,
    val displayName: String,
    val email: String? = null,
    val phone: String? = null,
    val provider: AuthProvider,
    val role: UserRole = UserRole.MEMBER,
)

data class AuthSession(
    val user: AuthUser? = null,
) {
    val isAuthenticated: Boolean get() = user != null
    val isAdmin: Boolean get() = user?.role == UserRole.ADMIN
}

data class UserProfile(
    val id: String,
    val displayName: String,
    val handle: String,
    val homeArea: String,
    val attendedCount: Int,
    val hostedCount: Int,
    val contributionPoints: Int,
)

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val defaultCheckInVisibility: CheckInVisibility = CheckInVisibility.FRIENDS,
    val showPlansToFriends: Boolean = true,
    val eventReminders: Boolean = true,
    val weeklyDigest: Boolean = true,
    val friendActivity: Boolean = true,
)

fun Event.isLive(nowMillis: Long): Boolean = nowMillis in startsAtMillis..endsAtMillis
