package com.codequest.academy.shared.network

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

class ApiClientSecurityTest {
    @Test
    fun `update checks reject untrusted release hosts before network access`() = runBlocking {
        val previous = ApiClient.baseUrl
        try {
            ApiClient.baseUrl = "https://updates.example.invalid"
            val result = ApiClient.checkUpdates("1.5.0")
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("trusted", ignoreCase = true))
        } finally {
            ApiClient.baseUrl = previous
        }
    }
}
