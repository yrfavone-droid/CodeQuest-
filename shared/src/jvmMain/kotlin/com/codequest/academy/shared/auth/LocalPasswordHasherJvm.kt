package com.codequest.academy.shared.auth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

actual object LocalPasswordHasher {
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val ALGORITHM = "PBKDF2-HMAC-SHA256"

    actual fun create(password: String): PasswordRecord {
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return PasswordRecord(
            hash = derive(password, salt),
            salt = Base64.getEncoder().encodeToString(salt),
            algorithm = ALGORITHM,
            parameters = "iterations=$ITERATIONS;keyBits=$KEY_BITS"
        )
    }

    actual fun verify(password: String, record: PasswordRecord): Boolean = runCatching {
        require(record.algorithm == ALGORITHM)
        val salt = Base64.getDecoder().decode(record.salt)
        val expected = Base64.getDecoder().decode(record.hash)
        MessageDigest.isEqual(expected, Base64.getDecoder().decode(derive(password, salt)))
    }.getOrDefault(false)

    private fun derive(password: String, salt: ByteArray): String {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_BITS)
        return try {
            val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
            Base64.getEncoder().encodeToString(bytes)
        } finally {
            spec.clearPassword()
        }
    }
}
