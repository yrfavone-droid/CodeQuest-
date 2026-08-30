package com.codequest.academy.shared.network

import com.codequest.academy.shared.logging.AppLogger
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class UpdateCheckResponse(
    val updateAvailable: Boolean = false,
    val currentVersion: String = "1.0.0",
    val latestVersion: String = "1.0.0",
    val downloadUrl: String = "",
    val fileName: String = "",
    val sha256: String = "",
    val sizeBytes: Long = 0,
    val releaseNotes: String = "",
    val isRequired: Boolean = false,
    val minimumVersion: String = "1.0.0"
)

@Serializable
data class FeatureFlagsResponse(
    val newMathModule: Boolean = true,
    val betaEditor: Boolean = false,
    val darkThemeV2: Boolean = true,
    val realTimeNotifications: Boolean = true,
    val offlineAutoSync: Boolean = true,
    val systemTrayEnabled: Boolean = true
)

@Serializable
private data class UpdateStatusRequest(
    val userId: String,
    val currentVersion: String,
    val targetVersion: String,
    val status: String,
    val errorMessage: String? = null
)

object ApiClient {
    /** Production update checks target the deployed release API; IDE runs can override this property. */
    var baseUrl: String = System.getProperty("nous.apiBaseUrl", "https://nous-ai-academy.vercel.app")
        set(value) {
            field = value
            AppLogger.apiBaseUrl = value
        }

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(6))
        .build()

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun checkUpdates(currentVersion: String, os: String = "windows"): Result<UpdateCheckResponse> = withContext(Dispatchers.IO) {
        try {
            val uri = URI.create("$baseUrl/api/app/check-updates?version=$currentVersion&os=$os")
            val request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .timeout(Duration.ofSeconds(8))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val parsed = jsonParser.decodeFromString<UpdateCheckResponse>(response.body())
                Result.success(parsed)
            } else {
                Result.failure(Exception("HTTP status ${response.statusCode()}"))
            }
        } catch (e: Exception) {
            AppLogger.warn("Failed to check updates from server: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun fetchFeatureFlags(): Result<FeatureFlagsResponse> = withContext(Dispatchers.IO) {
        try {
            val uri = URI.create("$baseUrl/api/app/feature-flags")
            val request = HttpRequest.newBuilder()
                .uri(uri)
                .GET()
                .timeout(Duration.ofSeconds(5))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() == 200) {
                val flags = jsonParser.decodeFromString<FeatureFlagsResponse>(response.body())
                Result.success(flags)
            } else {
                Result.success(FeatureFlagsResponse()) // Default flags fallback
            }
        } catch (e: Exception) {
            Result.success(FeatureFlagsResponse())
        }
    }

    suspend fun logUpdateStatus(
        userId: String,
        currentVersion: String,
        targetVersion: String,
        status: String,
        errorMessage: String? = null
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val bodyJson = jsonParser.encodeToString(
                UpdateStatusRequest(userId, currentVersion, targetVersion, status, errorMessage)
            )

            val request = HttpRequest.newBuilder()
                .uri(URI.create("$baseUrl/api/app/update-status"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .timeout(Duration.ofSeconds(5))
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.discarding())
            response.statusCode() == 200
        } catch (e: Exception) {
            false
        }
    }
}
