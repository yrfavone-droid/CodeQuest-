package com.codequest.academy.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import java.awt.Dimension
import java.io.File
import com.codequest.academy.shared.App

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.codequest.academy.database.AppDatabase
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.logging.AppLogger
import com.codequest.academy.shared.network.WsNotificationClient
import com.codequest.academy.shared.update.AutoUpdateManager
import com.codequest.academy.shared.data.NousLibraryCatalog
import com.codequest.academy.shared.learning.LearningHubContent
import com.codequest.academy.shared.learning.LearningHubProgress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun main() = application {
    LaunchedEffect(Unit) {
        AppLogger.info("Starting Nous AI Academy Desktop Application (v${AutoUpdateManager.currentVersion})...")
        // The Academy is fully usable offline. Update checks remain opt-in for
        // a future release channel and never gate local content or progress.
        if (System.getProperty("nous.enableOnlineUpdateChecks", "false").toBoolean()) {
            AutoUpdateManager.startPeriodicChecks()
            WsNotificationClient.connect()
        } else {
            AppLogger.info("Online update checks are disabled; using local-first mode.")
        }
    }

    val repository = remember {
        val databaseFile = localDatabaseFile()
        databaseFile.parentFile?.mkdirs()
        val isNewDatabase = !databaseFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath.replace("\\", "/")}")
        if (isNewDatabase) {
            AppDatabase.Schema.create(driver)
            AppLogger.info("Created Nous AI Academy database at ${databaseFile.absolutePath}")
        } else {
            AppLogger.info("Opening existing Nous AI Academy database at ${databaseFile.absolutePath}")
        }
        ProgressRepository(driver)
    }
    LaunchedEffect(repository) {
        val databasePath = localDatabaseFile().absolutePath
        // Package validation is intentionally off the UI thread. The window can
        // render its loading state immediately while the offline package is
        // staged, verified, and activated without touching learner-owned data.
        withContext(Dispatchers.IO) {
            runCatching { LearningHubContent.initialize(databasePath) }
                .onFailure { AppLogger.error("Learning Hub initialization failed without affecting learner data: ${it.message}") }
            LearningHubContent.state.value.error?.let { AppLogger.error("Learning Hub package was rejected: $it") }
            runCatching { LearningHubProgress.initialize(databasePath) }
                .onFailure { AppLogger.error("Learning progress initialization failed: ${it.message}") }
        }
    }
    val offlineDocuments = remember {
        val actions = DesktopOfflineDocumentActions(localContentDirectory())
        val installation = actions.installBundledResources()
        installation.filterNot { it.success }.forEach { AppLogger.error("Offline library install failed: ${it.message}") }
        repository.installVerifiedLibrary(NousLibraryCatalog.resources)
        actions
    }
    val windowState = rememberWindowState(width = 1440.dp, height = 900.dp)

    // Initialize System Tray with Brand Icon
    remember {
        DesktopTray.initialize(
            onOpenWindow = {
                // Focus window
            },
            onExit = {
                AppLogger.info("Exiting Nous AI Academy via System Tray.")
                exitApplication()
            }
        )
    }

    val brandIcon = painterResource("branding/nous-ai-academy-logo.png")

    Window(
        onCloseRequest = ::exitApplication,
        title = "Nous AI Academy v${AutoUpdateManager.currentVersion}",
        state = windowState,
        icon = brandIcon
    ) {
        LaunchedEffect(Unit) { window.minimumSize = Dimension(960, 640) }
        Box {
            App(repository, offlineDocuments)
            DesktopUpdateBanner()
        }
    }
}

/**
 * Keep the database in the user's profile rather than the process working
 * directory, so packaged launches and desktop shortcuts share one account
 * store. A database beside the executable from older builds is retained when
 * no new profile-store exists yet.
 */
private fun localDatabaseFile(): File {
    val home = File(System.getProperty("user.home"))
    val target = File(home, ".nous-ai-academy/nous_ai_academy.db")
    val legacyProfile = File(home, ".codequest-academy/codequest_progress.db")
    val legacyWorkingDirectory = File("codequest_progress.db").absoluteFile
    if (!target.exists()) {
        val legacy = listOf(legacyProfile, legacyWorkingDirectory).firstOrNull { it.exists() }
        if (legacy != null) {
            target.parentFile?.mkdirs()
            legacy.copyTo(target, overwrite = false)
            AppLogger.info("Copied existing local data into the Nous AI Academy profile store.")
        }
    }
    return target
}

private fun localContentDirectory(): File = File(System.getProperty("user.home"), ".nous-ai-academy/library/${NousLibraryCatalog.packId}")
