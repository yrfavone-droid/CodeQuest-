package com.codequest.academy.desktop

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.codequest.academy.shared.data.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.text.PDFTextStripper
import org.apache.pdfbox.rendering.ImageType
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.Desktop
import java.io.File
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import javax.imageio.ImageIO
import org.jetbrains.skia.Image

/** Provides offline extraction, download, text search, outline access, and page rendering. */
class JvmAcademyDocumentHandler(
    private val libraryDirectory: File = File(System.getProperty("user.home"), ".nous-ai-academy/library"),
    private val downloadsDirectory: File = File(System.getProperty("user.home"), "Downloads"),
    private val loader: ClassLoader = JvmAcademyDocumentHandler::class.java.classLoader
) : AcademyDocumentHandler, OfflinePdfReader {

    override fun openPdf(item: AcademyLibraryItem): AcademyDocumentActionResult = runCatching {
        val extracted = extract(item)
        check(Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) { "No default PDF reader is available. Use Download instead." }
        Desktop.getDesktop().open(extracted)
        AcademyDocumentActionResult(true, "Opened ${item.title} in your PDF reader.")
    }.getOrElse { AcademyDocumentActionResult(false, it.message ?: "Could not open this PDF.") }

    override fun downloadPdf(item: AcademyLibraryItem): AcademyDocumentActionResult = runCatching {
        val source = extract(item)
        downloadsDirectory.mkdirs()
        check(downloadsDirectory.isDirectory) { "The Downloads folder is not available." }
        val destination = uniqueFile(downloadsDirectory, "Nous-${item.id}-${source.name}")
        Files.copy(source.toPath(), destination.toPath())
        AcademyDocumentActionResult(true, "Saved to ${destination.absolutePath}")
    }.getOrElse { AcademyDocumentActionResult(false, it.message ?: "Could not save this PDF.") }

    override fun pageCount(item: AcademyLibraryItem): Int = runCatching { PDDocument.load(extract(item)).use { it.numberOfPages } }.getOrDefault(item.pageCount)

    override fun renderPage(item: AcademyLibraryItem, page: Int, zoom: Float): ImageBitmap? = runCatching {
        PDDocument.load(extract(item)).use { document ->
            val safePage = (page - 1).coerceIn(0, document.numberOfPages - 1)
            val buffered = PDFRenderer(document).renderImageWithDPI(safePage, 112f * zoom.coerceIn(.7f, 2.5f), ImageType.RGB)
            ByteArrayOutputStream().use { output ->
                check(ImageIO.write(buffered, "png", output)) { "Could not encode the local PDF page." }
                Image.makeFromEncoded(output.toByteArray()).asImageBitmap()
            }
        }
    }.getOrNull()

    override fun tableOfContents(item: AcademyLibraryItem): List<String> = runCatching {
        PDDocument.load(extract(item)).use { document ->
            val outline = document.documentCatalog.documentOutline ?: return@use emptyList()
            buildList {
                var entry = outline.firstChild
                while (entry != null && size < 40) { add(entry.title ?: "Section"); entry = entry.nextSibling }
            }
        }
    }.getOrDefault(emptyList())

    override fun searchPages(item: AcademyLibraryItem, query: String): List<Int> = runCatching {
        if (query.isBlank()) return@runCatching emptyList()
        PDDocument.load(extract(item)).use { document ->
            val stripper = PDFTextStripper()
            (1..document.numberOfPages).filter { page ->
                stripper.startPage = page; stripper.endPage = page
                stripper.getText(document).contains(query, ignoreCase = true)
            }
        }
    }.getOrDefault(emptyList())

    private fun extract(item: AcademyLibraryItem): File {
        val path = item.sourcePath
        require(path.startsWith("content/nous/") && path.endsWith(".pdf") && !path.contains("..")) { "Invalid bundled PDF path." }
        libraryDirectory.mkdirs()
        check(libraryDirectory.isDirectory) { "The local document library is not available." }
        val fileName = "${item.id}-${File(path).name}"
        val destination = File(libraryDirectory, fileName)
        loader.getResourceAsStream(path)?.use { input ->
            if (!destination.isFile || destination.length() == 0L) {
                val temporary = File(libraryDirectory, "$fileName.part")
                temporary.outputStream().use(input::copyTo)
                check(temporary.renameTo(destination)) { "Could not prepare the local PDF." }
            }
        } ?: error("This PDF is not included in the installed application.")
        return destination
    }

    private fun uniqueFile(directory: File, name: String): File {
        val dot = name.lastIndexOf('.')
        val stem = if (dot > 0) name.substring(0, dot) else name
        val extension = if (dot > 0) name.substring(dot) else ""
        var candidate = File(directory, name); var index = 1
        while (candidate.exists()) { candidate = File(directory, "$stem ($index)$extension"); index++ }
        return candidate
    }
}
