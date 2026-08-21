package com.poi.core.auth

import android.content.Intent
import com.poi.core.cloud.PoiCloudClient
import com.poi.core.model.AuthProvider
import com.poi.core.model.AuthSession
import com.poi.core.model.AuthUser
import com.poi.core.model.UserRole
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val cloud: PoiCloudClient,
    override val supportsPhoneSignIn: Boolean,
) : AuthRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _session = MutableStateFlow(AuthSession())
    override val session: StateFlow<AuthSession> = _session.asStateFlow()
    override val usesPreviewIdentity: Boolean = false

    init {
        scope.launch {
            cloud.supabase.auth.sessionStatus.collect {
                val user = cloud.supabase.auth.currentUserOrNull()
                _session.value = if (user == null) {
                    AuthSession()
                } else {
                    AuthSession(user.toPoiUser(loadRole(user.id)))
                }
            }
        }
    }

    override fun handleAuthCallback(intent: Intent) {
        cloud.handleAuthCallback(intent)
    }

    override suspend fun signInWithGoogle(): Result<Unit> = runCatching {
        cloud.supabase.auth.signInWith(Google)
    }

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> = runCatching {
        cloud.supabase.auth.signInWith(Email) {
            this.email = email.trim().lowercase(Locale.ROOT)
            this.password = password
        }
        val user = checkNotNull(cloud.supabase.auth.currentUserOrNull()) {
            "The account could not be loaded after sign-in."
        }
        user.toPoiUser(loadRole(user.id)).also { _session.value = AuthSession(it) }
    }

    override suspend fun createAccountWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): Result<EmailAccountResult> = runCatching {
        val created = cloud.supabase.auth.signUpWith(Email) {
            this.email = email.trim().lowercase(Locale.ROOT)
            this.password = password
            data = buildJsonObject {
                put("display_name", displayName.trim().ifBlank { "Poi member" })
            }
        }
        val signedIn = cloud.supabase.auth.currentUserOrNull()
        val poiUser = signedIn?.toPoiUser(loadRole(signedIn.id))
        if (poiUser != null) _session.value = AuthSession(poiUser)
        EmailAccountResult(
            user = poiUser ?: created?.toPoiUser(UserRole.MEMBER),
            requiresEmailConfirmation = signedIn == null,
        )
    }

    override suspend fun requestPhoneCode(phone: String): Result<PhoneChallenge> = runCatching {
        check(supportsPhoneSignIn) { "Phone verification is not enabled yet." }
        val normalized = normalizePhone(phone)
        cloud.supabase.auth.signInWith(OTP) {
            this.phone = normalized
        }
        PhoneChallenge(normalizedPhone = normalized, previewCode = null)
    }

    override suspend fun verifyPhoneCode(phone: String, code: String): Result<AuthUser> = runCatching {
        check(supportsPhoneSignIn) { "Phone verification is not enabled yet." }
        val normalized = normalizePhone(phone)
        cloud.supabase.auth.verifyPhoneOtp(
            type = io.github.jan.supabase.auth.OtpType.Phone.SMS,
            phone = normalized,
            token = code,
        )
        val user = checkNotNull(cloud.supabase.auth.currentUserOrNull()) {
            "The phone account could not be loaded after verification."
        }
        user.toPoiUser(loadRole(user.id)).also { _session.value = AuthSession(it) }
    }

    override suspend fun signInAsAdmin(email: String, accessCode: String): Result<AuthUser> = runCatching {
        cloud.supabase.auth.signInWith(Email) {
            this.email = email.trim().lowercase(Locale.ROOT)
            password = accessCode
        }
        val user = checkNotNull(cloud.supabase.auth.currentUserOrNull()) {
            "Administrator credentials are not valid."
        }
        check(loadRole(user.id) == UserRole.ADMIN) {
            cloud.supabase.auth.signOut()
            "This account does not have administrator privileges."
        }
        user.toPoiUser(UserRole.ADMIN).also { _session.value = AuthSession(it) }
    }

    override suspend fun signOut() {
        cloud.supabase.auth.signOut()
        _session.value = AuthSession()
    }

    private suspend fun loadRole(userId: String): UserRole = runCatching {
        cloud.supabase.from("profiles")
            .select(columns = Columns.list("role")) {
                filter { eq("id", userId) }
            }
            .decodeSingle<ProfileRoleRow>()
            .role
            .let { role ->
                if (role.equals("admin", ignoreCase = true)) UserRole.ADMIN else UserRole.MEMBER
            }
    }.getOrDefault(UserRole.MEMBER)

    private fun UserInfo.toPoiUser(role: UserRole): AuthUser {
        val providerName = appMetadata?.get("provider")?.jsonPrimitive?.contentOrNull
        val provider = when {
            role == UserRole.ADMIN -> AuthProvider.ADMIN
            providerName.equals("google", ignoreCase = true) -> AuthProvider.GOOGLE
            !phone.isNullOrBlank() && email.isNullOrBlank() -> AuthProvider.PHONE
            else -> AuthProvider.EMAIL
        }
        val displayName = userMetadata?.get("display_name")?.jsonPrimitive?.contentOrNull
            ?: userMetadata?.get("full_name")?.jsonPrimitive?.contentOrNull
            ?: email?.substringBefore('@')
            ?: "Poi member"
        return AuthUser(
            id = id,
            displayName = displayName,
            email = email,
            phone = phone,
            provider = provider,
            role = role,
        )
    }

    private fun normalizePhone(phone: String): String {
        val normalized = phone.filter { it.isDigit() || it == '+' }
        require(normalized.startsWith('+') && normalized.length in 11..16) {
            "Enter a complete number with country code, for example +91."
        }
        return normalized
    }
}

@Serializable
private data class ProfileRoleRow(
    @SerialName("role") val role: String,
)
