package com.codequest.academy.desktop

import com.codequest.academy.shared.data.AcademyLibraryItem
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JvmAcademyDocumentHandlerTest {
    @Test
    fun `downloads a bundled knowledge PDF without changing its contents`() {
        val root = Files.createTempDirectory("codequest-document-test")
        val downloads = root.resolve("downloads").toFile()
        val handler = JvmAcademyDocumentHandler(root.resolve("library").toFile(), downloads)
        val knowledgeItem = AcademyLibraryItem(
            "D01",
            "Advanced Python Patterns",
            "Supplied Knowledge Library brief PDF",
            "academy/documents/knowledge/D01_advanced_python_patterns.pdf"
        )

        val knowledgeResult = handler.downloadPdf(knowledgeItem)
        val bookResult = handler.downloadPdf(
            AcademyLibraryItem(
                "B01",
                "Python Foundations for Artificial Intelligence",
                "Supplied book blueprint PDF",
                "academy/source/BOOKS/blueprint_pdfs/B01_python_foundations_for_artificial_intelligence_100_page_blueprint.pdf"
            )
        )

        assertTrue(knowledgeResult.successful, knowledgeResult.message)
        assertTrue(bookResult.successful, bookResult.message)
        val files = downloads.listFiles()?.toList().orEmpty()
        assertEquals(2, files.size)
        files.forEach { file -> assertTrue(file.readBytes().copyOfRange(0, 4).decodeToString() == "%PDF") }
    }
}
