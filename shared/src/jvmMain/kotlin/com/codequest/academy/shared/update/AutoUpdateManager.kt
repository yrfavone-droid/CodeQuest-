package com.codequest.academy.shared.update

import com.codequest.academy.shared.logging.AppLogger
import com.codequest.academy.shared.network.ApiClient
import com.codequest.academy.shared.network.UpdateCheckResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.net.URI
import java.net.UnknownHostException
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.Duration
import java.util.Base64

sealed class UpdateState {
    object Idle : UpdateState()
    object Checking : UpdateState()
    data class UpdateAvailable(val info: UpdateCheckResponse) : UpdateState()
    object UpToDate : UpdateState()
    data class Failed(val info: UpdateCheckResponse?, val error: String) : UpdateState()
}

object AutoUpdateManager {
    /** The packaging task injects this value; the fallback keeps IDE runs deterministic. */
    val currentVersion: String = System.getProperty("nous.version", "1.5.0")
    /** Checks run automatically; installation always remains a user-approved action. */
    var updateChecksEnabled: Boolean = true
    var checkIntervalMs: Long = 2 * 60 * 60 * 1000L // 2 hours

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()
    private val _uiState = MutableStateFlow<UpdateUiState>(UpdateUiState.Idle)
    val uiState: StateFlow<UpdateUiState> = _uiState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO)
    private var periodicJob: Job? = null
    private var activeUpdateJob: Job? = null
    private var latestInfo: UpdateCheckResponse? = null

    fun startPeriodicChecks() {
        periodicJob?.cancel()
        if (!updateChecksEnabled) return
        periodicJob = scope.launch {
            checkForUpdates(manual = false)
            while (true) {
                delay(checkIntervalMs)
                if (updateChecksEnabled) {
                    checkForUpdates(manual = false)
                }
            }
        }
    }

    suspend fun checkForUpdates(manual: Boolean = false): UpdateCheckResponse? {
        AppLogger.info("Checking for updates (current version: $currentVersion, manual: $manual)...")
        _updateState.value = UpdateState.Checking
        _uiState.value = UpdateUiState.Checking

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
                    latestInfo = response
                    _uiState.value = UpdateUiState.Available(response.toUiInfo(currentVersion))
                    ApiClient.logUpdateStatus("local_user", currentVersion, response.latestVersion, "available")

                    response
                } else {
                    AppLogger.info("Nous AI Academy is up to date (v$currentVersion).")
                    _updateState.value = UpdateState.UpToDate
                    latestInfo = null
                    _uiState.value = UpdateUiState.UpToDate
                    null
                }
            },
            onFailure = { err ->
                val errorMsg = "Update check failed: ${err.message}"
                AppLogger.warn(errorMsg)
                _updateState.value = UpdateState.Failed(null, errorMsg)
                _uiState.value = if (err is UnknownHostException || err.message?.contains("timed out", true) == true) UpdateUiState.Offline else UpdateUiState.Failed(errorMsg)
                null
            }
        )
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

    fun dismissUpdate() {
        if (_updateState.value is UpdateState.UpdateAvailable) _updateState.value = UpdateState.Idle
        if (_uiState.value is UpdateUiState.Available) _uiState.value = UpdateUiState.Idle
    }

    fun requestCheck() {
        scope.launch { checkForUpdates(manual = true) }
    }

    fun requestInstall() {
        val info = latestInfo ?: return
        activeUpdateJob?.cancel()
        activeUpdateJob = scope.launch { downloadAndPrepare(info) }
    }

    fun cancelUpdate() {
        activeUpdateJob?.cancel()
        activeUpdateJob = null
        latestInfo?.let { _uiState.value = UpdateUiState.Available(it.toUiInfo(currentVersion)) } ?: run { _uiState.value = UpdateUiState.Idle }
    }

    private suspend fun downloadAndPrepare(info: UpdateCheckResponse) {
        try {
            require(verifyManifestSignature(info)) { "This release manifest is not cryptographically verified. Installation is disabled." }
            require(info.platform.equals("windows", true) && info.architecture.equals("x64", true)) { "This update is not compatible with this Windows x64 installation." }
            require(info.sizeBytes in 1..MAX_UPDATE_BYTES) { "The update size is outside the allowed safety limit." }
            val uri = URI.create(info.downloadUrl)
            require(isTrustedUpdateUri(uri)) { "The update URL is not a trusted HTTPS endpoint." }

            _uiState.value = UpdateUiState.Downloading
            val updateDir = File(System.getProperty("user.home"), ".nous-ai-academy/updates/app").apply { mkdirs() }
            val target = File(updateDir, info.fileName.ifBlank { "Nous-AI-Academy-Setup-${info.latestVersion}.exe" })
            val partial = File("${target.absolutePath}.part")
            val client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).followRedirects(HttpClient.Redirect.NORMAL).build()
            val request = HttpRequest.newBuilder(uri).GET().timeout(Duration.ofMinutes(5)).build()
            val response = client.send(request, HttpResponse.BodyHandlers.ofFile(partial.toPath()))
            require(response.statusCode() == 200) { "The update server returned HTTP ${response.statusCode()}." }
            require(isTrustedUpdateUri(response.uri())) { "The download redirected to an untrusted host." }
            require(partial.length() == info.sizeBytes) { "The downloaded update size does not match its manifest." }

            _uiState.value = UpdateUiState.Verifying
            require(sha256(partial) == info.sha256.lowercase()) { "The downloaded update checksum does not match its manifest." }
            Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
            _uiState.value = UpdateUiState.ReadyToRestart
            launchVerifiedInstaller(target)
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            _uiState.value = UpdateUiState.Idle
            throw cancelled
        } catch (error: Exception) {
            _uiState.value = UpdateUiState.Failed(error.message ?: "The update could not be installed.")
            AppLogger.warn("Secure update failed: ${error.message}")
        }
    }

    private fun launchVerifiedInstaller(installer: File) {
        val pid = ProcessHandle.current().pid()
        val runtimeHome = File(System.getProperty("java.home", ""))
        val packagedApp = runtimeHome.parentFile?.resolve("Nous-AI-Academy.exe")
        val appExe = packagedApp?.takeIf { it.isFile }
            ?: File(System.getProperty("user.home"), "AppData/Local/Programs/Nous AI Academy/Nous-AI-Academy.exe")
        val script = File(installer.parentFile, "apply-update.ps1")
        val installerPath = installer.absolutePath.replace("'", "''")
        val appPath = appExe.absolutePath.replace("'", "''")
        val scriptPath = script.absolutePath.replace("'", "''")
        script.writeText("Start-Sleep -Seconds 2\n\$process = Get-Process -Id $pid -ErrorAction SilentlyContinue\nif (\$process) { \$process.CloseMainWindow() | Out-Null; \$process.WaitForExit(10000) | Out-Null }\nif (Get-Process -Id $pid -ErrorAction SilentlyContinue) { Stop-Process -Id $pid -Force }\nStart-Process -FilePath '$installerPath' -ArgumentList '/silent' -Wait\nif (Test-Path '$appPath') { Start-Process -FilePath '$appPath' -WorkingDirectory (Split-Path '$appPath') }\nRemove-Item -LiteralPath '$installerPath' -Force -ErrorAction SilentlyContinue\nRemove-Item -LiteralPath '$scriptPath' -Force -ErrorAction SilentlyContinue\n")
        ProcessBuilder("powershell.exe", "-NoProfile", "-WindowStyle", "Hidden", "-ExecutionPolicy", "Bypass", "-File", script.absolutePath).start()
        _uiState.value = UpdateUiState.Installing
        AppLogger.info("Verified update staged; waiting for application process $pid to exit before installation.")
    }

    private fun isTrustedUpdateUri(uri: URI): Boolean {
        val host = uri.host?.lowercase() ?: return false
        val dev = System.getProperty("nous.allowLocalUpdateEndpoint", "false").toBoolean()
        return uri.scheme.equals("https", true) && (host == TRUSTED_HOST || (dev && host in setOf("localhost", "127.0.0.1")))
    }

    /**
     * Verify the detached release signature with the public key supplied by the
     * packaged application. No key means verification fails closed, which keeps
     * unsigned development/public manifests from ever launching an installer.
     */
    private fun verifyManifestSignature(info: UpdateCheckResponse): Boolean {
        if (info.signature.isBlank() || !info.signatureAlgorithm.equals("SHA256withRSA", true)) return false
        val publicKeyEncoded = System.getProperty("nous.updatePublicKeyBase64", "").trim()
        if (publicKeyEncoded.isBlank()) return false
        return try {
            val keyBytes = Base64.getDecoder().decode(publicKeyEncoded)
            val publicKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(keyBytes))
            val payload = listOf(info.manifestVersion, info.latestVersion, info.platform.lowercase(), info.architecture.lowercase(), info.fileName, info.sha256.lowercase(), info.sizeBytes.toString()).joinToString("|").toByteArray(Charsets.UTF_8)
            Signature.getInstance("SHA256withRSA").apply {
                initVerify(publicKey)
                update(payload)
            }.verify(Base64.getDecoder().decode(info.signature))
        } catch (_: Exception) {
            false
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun UpdateCheckResponse.toUiInfo(current: String) = UpdateUiInfo(current, latestVersion, releaseDate, releaseNotes, updateType, sizeBytes)

    private const val TRUSTED_HOST = "nous-ai-academy.vercel.app"
    private const val MAX_UPDATE_BYTES = 250L * 1024L * 1024L

    fun recordUpdateAction(info: UpdateCheckResponse) {
        scope.launch {
            ApiClient.logUpdateStatus("local_user", currentVersion, info.latestVersion, "download_opened")
        }
    }

}
