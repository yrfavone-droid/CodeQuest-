package com.codequest.academy.desktop

import com.codequest.academy.shared.data.AcademyLibraryItem
import com.codequest.academy.shared.data.LibraryKind
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmAcademyDocumentHandlerTest {
    private val book = AcademyLibraryItem("BOOK-01", "Python Foundations for Artificial Intelligence", "Core Book", "content/nous/books/book_01_python_foundations_for_artificial_intelligence.pdf", 150, LibraryKind.BOOK, "nous_book_01")
    private val file = AcademyLibraryItem("FILE-01", "Advanced Python Patterns", "Intensive File", "content/nous/intensive_files/deep_01_advanced_python_patterns.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_01")

    @Test
    fun `downloads bundled Nous PDFs and reads their actual page counts`() {
        val root = Files.createTempDirectory("nous-document-test")
        val downloads = root.resolve("downloads").toFile()
        val handler = JvmAcademyDocumentHandler(root.resolve("library").toFile(), downloads)

        assertTrue(handler.downloadPdf(book).successful)
        assertTrue(handler.downloadPdf(file).successful)
        assertEquals(150, handler.pageCount(book))
        assertEquals(50, handler.pageCount(file))
        assertTrue(handler.renderPage(book, 1, 1f) != null, "The first bundled book page should render offline.")
        assertTrue(handler.searchPages(book, "Python").isNotEmpty(), "Local PDF search should find document text.")
        assertEquals(2, downloads.listFiles()?.size)
        downloads.listFiles().orEmpty().forEach { pdf -> assertTrue(pdf.readBytes().copyOfRange(0, 4).decodeToString() == "%PDF") }
    }
}
