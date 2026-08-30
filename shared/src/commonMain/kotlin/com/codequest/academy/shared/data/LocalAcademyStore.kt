package com.codequest.academy.shared.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlinx.datetime.Clock

/**
 * Owns the application-maintained library catalogue only. Learner-owned tables
 * (profiles, settings, notes, bookmarks, and reading state) are never cleared
 * by this transition.
 */
data class CleanLibrarySummary(
    val version: String,
    val archivedBooks: Int,
    val archivedFiles: Int,
    val changed: Boolean
)

class LocalAcademyStore(private val driver: SqlDriver) {
    fun ensureSchema() {
        schemaStatements.forEach { driver.execute(null, it, 0) }
    }

    /**
     * Removes the old supplied catalogue from active storage after preserving a
     * minimal private archive reference for future migration. It deliberately
     * does not touch learner bookmarks, notes, highlights, reader positions,
     * accounts, sessions, or settings.
     */
    fun prepareEmptyLibrary(): CleanLibrarySummary {
        ensureSchema()
        val version = "nous-clean-library-v1"
        if (stringQuery("SELECT version FROM AcademyContentPack WHERE pack_id = 'nous-clean-library'") == version) {
            return CleanLibrarySummary(version, 0, 0, changed = false)
        }

        val books = count("AiBook").toInt()
        val files = count("AiKnowledgeFile").toInt()
        driver.execute(null, "BEGIN IMMEDIATE", 0)
        try {
            val archivedAt = Clock.System.now().toEpochMilliseconds()
            driver.execute(null, "INSERT OR IGNORE INTO LegacyLibraryArchive(content_type, content_id, title, source_path, archived_at) SELECT 'book', id, title, source_path, $archivedAt FROM AiBook", 0)
            driver.execute(null, "INSERT OR IGNORE INTO LegacyLibraryArchive(content_type, content_id, title, source_path, archived_at) SELECT 'intensive_file', id, title, source_path, $archivedAt FROM AiKnowledgeFile", 0)
            driver.execute(null, "DELETE FROM AiBookSection", 0)
            driver.execute(null, "DELETE FROM AiBook", 0)
            driver.execute(null, "DELETE FROM AiKnowledgeFile", 0)
            driver.execute(null, "DELETE FROM AcademySearch", 0)
            execute(
                "INSERT OR REPLACE INTO AcademyContentPack(pack_id, version, source_hash, installed_at) VALUES ('nous-clean-library', ?, 'empty-library-no-curriculum', ?)",
                listOf(version, archivedAt.toString())
            )
            driver.execute(null, "COMMIT", 0)
        } catch (error: Throwable) {
            driver.execute(null, "ROLLBACK", 0)
            throw error
        }
        return CleanLibrarySummary(version, books, files, changed = books > 0 || files > 0)
    }

    private fun count(table: String): Long = driver.executeQuery(null, "SELECT COUNT(*) FROM $table", {
        QueryResult.Value(if (it.next().value) it.getLong(0) ?: 0 else 0)
    }, 0).value

    private fun stringQuery(sql: String): String? = driver.executeQuery(null, sql, {
        QueryResult.Value(if (it.next().value) it.getString(0) else null)
    }, 0).value

    private fun execute(sql: String, values: List<String>) = driver.execute(null, sql, values.size) {
        values.forEachIndexed { index, value -> bindString(index, value) }
    }

    private companion object {
        val schemaStatements = listOf(
            "CREATE TABLE IF NOT EXISTS AcademyContentPack (pack_id TEXT PRIMARY KEY, version TEXT NOT NULL, source_hash TEXT NOT NULL, installed_at INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS AiBook (id TEXT PRIMARY KEY, title TEXT NOT NULL, current_version INTEGER NOT NULL, status TEXT NOT NULL, source_path TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS AiBookSection (id TEXT PRIMARY KEY, book_id TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, content_path TEXT NOT NULL, estimated_pages INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS AiKnowledgeFile (id TEXT PRIMARY KEY, title TEXT NOT NULL, summary TEXT NOT NULL, current_version INTEGER NOT NULL, status TEXT NOT NULL, source_path TEXT NOT NULL, required INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS LegacyLibraryArchive (content_type TEXT NOT NULL, content_id TEXT NOT NULL, title TEXT NOT NULL, source_path TEXT NOT NULL, archived_at INTEGER NOT NULL, PRIMARY KEY (content_type, content_id))"
        )
    }
}
