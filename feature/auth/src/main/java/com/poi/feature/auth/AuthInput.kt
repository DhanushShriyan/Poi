package com.poi.feature.auth

internal fun validateEmailAccountInput(
    displayName: String,
    email: String,
    password: String,
    creatingAccount: Boolean,
): String? {
    if (creatingAccount && displayName.trim().isBlank()) return "Enter your name."
    if (!EMAIL_PATTERN.matches(email.trim())) return "Enter a valid email address."
    if (!creatingAccount && password.isBlank()) return "Enter your password."
    if (creatingAccount && (password.length < 8 || password.none(Char::isLetter) || password.none(Char::isDigit))) {
        return "Use at least 8 characters with letters and numbers."
    }
    return null
}

internal fun friendlyAuthError(error: Throwable): String {
    val message = error.message.orEmpty()
    val normalized = message.lowercase()
    return when {
        normalized.contains("jwt issued at future") -> "Your new session is not ready yet. Wait a moment and retry."
        normalized.contains("email not confirmed") -> "Confirm your email before signing in."
        normalized.contains("rate limit") || normalized.contains("too many requests") -> "Too many attempts. Wait a few minutes before trying again."
        normalized.contains("provider is not enabled") -> "This sign-in method is not configured. Use email and password for now."
        normalized.contains("unable to resolve host") ||
            normalized.contains("no address associated with hostname") ||
            normalized.contains("failed to connect") ||
            normalized.contains("timeout") ->
            "Poi cannot reach the account service. Check your internet connection and try again."
        normalized.contains("invalid login credentials") || normalized.contains("invalid credentials") ->
            "The email or password is incorrect."
        normalized.contains("user already registered") || normalized.contains("already exists") ->
            "An account already exists for this email. Choose Sign in instead."
        normalized.contains("password") -> "Use at least 8 characters with letters and numbers."
        normalized.contains("email") -> "Check the email address and try again."
        else -> "We couldn't complete account access. Please try again."
    }
}

private val EMAIL_PATTERN = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
