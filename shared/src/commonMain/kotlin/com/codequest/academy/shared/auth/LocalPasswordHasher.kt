package com.codequest.academy.shared.auth

data class PasswordRecord(
    val hash: String,
    val salt: String,
    val algorithm: String,
    val parameters: String
)

expect object LocalPasswordHasher {
    fun create(password: String): PasswordRecord
    fun verify(password: String, record: PasswordRecord): Boolean
}

fun normalizeLocalEmail(email: String): String {
    // Canonicalize the complete address so equality and the SQLite unique
    // index remain case-insensitive even for legacy databases.
    return email.trim().lowercase()
}

fun validateLocalDisplayName(value: String): String? {
    val normalized = value.trim()
    return when {
        normalized.length !in 2..50 -> "Display name must be 2–50 characters."
        normalized.any { it.isISOControl() } -> "Display name contains an invalid character."
        normalized.all { it.isWhitespace() } -> "Display name cannot be blank."
        else -> null
    }
}

fun validateLocalEmail(value: String): String? {
    val normalized = normalizeLocalEmail(value)
    return if (!Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(normalized)) "Enter a valid email address." else null
}

fun validateLocalPassword(value: String): String? = when {
    value.length < 8 -> "Password must be at least 8 characters."
    value.length > 128 -> "Password must be 128 characters or fewer."
    value.none { it.isLetter() } -> "Password must contain a letter."
    value.none { it.isDigit() } -> "Password must contain a number."
    else -> null
}
