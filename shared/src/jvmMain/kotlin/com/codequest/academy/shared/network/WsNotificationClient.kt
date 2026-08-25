package com.codequest.academy.shared.network

import com.codequest.academy.shared.logging.AppLogger
import com.codequest.academy.shared.update.AutoUpdateManager
import java.net.URI
import java.net.http.HttpClient
import java.net.http.WebSocket
import java.time.Duration
import java.util.concurrent.CompletionStage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed class ServerNotification {
    data class UpdateAvailablePush(val version: String, val releaseNotes: String, val downloadUrl: String) : ServerNotification()
    data class MessagePush(val text: String) : ServerNotification()
}

object WsNotificationClient {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var webSocket: WebSocket? = null
    /** Push updates are optional and only enabled for a configured, authenticated deployment. */
    var serverWsUrl: String = System.getProperty("codequest.wsUrl", "")

    private val _notifications = MutableSharedFlow<ServerNotification>()
    val notifications: SharedFlow<ServerNotification> = _notifications.asSharedFlow()

    fun connect() {
        if (serverWsUrl.isBlank()) {
            AppLogger.debug("WebSocket notifications are not configured; using manual update checks.")
            return
        }
        scope.launch {
            try {
                AppLogger.info("Connecting to WebSocket notification service at $serverWsUrl...")
                val client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()

                client.newWebSocketBuilder()
                    .buildAsync(URI.create(serverWsUrl), Listener())
                    .thenAccept { ws ->
                        webSocket = ws
                        AppLogger.info("Connected to WebSocket notification stream!")
                    }
                    .exceptionally { throwable ->
                        AppLogger.warn("WebSocket connection failed: ${throwable.message}")
                        null
                    }
            } catch (e: Exception) {
                AppLogger.warn("Failed to connect WebSocket: ${e.message}")
            }
        }
    }

    private class Listener : WebSocket.Listener {
        override fun onOpen(webSocket: WebSocket) {
            webSocket.request(1)
        }

        override fun onText(webSocket: WebSocket, data: CharSequence, last: Boolean): CompletionStage<*>? {
            val text = data.toString()
            AppLogger.info("[WS Push Received] $text")

            scope.launch {
                try {
                    if (text.contains("UPDATE_AVAILABLE")) {
                        AutoUpdateManager.checkForUpdates(manual = false)
                    }
                } catch (e: Exception) {
                    AppLogger.warn("Error processing WS frame: ${e.message}")
                }
            }

            webSocket.request(1)
            return null
        }

        override fun onError(webSocket: WebSocket, error: Throwable) {
            AppLogger.warn("WebSocket error: ${error.message}")
        }
    }
}
