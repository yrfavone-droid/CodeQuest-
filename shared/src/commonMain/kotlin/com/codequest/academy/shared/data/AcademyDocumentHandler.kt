package com.codequest.academy.shared.data

/**
 * Desktop document actions are deliberately local. The supplied PDFs are
 * bundled with the installer and are never uploaded before opening or saving.
 */
interface AcademyDocumentHandler {
    fun openPdf(item: AcademyLibraryItem): AcademyDocumentActionResult
    fun downloadPdf(item: AcademyLibraryItem): AcademyDocumentActionResult
}

data class AcademyDocumentActionResult(
    val successful: Boolean,
    val message: String
)
