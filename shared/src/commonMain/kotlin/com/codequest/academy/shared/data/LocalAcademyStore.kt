package com.codequest.academy.shared.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import com.codequest.academy.database.AppDatabase
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

data class LibraryReadingState(
    val resource: OfflineLibraryResource,
    val currentPage: Int,
    val updatedAt: Long
)

data class LibraryBookmark(
    val id: String,
    val resourceId: String,
    val page: Int,
    val createdAt: Long
)

class LocalAcademyStore(private val driver: SqlDriver) {
    private val database = AppDatabase(driver)

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
        database.transaction {
            val archivedAt = Clock.System.now().toEpochMilliseconds()
            driver.execute(null, "INSERT OR IGNORE INTO LegacyLibraryArchive(content_type, content_id, title, source_path, archived_at) SELECT 'book', id, title, source_path, $archivedAt FROM AiBook", 0)
            driver.execute(null, "INSERT OR IGNORE INTO LegacyLibraryArchive(content_type, content_id, title, source_path, archived_at) SELECT 'intensive_file', id, title, source_path, $archivedAt FROM AiKnowledgeFile", 0)
            driver.execute(null, "DELETE FROM AiBookSection", 0)
            driver.execute(null, "DELETE FROM AiBook", 0)
            driver.execute(null, "DELETE FROM AiKnowledgeFile", 0)
            execute(
                "INSERT OR REPLACE INTO AcademyContentPack(pack_id, version, source_hash, installed_at) VALUES ('nous-clean-library', ?, 'empty-library-no-curriculum', ?)",
                listOf(version, archivedAt.toString())
            )
        }
        return CleanLibrarySummary(version, books, files, changed = books > 0 || files > 0)
    }

    /** Installs catalogue metadata only. PDFs are kept in a separately validated, versioned local folder. */
    fun installVerifiedLibrary(resources: List<OfflineLibraryResource>) {
        require(resources.size == 25) { "The verified Nous package must contain exactly 25 resources." }
        require(resources.count { it.kind == LibraryKind.BOOK } == 5) { "The verified Nous package must contain five books." }
        require(resources.count { it.kind == LibraryKind.INTENSIVE_FILE } == 20) { "The verified Nous package must contain twenty intensive files." }
        ensureSchema()
        val now = Clock.System.now().toEpochMilliseconds()
        database.transaction {
            resources.forEach { resource ->
                driver.execute(null, """INSERT OR REPLACE INTO NousLibraryResource
                    (id, pack_id, kind, title, subtitle, page_count, resource_path, sha256, installed_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)""".trimIndent(), 9) {
                    bindString(0, resource.id); bindString(1, NousLibraryCatalog.packId); bindString(2, resource.kind.name)
                    bindString(3, resource.title); bindString(4, resource.subtitle); bindLong(5, resource.pageCount.toLong())
                    bindString(6, resource.resourcePath); bindString(7, resource.sha256); bindLong(8, now)
                }
            }
            driver.execute(null, "INSERT OR REPLACE INTO AcademyContentPack(pack_id, version, source_hash, installed_at) VALUES (?, ?, ?, ?)", 4) {
                bindString(0, NousLibraryCatalog.packId); bindString(1, "2"); bindString(2, NousLibraryCatalog.packageSha256); bindLong(3, now)
            }
        }
    }

    fun libraryResources(kind: LibraryKind? = null): List<OfflineLibraryResource> = driver.executeQuery(null, """
        SELECT id, kind, title, subtitle, page_count, resource_path, sha256
        FROM NousLibraryResource WHERE pack_id = ? ${if (kind == null) "" else "AND kind = ?"} ORDER BY id
    """.trimIndent(), {
        QueryResult.Value(buildList {
            while (it.next().value) add(OfflineLibraryResource(
                id = requireNotNull(it.getString(0)), kind = LibraryKind.valueOf(requireNotNull(it.getString(1))),
                title = requireNotNull(it.getString(2)), subtitle = requireNotNull(it.getString(3)), pageCount = requireNotNull(it.getLong(4)).toInt(),
                resourcePath = requireNotNull(it.getString(5)), sha256 = requireNotNull(it.getString(6))
            ))
        })
    }, if (kind == null) 1 else 2) {
        bindString(0, NousLibraryCatalog.packId)
        if (kind != null) bindString(1, kind.name)
    }.value

    fun readingState(userId: String, resource: OfflineLibraryResource): LibraryReadingState {
        val row = driver.executeQuery(null, "SELECT current_page, updated_at FROM NousReaderState WHERE user_id = ? AND content_id = ?", {
            QueryResult.Value(if (it.next().value) Pair(requireNotNull(it.getLong(0)).toInt(), requireNotNull(it.getLong(1))) else null)
        }, 2) { bindString(0, userId); bindString(1, resource.id) }.value
        return LibraryReadingState(resource, row?.first ?: 1, row?.second ?: 0L)
    }

    fun saveReadingPage(userId: String, resource: OfflineLibraryResource, page: Int) {
        require(page in 1..resource.pageCount) { "Page must be within this document." }
        driver.execute(null, "INSERT OR REPLACE INTO NousReaderState(user_id, content_id, current_page, zoom, updated_at) VALUES (?, ?, ?, COALESCE((SELECT zoom FROM NousReaderState WHERE user_id = ? AND content_id = ?), 1), ?)", 6) {
            bindString(0, userId); bindString(1, resource.id); bindLong(2, page.toLong()); bindString(3, userId); bindString(4, resource.id); bindLong(5, Clock.System.now().toEpochMilliseconds())
        }
    }

    fun readingStates(userId: String): List<LibraryReadingState> = libraryResources().map { readingState(userId, it) }.filter { it.updatedAt > 0 }

    fun addBookmark(userId: String, resource: OfflineLibraryResource, page: Int): LibraryBookmark {
        require(page in 1..resource.pageCount) { "Page must be within this document." }
        val now = Clock.System.now().toEpochMilliseconds()
        val id = "${resource.id.lowercase()}-$now"
        driver.execute(null, "INSERT OR REPLACE INTO AiBookmark(id, user_id, content_type, content_id, location, created_at) VALUES (?, ?, 'pdf', ?, ?, ?)", 6) {
            bindString(0, id); bindString(1, userId); bindString(2, resource.id); bindString(3, page.toString()); bindLong(4, now)
        }
        return LibraryBookmark(id, resource.id, page, now)
    }

    fun bookmarks(userId: String): List<LibraryBookmark> = driver.executeQuery(null, "SELECT id, content_id, location, created_at FROM AiBookmark WHERE user_id = ? AND content_type = 'pdf' ORDER BY created_at DESC", {
        QueryResult.Value(buildList {
            while (it.next().value) add(LibraryBookmark(requireNotNull(it.getString(0)), requireNotNull(it.getString(1)), requireNotNull(it.getString(2)).toIntOrNull() ?: 1, requireNotNull(it.getLong(3))))
        })
    }, 1) { bindString(0, userId) }.value

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
            "CREATE TABLE IF NOT EXISTS AiBookmark (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, content_type TEXT NOT NULL, content_id TEXT NOT NULL, location TEXT NOT NULL, created_at INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS NousReaderState (user_id TEXT NOT NULL, content_id TEXT NOT NULL, current_page INTEGER NOT NULL DEFAULT 1, zoom REAL NOT NULL DEFAULT 1, updated_at INTEGER NOT NULL, PRIMARY KEY (user_id, content_id))",
            "CREATE TABLE IF NOT EXISTS NousLibraryResource (id TEXT PRIMARY KEY, pack_id TEXT NOT NULL, kind TEXT NOT NULL, title TEXT NOT NULL, subtitle TEXT NOT NULL, page_count INTEGER NOT NULL, resource_path TEXT NOT NULL, sha256 TEXT NOT NULL, installed_at INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS LegacyLibraryArchive (content_type TEXT NOT NULL, content_id TEXT NOT NULL, title TEXT NOT NULL, source_path TEXT NOT NULL, archived_at INTEGER NOT NULL, PRIMARY KEY (content_type, content_id))",
            "CREATE TABLE IF NOT EXISTS LearningHubSection (id TEXT PRIMARY KEY, position INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'awaiting_import', content_version TEXT, source_checksum TEXT)",
            "CREATE TABLE IF NOT EXISTS LearningHubTopic (id TEXT PRIMARY KEY, section_id TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'awaiting_import')",
            "CREATE TABLE IF NOT EXISTS LearningHubLesson (id TEXT PRIMARY KEY, section_id TEXT NOT NULL, topic_id TEXT, title TEXT NOT NULL, position INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'awaiting_import', current_version INTEGER NOT NULL DEFAULT 0, estimated_minutes INTEGER)",
            "CREATE TABLE IF NOT EXISTS LearningHubQuickSheet (id TEXT PRIMARY KEY, lesson_id TEXT NOT NULL, content_json TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'awaiting_import', version INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS LearningHubPracticeSet (id TEXT PRIMARY KEY, lesson_id TEXT NOT NULL, title TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'awaiting_import')",
            "CREATE TABLE IF NOT EXISTS LearningHubPracticeQuestion (id TEXT PRIMARY KEY, practice_set_id TEXT NOT NULL, question_json TEXT NOT NULL, answer_json TEXT, explanation_json TEXT, difficulty TEXT, topic_tag TEXT, status TEXT NOT NULL DEFAULT 'awaiting_import')",
            "CREATE TABLE IF NOT EXISTS LearningHubChallenge (id TEXT PRIMARY KEY, section_id TEXT NOT NULL, content_json TEXT, status TEXT NOT NULL DEFAULT 'awaiting_import')",
            "CREATE TABLE IF NOT EXISTS LearningHubProgress (user_id TEXT NOT NULL, section_id TEXT NOT NULL, lesson_id TEXT, best_practice_score REAL, attempts INTEGER NOT NULL DEFAULT 0, completed INTEGER NOT NULL DEFAULT 0, last_opened_at INTEGER, time_spent_seconds INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (user_id, section_id, lesson_id))",
            "CREATE TABLE IF NOT EXISTS LearningHubContentValidation (id TEXT PRIMARY KEY, content_version TEXT NOT NULL, status TEXT NOT NULL, errors_json TEXT NOT NULL DEFAULT '[]', validated_at INTEGER)",
            "CREATE TABLE IF NOT EXISTS LearningHubQuizAttempt (id INTEGER PRIMARY KEY AUTOINCREMENT, user_id TEXT NOT NULL, lesson_id TEXT NOT NULL, correct_count INTEGER NOT NULL, total_count INTEGER NOT NULL, incorrect_ids TEXT NOT NULL DEFAULT '', created_at INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS LearningHubLessonState (user_id TEXT NOT NULL, lesson_id TEXT NOT NULL, article_units_read INTEGER NOT NULL DEFAULT 0, review_items TEXT NOT NULL DEFAULT '', note TEXT NOT NULL DEFAULT '', bookmarked INTEGER NOT NULL DEFAULT 0, updated_at INTEGER NOT NULL, PRIMARY KEY (user_id, lesson_id))",
            "CREATE TABLE IF NOT EXISTS LearningHubPatchVersion (version_key TEXT PRIMARY KEY, content_version TEXT NOT NULL, source_hash TEXT NOT NULL, section_count INTEGER NOT NULL, lesson_count INTEGER NOT NULL, problem_count INTEGER NOT NULL, installed_at INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS LearningHubActivePatch (singleton INTEGER PRIMARY KEY CHECK(singleton = 1), version_key TEXT NOT NULL)"
        )
    }
}
