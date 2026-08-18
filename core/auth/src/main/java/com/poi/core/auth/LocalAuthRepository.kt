package com.poi.core.auth

import android.content.Context
import com.poi.core.model.AuthProvider
import com.poi.core.model.AuthSession
import com.poi.core.model.AuthUser
import com.poi.core.model.UserRole
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Offline identity preview. Member methods validate form shape but do not prove identity.
 * Replace this implementation with a server-backed repository before accepting real users.
 * Admin access uses a high-entropy code hash injected outside source control; production
 * administration must additionally enforce the role on every server request.
 */
class LocalAuthRepository(
    context: Context,
    private val adminPolicy: AdminPolicy,
) : AuthRepository {
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _session = MutableStateFlow(loadSession())
    override val session: StateFlow<AuthSession> = _session.asStateFlow()
    override val usesPreviewIdentity: Boolean = true
    private var pendingPhone: String? = null
    private var pendingCode: String? = null

    override suspend fun signInWithGoogle(email: String, displayName: String): Result<AuthUser> =
        createMember(email, displayName, AuthProvider.GOOGLE)

    override suspend fun signInWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): Result<AuthUser> {
        if (password.length < 6) return Result.failure(IllegalArgumentException("Use at least 6 characters"))
        return createMember(email, displayName, AuthProvider.EMAIL)
    }

    override suspend fun requestPhoneCode(phone: String): Result<PhoneChallenge> = runCatching {
        val normalized = phone.filter { it.isDigit() || it == '+' }
        require(normalized.length >= 10) { "Enter a complete phone number" }
        pendingPhone = normalized
        pendingCode = PREVIEW_PHONE_CODE
        PhoneChallenge(normalized, PREVIEW_PHONE_CODE)
    }

    override suspend fun verifyPhoneCode(phone: String, code: String): Result<AuthUser> = runCatching {
        val normalized = phone.filter { it.isDigit() || it == '+' }
        require(normalized == pendingPhone && code == pendingCode) { "That verification code is not valid" }
        AuthUser(
            id = "phone-${UUID.randomUUID()}",
            displayName = "Poi member",
            phone = normalized,
            provider = AuthProvider.PHONE,
        ).also(::saveSession)
    }

    override suspend fun signInAsAdmin(email: String, accessCode: String): Result<AuthUser> = runCatching {
        require(adminPolicy.isConfigured) { "Administrator access is not configured in this build" }
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        require(normalizedEmail == adminPolicy.email.trim().lowercase(Locale.ROOT)) {
            "Administrator credentials are not valid"
        }
        val suppliedHash = sha256(accessCode)
        require(
            MessageDigest.isEqual(
                suppliedHash.toByteArray(),
                adminPolicy.accessCodeSha256.lowercase(Locale.ROOT).toByteArray(),
            ),
        ) { "Administrator credentials are not valid" }

        AuthUser(
            id = "poi-admin",
            displayName = "Poi Administrator",
            email = normalizedEmail,
            provider = AuthProvider.ADMIN,
            role = UserRole.ADMIN,
        ).also(::saveSession)
    }

    override suspend fun signOut() {
        _session.value = AuthSession()
        preferences.edit().clear().apply()
    }

    private fun createMember(email: String, displayName: String, provider: AuthProvider): Result<AuthUser> =
        runCatching {
            val normalizedEmail = email.trim().lowercase(Locale.ROOT)
            require(EMAIL_PATTERN.matches(normalizedEmail)) { "Enter a valid email address" }
            val name = displayName.trim().ifBlank {
                normalizedEmail.substringBefore('@').replaceFirstChar(Char::uppercase)
            }
            AuthUser(
                id = "member-${UUID.randomUUID()}",
                displayName = name,
                email = normalizedEmail,
                provider = provider,
            ).also(::saveSession)
        }

    private fun saveSession(user: AuthUser) {
        _session.value = AuthSession(user)
        preferences.edit()
            .putString(KEY_ID, user.id)
            .putString(KEY_NAME, user.displayName)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_PHONE, user.phone)
            .putString(KEY_PROVIDER, user.provider.name)
            .putString(KEY_ROLE, user.role.name)
            .apply()
    }

    private fun loadSession(): AuthSession {
        val id = preferences.getString(KEY_ID, null) ?: return AuthSession()
        return runCatching {
            AuthSession(
                AuthUser(
                    id = id,
                    displayName = preferences.getString(KEY_NAME, "Poi member").orEmpty(),
                    email = preferences.getString(KEY_EMAIL, null),
                    phone = preferences.getString(KEY_PHONE, null),
                    provider = AuthProvider.valueOf(preferences.getString(KEY_PROVIDER, "EMAIL").orEmpty()),
                    role = UserRole.valueOf(preferences.getString(KEY_ROLE, "MEMBER").orEmpty()),
                ),
            )
        }.getOrDefault(AuthSession())
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
        const val PREVIEW_PHONE_CODE = "246810"
        const val PREFS_NAME = "poi_auth_session"
        const val KEY_ID = "id"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
        const val KEY_PHONE = "phone"
        const val KEY_PROVIDER = "provider"
        const val KEY_ROLE = "role"
    }
}
