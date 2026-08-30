package com.codequest.academy.desktop

import com.codequest.academy.shared.data.AcademyDocumentActionResult
import com.codequest.academy.shared.data.AcademyDocumentHandler
import com.codequest.academy.shared.data.AcademyLibraryItem
import java.awt.Desktop
import java.io.File
import java.nio.file.Files

/** Copies a bundled PDF to the learner's computer before opening or saving it. */
class JvmAcademyDocumentHandler(
    private val libraryDirectory: File = File(System.getProperty("user.home"), ".codequest-academy/library"),
    private val downloadsDirectory: File = File(System.getProperty("user.home"), "Downloads"),
    private val loader: ClassLoader = JvmAcademyDocumentHandler::class.java.classLoader
) : AcademyDocumentHandler {

    override fun openPdf(item: AcademyLibraryItem): AcademyDocumentActionResult = runCatching {
        val extracted = extract(item)
        check(Desktop.isDesktopSupported()) {
            "No default PDF reader is available. Use Download PDF instead."
        }
        val desktop = Desktop.getDesktop()
        check(desktop.isSupported(Desktop.Action.OPEN)) { "No default PDF reader is available. Use Download PDF instead." }
        desktop.open(extracted)
        AcademyDocumentActionResult(true, "Opened ${item.title} in your PDF reader.")
    }.getOrElse { AcademyDocumentActionResult(false, it.message ?: "Could not open this PDF.") }

    override fun downloadPdf(item: AcademyLibraryItem): AcademyDocumentActionResult = runCatching {
        val source = extract(item)
        downloadsDirectory.mkdirs()
        check(downloadsDirectory.isDirectory) { "The Downloads folder is not available." }
        val destination = uniqueFile(downloadsDirectory, "CodeQuest-${item.id}-${source.name}")
        Files.copy(source.toPath(), destination.toPath())
        AcademyDocumentActionResult(true, "Saved to ${destination.absolutePath}")
    }.getOrElse { AcademyDocumentActionResult(false, it.message ?: "Could not save this PDF.") }

    private fun extract(item: AcademyLibraryItem): File {
        val path = item.sourcePath
        require(path.startsWith("academy/") && path.endsWith(".pdf") && !path.contains("..")) { "Invalid bundled PDF path." }
        libraryDirectory.mkdirs()
        check(libraryDirectory.isDirectory) { "The local document library is not available." }
        val fileName = "${item.id}-${File(path).name}"
        val destination = File(libraryDirectory, fileName)
        val resource = loader.getResourceAsStream("curriculum/$path")
            ?: error("This PDF is not included in the installed application.")
        resource.use { input ->
            if (!destination.isFile) {
                val temporary = File(libraryDirectory, "$fileName.part")
                temporary.outputStream().use { output -> input.copyTo(output) }
                check(temporary.renameTo(destination)) { "Could not prepare the local PDF." }
            }
        }
        return destination
    }

    private fun uniqueFile(directory: File, name: String): File {
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var candidate = File(directory, name)
        var index = 1
        while (candidate.exists()) {
            candidate = File(directory, "$stem ($index)$extension")
            index += 1
        }
        return candidate
    }
}
