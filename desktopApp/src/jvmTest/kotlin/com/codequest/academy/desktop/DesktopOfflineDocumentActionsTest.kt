package com.codequest.academy.desktop

import com.codequest.academy.shared.data.NousLibraryCatalog
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopOfflineDocumentActionsTest {
    @Test
    fun installsVerifiedPdfsAndReportsCorruptionWithoutOpeningThem() {
        val root = Files.createTempDirectory("nous-library-test").toFile()
        try {
            val actions = DesktopOfflineDocumentActions(root)
            val installation = actions.installBundledResources()
            assertEquals(25, installation.size)
            assertTrue(installation.all { it.success })
            assertEquals(5, NousLibraryCatalog.resources.count { it.resourcePath.startsWith("content/books/") })
            assertEquals(20, NousLibraryCatalog.resources.count { it.resourcePath.startsWith("content/deep_dives/") })
            assertTrue(NousLibraryCatalog.resources.all { actions.availability(it).success })

            val corrupted = root.resolve(NousLibraryCatalog.resources.first().resourcePath)
            corrupted.writeBytes(byteArrayOf(0, 1, 2, 3))
            assertFalse(actions.availability(NousLibraryCatalog.resources.first()).success)
        } finally {
            root.deleteRecursively()
        }
    }
}
