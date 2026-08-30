package com.codequest.academy.shared.update

import com.codequest.academy.shared.network.ApiClient
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class AutoUpdateManagerTest {
    @Test
    fun `newer server release makes update available state visible to the app`() = runBlocking {
        val body = """{
            "updateAvailable": true,
            "currentVersion": "1.5.0",
            "latestVersion": "1.5.1",
            "downloadUrl": "https://example.test/nous-ai-academy-setup.exe",
            "fileName": "nous-ai-academy-setup.exe",
            "releaseNotes": "Quality improvements"
        }""".trimIndent()
        val server = HttpServer.create(InetSocketAddress(0), 0).apply {
            createContext("/api/app/check-updates") { exchange ->
                val bytes = body.encodeToByteArray()
                exchange.responseHeaders.add("Content-Type", "application/json")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            }
            start()
        }
        val previousBaseUrl = ApiClient.baseUrl
        try {
            ApiClient.baseUrl = "http://127.0.0.1:${server.address.port}"
            val update = AutoUpdateManager.checkForUpdates(manual = true)
            assertEquals("1.5.1", update?.latestVersion)
            val state = assertIs<UpdateState.UpdateAvailable>(AutoUpdateManager.updateState.value)
            assertEquals("https://example.test/nous-ai-academy-setup.exe", state.info.downloadUrl)
        } finally {
            ApiClient.baseUrl = previousBaseUrl
            AutoUpdateManager.dismissUpdate()
            server.stop(0)
        }
    }
}
