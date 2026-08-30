package com.codequest.academy.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.codequest.academy.database.AppDatabase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalAcademyStoreTest {
    private fun repository(): ProgressRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return ProgressRepository(driver)
    }

    @Test
    fun installsOnlyTheStableNousLibraryCatalog() {
        val repository = repository()
        val first = repository.installNousLibrary()
        val second = repository.installNousLibrary()

        assertTrue(first.changed)
        assertFalse(second.changed)
        assertEquals(5, first.books)
        assertEquals(20, first.knowledgeFiles)
        assertEquals((1..5).map { "BOOK-%02d".format(it) }, repository.getNousBooks().map { it.id })
        assertEquals((1..20).map { "FILE-%02d".format(it) }, repository.getNousIntensiveFiles().map { it.id })
        assertTrue(repository.getNousBooks().all { it.pageCount == 150 && it.kind == LibraryKind.BOOK })
        assertTrue(repository.getNousIntensiveFiles().all { it.pageCount == 50 && it.kind == LibraryKind.INTENSIVE_FILE })
    }

    @Test
    fun persistsLocalReaderPositionAndBookmarks() {
        val repository = repository()
        repository.installNousLibrary()
        repository.createProfile("Offline learner")
        repository.saveReaderState("BOOK-01", 12, 1.4f)
        repository.toggleReaderBookmark("BOOK-01", 12)

        assertEquals(12, repository.getReaderState("BOOK-01").page)
        assertEquals(1.4f, repository.getReaderState("BOOK-01").zoom)
        assertEquals(setOf(12), repository.getReaderBookmarks("BOOK-01"))
    }
}
