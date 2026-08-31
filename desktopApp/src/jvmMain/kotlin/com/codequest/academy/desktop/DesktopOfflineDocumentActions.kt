package com.codequest.academy.desktop

import com.codequest.academy.shared.data.NousLibraryCatalog
import com.codequest.academy.shared.data.OfflineLibraryResource
import com.codequest.academy.shared.documents.DocumentActionResult
import com.codequest.academy.shared.documents.OfflineDocumentActions
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Maintains the packaged PDFs in a versioned, app-owned local folder. Every
 * open/save action rechecks the expected SHA-256 and never needs the network.
 */
class DesktopOfflineDocumentActions(private val contentRoot: File) : OfflineDocumentActions {
    fun installBundledResources(): List<DocumentActionResult> = NousLibraryCatalog.resources.map { resource ->
        install(resource)
    }

    override fun availability(resource: OfflineLibraryResource): DocumentActionResult {
        val file = localFile(resource)
        return when {
            !file.isFile -> DocumentActionResult(false, "This offline PDF is missing. Reinstall the verified Nous content package.")
            !hasExpectedChecksum(file, resource.sha256) -> DocumentActionResult(false, "This offline PDF is corrupted and was not opened. Reinstall the verified Nous content package.")
            else -> DocumentActionResult(true, "Ready offline: ${file.name}")
        }
    }

    override fun open(resource: OfflineLibraryResource): DocumentActionResult {
        val available = availability(resource)
        if (!available.success) return available
        if (!Desktop.isDesktopSupported()) return DocumentActionResult(false, "This Windows device cannot open local PDF files from the app.")
        return runCatching {
            Desktop.getDesktop().open(localFile(resource))
            DocumentActionResult(true, "Opened ${resource.title} from local storage.")
        }.getOrElse { DocumentActionResult(false, "Could not open this PDF. Install or choose a default PDF viewer, then try again.") }
    }

    override fun saveCopy(resource: OfflineLibraryResource): DocumentActionResult {
        val available = availability(resource)
        if (!available.success) return available
        val dialog = FileDialog(null as Frame?, "Save ${resource.title}", FileDialog.SAVE).apply {
            file = localFile(resource).name
            isVisible = true
        }
        val directory = dialog.directory ?: return DocumentActionResult(false, "Save cancelled.")
        val selected = dialog.file ?: return DocumentActionResult(false, "Save cancelled.")
        val destination = File(directory, if (selected.endsWith(".pdf", ignoreCase = true)) selected else "$selected.pdf")
        return runCatching {
            Files.copy(localFile(resource).toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            if (!hasExpectedChecksum(destination, resource.sha256)) {
                destination.delete()
                error("The saved copy did not pass its integrity check.")
            }
            DocumentActionResult(true, "Saved a verified copy to ${destination.absolutePath}")
        }.getOrElse { DocumentActionResult(false, "Could not save a complete PDF copy: ${it.message ?: "unknown file error"}") }
    }

    private fun install(resource: OfflineLibraryResource): DocumentActionResult {
        val target = localFile(resource)
        if (target.isFile && hasExpectedChecksum(target, resource.sha256)) return DocumentActionResult(true, "Already installed: ${resource.id}")
        return runCatching {
            target.parentFile?.mkdirs()
            val temporary = File(target.parentFile, "${target.name}.part")
            resourceStream(resource).use { source -> Files.copy(source, temporary.toPath(), StandardCopyOption.REPLACE_EXISTING) }
            check(hasExpectedChecksum(temporary, resource.sha256)) { "Bundled PDF checksum does not match the verified content manifest." }
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            DocumentActionResult(true, "Installed: ${resource.id}")
        }.getOrElse { error -> DocumentActionResult(false, "Could not install ${resource.id}: ${error.message ?: "unknown error"}") }
    }

    private fun localFile(resource: OfflineLibraryResource) = File(contentRoot, resource.resourcePath)

    private fun resourceStream(resource: OfflineLibraryResource): InputStream =
        requireNotNull(javaClass.classLoader.getResourceAsStream(resource.resourcePath)) { "Packaged PDF resource is missing: ${resource.resourcePath}" }

    private fun hasExpectedChecksum(file: File, expected: String): Boolean = runCatching {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        digest.digest().joinToString("") { "%02x".format(it) }.equals(expected, ignoreCase = true)
    }.getOrDefault(false)
}
