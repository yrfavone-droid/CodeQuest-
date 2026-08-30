package com.codequest.academy.shared.logging

import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AppLogger {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        .build()

    private val logDir: File by lazy {
        val userHome = System.getProperty("user.home")
        File(userHome, ".nous-ai-academy/logs").apply { mkdirs() }
    }

    private val logFile: File
        get() = File(logDir, "app.log")

    private const val MAX_LOG_BYTES = 10 * 1024 * 1024L // 10MB limit

    var apiBaseUrl: String = "http://localhost:3000"

    init {
        rotateLogIfNeeded()
        info("AppLogger initialized. Log location: ${logFile.absolutePath}")
    }

    fun info(message: String) = write("INFO", message)
    fun warn(message: String) = write("WARN", message)
    fun debug(message: String) = write("DEBUG", message)

    fun error(message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) {
            "$message | Exception: ${throwable.message}\n${throwable.stackTraceToString()}"
        } else message

        write("ERROR", fullMsg)
        reportErrorToServer("UNHANDLED_ERROR", message, throwable?.stackTraceToString())
    }

    private fun write(level: String, message: String) {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val line = "[$timestamp] [$level] $message\n"
        println(line.trim())

        scope.launch {
            synchronized(this) {
                try {
                    rotateLogIfNeeded()
                    FileWriter(logFile, true).use { it.write(line) }
                } catch (e: Exception) {
                    System.err.println("Failed to write log: ${e.message}")
                }
            }
        }
    }

    private fun rotateLogIfNeeded() {
        if (logFile.exists() && logFile.length() > MAX_LOG_BYTES) {
            val backup = File(logDir, "app.log.old")
            if (backup.exists()) backup.delete()
            logFile.renameTo(backup)
        }
    }

    fun reportErrorToServer(errorType: String, message: String, stackTrace: String? = null) {
        scope.launch {
            try {
                val json = """
                    {
                      "errorType": "$errorType",
                      "message": "${escapeJson(message)}",
                      "stackTrace": "${escapeJson(stackTrace ?: "")}",
                      "os": "${System.getProperty("os.name")}",
                      "osVersion": "${System.getProperty("os.version")}",
                      "appVersion": "${System.getProperty("nous.version", "1.5.0")}" 
                    }
                """.trimIndent()

                val request = HttpRequest.newBuilder()
                    .uri(URI.create("$apiBaseUrl/api/app/report-error"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .timeout(Duration.ofSeconds(5))
                    .build()

                httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
            } catch (e: Exception) {
                // Silently ignore reporting errors when server is unreachable
            }
        }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun getLogs(): String {
        return if (logFile.exists()) logFile.readText() else "No logs recorded yet."
    }

    fun getLogSizeBytes(): Long {
        return if (logFile.exists()) logFile.length() else 0L
    }
}
