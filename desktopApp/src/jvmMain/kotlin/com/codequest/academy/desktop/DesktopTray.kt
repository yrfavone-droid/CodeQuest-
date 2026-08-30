package com.codequest.academy.desktop

import com.codequest.academy.shared.logging.AppLogger
import com.codequest.academy.shared.update.AutoUpdateManager
import com.codequest.academy.shared.update.UpdateState
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

object DesktopTray {
    private var trayIcon: TrayIcon? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun initialize(onOpenWindow: () -> Unit, onExit: () -> Unit) {
        if (!SystemTray.isSupported()) {
            AppLogger.warn("System tray is not supported on this platform.")
            return
        }

        try {
            val systemTray = SystemTray.getSystemTray()
            val popup = PopupMenu()

            val openItem = MenuItem("Open Nous AI Academy").apply {
                addActionListener { onOpenWindow() }
            }

            val checkUpdatesItem = MenuItem("Check for Updates").apply {
                addActionListener {
                    scope.launch(Dispatchers.IO) {
                        AutoUpdateManager.checkForUpdates(manual = true)
                    }
                }
            }

            val exitItem = MenuItem("Exit").apply {
                addActionListener { onExit() }
            }

            popup.add(openItem)
            popup.add(checkUpdatesItem)
            popup.addSeparator()
            popup.add(exitItem)

            val image = createTrayIconImage()
            trayIcon = TrayIcon(image, "Nous AI Academy", popup).apply {
                isImageAutoSize = true
                addActionListener { onOpenWindow() }
            }

            systemTray.add(trayIcon)
            AppLogger.info("System Tray initialized successfully with brand logo.")

            // Listen to update state and show tray notifications
            scope.launch {
                AutoUpdateManager.updateState.collectLatest { state ->
                    when (state) {
                        is UpdateState.UpdateAvailable -> {
                            showNotification(
                                "New Version Available",
                                "Nous AI Academy v${state.info.latestVersion} is available. Download the installer to update."
                            )
                        }
                        else -> {}
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.warn("Failed to set up System Tray: ${e.message}")
        }
    }

    fun showNotification(title: String, message: String) {
        trayIcon?.displayMessage(title, message, TrayIcon.MessageType.INFO)
    }

    private fun createTrayIconImage(): Image {
        return try {
            val stream = DesktopTray::class.java.classLoader.getResourceAsStream("branding/codequest-academy-logo.png")
            if (stream != null) {
                ImageIO.read(stream)
            } else {
                createFallbackTrayIcon()
            }
        } catch (e: Exception) {
            createFallbackTrayIcon()
        }
    }

    private fun createFallbackTrayIcon(): Image {
        val size = 16
        val image = BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = java.awt.Color(124, 92, 255) // Brand purple #7C5CFF
        g.fillRoundRect(0, 0, size, size, 4, 4)
        g.color = java.awt.Color.WHITE
        g.font = java.awt.Font("SansSerif", java.awt.Font.BOLD, 10)
        g.drawString("Σ", 4, 12)
        g.dispose()
        return image
    }
}
