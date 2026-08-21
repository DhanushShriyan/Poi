package com.poi.core.auth

import android.content.Intent
import com.poi.core.model.AuthSession
import com.poi.core.model.AuthUser
import kotlinx.coroutines.flow.StateFlow

data class AdminPolicy(
    val email: String,
    val accessCodeSha256: String,
) {
    val isConfigured: Boolean = email.isNotBlank() && accessCodeSha256.length == 64
}

data class PhoneChallenge(
    val normalizedPhone: String,
    val previewCode: String?,
)

data class EmailAccountResult(
    val user: AuthUser?,
    val requiresEmailConfirmation: Boolean,
)

interface AuthRepository {
    val session: StateFlow<AuthSession>
    val usesPreviewIdentity: Boolean
    val supportsGoogleSignIn: Boolean
    val supportsPhoneSignIn: Boolean

    fun handleAuthCallback(intent: Intent)
    suspend fun signInWithGoogle(): Result<Unit>
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun createAccountWithEmail(
        email: String,
        password: String,
        displayName: String,
    ): Result<EmailAccountResult>
    suspend fun requestPhoneCode(phone: String): Result<PhoneChallenge>
    suspend fun verifyPhoneCode(phone: String, code: String): Result<AuthUser>
    suspend fun signInAsAdmin(email: String, accessCode: String): Result<AuthUser>
    suspend fun signOut()
}
