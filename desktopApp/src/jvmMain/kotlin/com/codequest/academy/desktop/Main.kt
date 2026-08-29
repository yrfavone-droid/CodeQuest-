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

fun main() = application {
    LaunchedEffect(Unit) {
        AppLogger.info("Starting CodeQuest Academy Desktop Application (v${AutoUpdateManager.currentVersion})...")
        // The Academy is fully usable offline. Update checks remain opt-in for
        // a future release channel and never gate local content or progress.
        if (System.getProperty("codequest.enableOnlineUpdateChecks", "false").toBoolean()) {
            AutoUpdateManager.startPeriodicChecks()
            WsNotificationClient.connect()
        } else {
            AppLogger.info("Online update checks are disabled; using local-first mode.")
        }
    }

    val fileReader = remember { JvmCurriculumFileReader() }
    val repository = remember {
        val databaseFile = localDatabaseFile()
        databaseFile.parentFile?.mkdirs()
        val isNewDatabase = !databaseFile.exists()
        val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath.replace("\\", "/")}")
        if (isNewDatabase) {
            AppDatabase.Schema.create(driver)
            AppLogger.info("Created CodeQuest database at ${databaseFile.absolutePath}")
        } else {
            AppLogger.info("Opening existing CodeQuest database at ${databaseFile.absolutePath}")
        }
        ProgressRepository(driver)
    }
    val windowState = rememberWindowState(width = 1440.dp, height = 900.dp)

    // Initialize System Tray with Brand Icon
    remember {
        DesktopTray.initialize(
            onOpenWindow = {
                // Focus window
            },
            onExit = {
                AppLogger.info("Exiting CodeQuest Academy via System Tray.")
                exitApplication()
            }
        )
    }

    val brandIcon = painterResource("branding/codequest-academy-logo.png")

    Window(
        onCloseRequest = ::exitApplication,
        title = "CodeQuest Academy v${AutoUpdateManager.currentVersion}",
        state = windowState,
        icon = brandIcon
    ) {
        LaunchedEffect(Unit) { window.minimumSize = Dimension(960, 640) }
        Box {
            App(fileReader, repository)
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
    val appData = File(System.getProperty("user.home"), ".codequest-academy/codequest_progress.db")
    val legacy = File("codequest_progress.db").absoluteFile
    return if (!appData.exists() && legacy.exists()) legacy else appData
}
