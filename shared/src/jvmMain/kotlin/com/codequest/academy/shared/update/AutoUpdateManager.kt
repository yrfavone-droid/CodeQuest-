package com.codequest.academy.shared.update

import com.codequest.academy.shared.logging.AppLogger
import com.codequest.academy.shared.network.ApiClient
import com.codequest.academy.shared.network.UpdateCheckResponse
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.security.MessageDigest
import java.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateCheckResponse) : UpdateState()
    data class Downloading(
        val info: UpdateCheckResponse,
        val progressPercent: Int,
        val bytesDownloaded: Long,
        val totalBytes: Long
    ) : UpdateState()
    data class ReadyToInstall(val info: UpdateCheckResponse, val downloadedFile: File) : UpdateState()
    object UpToDate : UpdateState()
    data class Failed(val info: UpdateCheckResponse?, val error: String) : UpdateState()
}

object AutoUpdateManager {
    val currentVersion: String = "1.0.0"
    var autoUpdateEnabled: Boolean = true
    var checkIntervalMs: Long = 2 * 60 * 60 * 1000L // 2 hours

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var periodicJob: Job? = null

    private val httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.ALWAYS)
        .build()

    private val downloadDir: File by lazy {
        val userHome = System.getProperty("user.home")
        File(userHome, ".codequest-academy/updates").apply { mkdirs() }
    }

    fun startPeriodicChecks() {
        periodicJob?.cancel()
        periodicJob = scope.launch {
            while (true) {
                if (autoUpdateEnabled && _updateState.value !is UpdateState.Downloading) {
                    checkForUpdates(manual = false)
                }
                delay(checkIntervalMs)
            }
        }
    }

    suspend fun checkForUpdates(manual: Boolean = false): UpdateCheckResponse? {
        AppLogger.info("Checking for updates (current version: $currentVersion, manual: $manual)...")
        _updateState.value = UpdateState.Checking

        val osName = System.getProperty("os.name").lowercase()
        val osKey = when {
            osName.contains("win") -> "windows"
            osName.contains("mac") -> "macos"
            else -> "linux"
        }

        val result = ApiClient.checkUpdates(currentVersion, osKey)
        return result.fold(
            onSuccess = { response ->
                if (response.updateAvailable && isNewerVersion(response.latestVersion, currentVersion)) {
                    AppLogger.info("Update available: v${response.latestVersion} (${response.downloadUrl})")
                    _updateState.value = UpdateState.UpdateAvailable(response)
                    ApiClient.logUpdateStatus("local_user", currentVersion, response.latestVersion, "available")

                    // Automatically start downloading if required update or auto-update enabled
                    if (autoUpdateEnabled || response.isRequired) {
                        downloadUpdate(response)
                    }
                    response
                } else {
                    AppLogger.info("CodeQuest Academy is up to date (v$currentVersion).")
                    _updateState.value = UpdateState.UpToDate
                    null
                }
            },
            onFailure = { err ->
                val errorMsg = "Update check failed: ${err.message}"
                AppLogger.warn(errorMsg)
                _updateState.value = UpdateState.Failed(null, errorMsg)
                null
            }
        )
    }

    fun downloadUpdate(info: UpdateCheckResponse) {
        scope.launch {
            try {
                AppLogger.info("Starting background update download: ${info.downloadUrl}")
                ApiClient.logUpdateStatus("local_user", currentVersion, info.latestVersion, "started")

                val targetFile = File(downloadDir, info.fileName.ifBlank { "CodeQuest-Academy-${info.latestVersion}-update.zip" })
                if (targetFile.exists()) targetFile.delete()

                val request = HttpRequest.newBuilder()
                    .uri(URI.create(info.downloadUrl))
                    .GET()
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream())
                if (response.statusCode() != 200) {
                    throw Exception("HTTP download error ${response.statusCode()}")
                }

                val totalBytes = response.headers().firstValueAsLong("Content-Length").orElse(info.sizeBytes.coerceAtLeast(1L))
                var downloadedBytes = 0L

                response.body().use { inputStream ->
                    FileOutputStream(targetFile).use { outputStream ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        var lastReportTime = 0L

                        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                            outputStream.write(buffer, 0, bytesRead)
                            downloadedBytes += bytesRead
                            val progress = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)

                            val now = System.currentTimeMillis()
                            if (now - lastReportTime > 300 || progress == 100) {
                                lastReportTime = now
                                _updateState.value = UpdateState.Downloading(info, progress, downloadedBytes, totalBytes)
                            }
                        }
                    }
                }

                // Verify SHA-256 Checksum if provided
                if (info.sha256.isNotBlank()) {
                    AppLogger.info("Verifying update checksum (SHA-256)...")
                    val calculatedHash = calculateSha256(targetFile)
                    if (!calculatedHash.equals(info.sha256, ignoreCase = true)) {
                        targetFile.delete()
                        val checksumError = "Signature verification failed! Hash mismatch ($calculatedHash vs expected ${info.sha256})"
                        AppLogger.error(checksumError)
                        ApiClient.logUpdateStatus("local_user", currentVersion, info.latestVersion, "failed", checksumError)
                        _updateState.value = UpdateState.Failed(info, checksumError)
                        return@launch
                    }
                    AppLogger.info("SHA-256 Checksum verified successfully!")
                }

                ApiClient.logUpdateStatus("local_user", currentVersion, info.latestVersion, "downloaded")
                AppLogger.info("Update downloaded successfully to ${targetFile.absolutePath}")
                _updateState.value = UpdateState.ReadyToInstall(info, targetFile)

            } catch (e: Exception) {
                val errorMsg = "Update download error: ${e.message}"
                AppLogger.error(errorMsg, e)
                ApiClient.logUpdateStatus("local_user", currentVersion, info.latestVersion, "failed", errorMsg)
                _updateState.value = UpdateState.Failed(info, errorMsg)
            }
        }
    }

    fun applyUpdateAndRestart(downloadedFile: File) {
        scope.launch {
            try {
                AppLogger.info("Applying update from ${downloadedFile.absolutePath} and restarting app...")
                val currentExe = File(System.getProperty("java.class.path"))

                // Create helper script to apply update after process exit
                val scriptFile = File(downloadDir, "apply_update.bat")
                val pid = ProcessHandle.current().pid()

                val scriptContent = """
                    @echo off
                    echo Waiting for CodeQuest Academy process ($pid) to exit...
                    timeout /t 2 /nobreak >nul
                    echo Extracting/Applying update package...
                    copy /Y "${downloadedFile.absolutePath}" "%USERPROFILE%\.codequest-academy\updated_app.zip"
                    echo Update stage complete! Restarting CodeQuest Academy...
                    start "" "${currentExe.absolutePath}"
                    del "%~f0"
                """.trimIndent()

                scriptFile.writeText(scriptContent)
                ApiClient.logUpdateStatus("local_user", currentVersion, "1.2.0", "installed")

                // Launch external script detached
                ProcessBuilder("cmd.exe", "/c", scriptFile.absolutePath)
                    .directory(downloadDir)
                    .start()

                delay(500)
                System.exit(0)
            } catch (e: Exception) {
                AppLogger.error("Failed to execute update restart script: ${e.message}", e)
            }
        }
    }

    private fun isNewerVersion(latest: String, current: String): Boolean {
        val latestParts = latest.split(".").mapNotNull { it.toIntOrNull() }
        val currentParts = current.split(".").mapNotNull { it.toIntOrNull() }

        for (i in 0 until maxOf(latestParts.size, currentParts.size)) {
            val l = latestParts.getOrElse(i) { 0 }
            val c = currentParts.getOrElse(i) { 0 }
            if (l > c) return true
            if (l < c) return false
        }
        return false
    }

    private fun calculateSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { stream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
