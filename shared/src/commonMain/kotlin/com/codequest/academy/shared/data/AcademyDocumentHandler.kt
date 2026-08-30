package com.codequest.academy.shared.data

import androidx.compose.ui.graphics.ImageBitmap

/**
 * Desktop document actions are deliberately local. The supplied PDFs are
 * bundled with the installer and are never uploaded before opening or saving.
 */
interface AcademyDocumentHandler {
    fun openPdf(item: AcademyLibraryItem): AcademyDocumentActionResult
    fun downloadPdf(item: AcademyLibraryItem): AcademyDocumentActionResult
}

/** Local renderer contract. Implementations must only read the packaged PDF. */
interface OfflinePdfReader {
    fun pageCount(item: AcademyLibraryItem): Int
    fun renderPage(item: AcademyLibraryItem, page: Int, zoom: Float): ImageBitmap?
    fun tableOfContents(item: AcademyLibraryItem): List<String>
    fun searchPages(item: AcademyLibraryItem, query: String): List<Int>
}

data class AcademyDocumentActionResult(
    val successful: Boolean,
    val message: String
)
