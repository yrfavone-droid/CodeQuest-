package com.codequest.academy.shared.documents

import com.codequest.academy.shared.data.OfflineLibraryResource

data class DocumentActionResult(val success: Boolean, val message: String)

/** Platform boundary for opening and saving verified local PDFs. No network path is available here. */
interface OfflineDocumentActions {
    fun availability(resource: OfflineLibraryResource): DocumentActionResult
    fun open(resource: OfflineLibraryResource): DocumentActionResult
    fun saveCopy(resource: OfflineLibraryResource): DocumentActionResult
}
