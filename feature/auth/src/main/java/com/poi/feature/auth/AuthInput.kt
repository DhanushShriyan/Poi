package com.poi.feature.auth

internal fun validateEmailAccountInput(
    displayName: String,
    email: String,
    password: String,
    creatingAccount: Boolean,
): String? {
    if (creatingAccount && displayName.trim().isBlank()) return "Enter your name."
    if (!EMAIL_PATTERN.matches(email.trim())) return "Enter a valid email address."
    if (password.length < 8 || password.none(Char::isLetter) || password.none(Char::isDigit)) {
        return "Use at least 8 characters with letters and numbers."
    }
    return null
}

internal fun friendlyAuthError(error: Throwable): String {
    val message = error.message.orEmpty()
    val normalized = message.lowercase()
    return when {
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
