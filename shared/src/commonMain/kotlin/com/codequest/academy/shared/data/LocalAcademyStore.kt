package com.codequest.academy.shared.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlinx.datetime.Clock

/** Metadata for one bundled Nous document. The local PDF remains the source of truth. */
data class AcademyLibraryItem(
    val id: String,
    val title: String,
    val detail: String,
    val sourcePath: String,
    val pageCount: Int,
    val kind: LibraryKind,
    val coverResource: String
)

enum class LibraryKind { BOOK, INTENSIVE_FILE }
data class ReaderState(val page: Int = 1, val zoom: Float = 1f)

data class LocalAcademyInstallResult(
    val version: String,
    val plannedProblemSlots: Int = 0,
    val publishedProblems: Int = 0,
    val tracks: Int = 0,
    val lessons: Int = 0,
    val books: Int,
    val knowledgeFiles: Int,
    val changed: Boolean
)

/**
 * The offline Nous library is stored in the existing SQLite file. Installation
 * changes application-owned catalogue rows only; learner-owned records stay put.
 */
class LocalAcademyStore(private val driver: SqlDriver) {
    fun ensureSchema() {
        schemaStatements.forEach { driver.execute(null, it, 0) }
        driver.execute(null, "CREATE VIRTUAL TABLE IF NOT EXISTS AcademySearch USING fts5(content_type, content_id UNINDEXED, title, body)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_ai_bookmark_content ON AiBookmark(user_id, content_id)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_nous_reader_state_user ON NousReaderState(user_id, content_id)", 0)
    }

    fun installNousLibrary(): LocalAcademyInstallResult {
        ensureSchema()
        val version = "nous-offline-library-v2"
        val installed = stringQuery("SELECT version FROM AcademyContentPack WHERE pack_id = 'nous-ai-library'")
        if (installed == version && count("AiBook") == 5L && count("AiKnowledgeFile") == 20L) return summary(version, false)

        driver.execute(null, "BEGIN IMMEDIATE", 0)
        try {
            migrateBookmarks()
            driver.execute(null, "DELETE FROM AiBookSection", 0)
            driver.execute(null, "DELETE FROM AiBook", 0)
            driver.execute(null, "DELETE FROM AiKnowledgeFile", 0)
            driver.execute(null, "DELETE FROM AcademySearch", 0)
            documentSeeds.filter { it.kind == LibraryKind.BOOK }.forEach { item ->
                execute("INSERT INTO AiBook(id, title, current_version, status, source_path) VALUES (?, ?, 2, 'published', ?)", listOf(item.id, item.title, item.sourcePath))
                execute("INSERT INTO AiBookSection(id, book_id, title, position, content_path, estimated_pages) VALUES (?, ?, 'Offline PDF', 1, ?, ?)", listOf("${item.id}:pdf", item.id, item.sourcePath, item.pageCount.toString()))
            }
            documentSeeds.filter { it.kind == LibraryKind.INTENSIVE_FILE }.forEach { item ->
                execute("INSERT INTO AiKnowledgeFile(id, title, summary, current_version, status, source_path, required) VALUES (?, ?, ?, 2, 'published', ?, 0)", listOf(item.id, item.title, item.detail, item.sourcePath))
            }
            documentSeeds.forEach { item -> execute("INSERT INTO AcademySearch(content_type, content_id, title, body) VALUES (?, ?, ?, ?)", listOf(item.kind.name.lowercase(), item.id, item.title, item.detail)) }
            execute("INSERT OR REPLACE INTO AcademyContentPack(pack_id, version, source_hash, installed_at) VALUES ('nous-ai-library', ?, '25-validated-pdfs-1750-pages', ?)", listOf(version, now().toString()))
            driver.execute(null, "COMMIT", 0)
        } catch (error: Throwable) {
            driver.execute(null, "ROLLBACK", 0)
            throw error
        }
        return summary(version, true)
    }

    fun books(): List<AcademyLibraryItem> = documentSeeds.filter { it.kind == LibraryKind.BOOK }
    fun intensiveFiles(): List<AcademyLibraryItem> = documentSeeds.filter { it.kind == LibraryKind.INTENSIVE_FILE }

    fun readerState(userId: String, contentId: String): ReaderState = driver.executeQuery(null,
        "SELECT current_page, zoom FROM NousReaderState WHERE user_id = ? AND content_id = ?", {
            QueryResult.Value(if (it.next().value) ReaderState(it.getLong(0)?.toInt() ?: 1, (it.getDouble(1) ?: 1.0).toFloat()) else ReaderState())
        }, 2) { bindString(0, userId); bindString(1, contentId) }.value

    fun saveReaderState(userId: String, contentId: String, page: Int, zoom: Float) =
        execute("INSERT OR REPLACE INTO NousReaderState(user_id, content_id, current_page, zoom, updated_at) VALUES (?, ?, ?, ?, ?)", listOf(userId, contentId, page.coerceAtLeast(1).toString(), zoom.coerceIn(0.7f, 2.5f).toString(), now().toString()))

    fun bookmarkedPages(userId: String, contentId: String): Set<Int> = driver.executeQuery(null,
        "SELECT location FROM AiBookmark WHERE user_id = ? AND content_id = ? AND content_type = 'pdf'", {
            QueryResult.Value(buildSet { while (it.next().value) it.getString(0)?.toIntOrNull()?.let(::add) })
        }, 2) { bindString(0, userId); bindString(1, contentId) }.value

    fun toggleBookmark(userId: String, contentId: String, page: Int) {
        val location = page.coerceAtLeast(1).toString()
        val exists = driver.executeQuery(null, "SELECT id FROM AiBookmark WHERE user_id = ? AND content_id = ? AND content_type = 'pdf' AND location = ?", {
            QueryResult.Value(if (it.next().value) it.getString(0) else null)
        }, 3) { bindString(0, userId); bindString(1, contentId); bindString(2, location) }.value
        if (exists == null) execute("INSERT INTO AiBookmark(id, user_id, content_type, content_id, location, created_at) VALUES (?, ?, 'pdf', ?, ?, ?)", listOf("$userId:$contentId:$location", userId, contentId, location, now().toString()))
        else execute("DELETE FROM AiBookmark WHERE id = ?", listOf(exists))
    }

    private fun migrateBookmarks() {
        (1..5).forEach { number -> execute("UPDATE AiBookmark SET content_id = ? WHERE content_id = ?", listOf("BOOK-%02d".format(number), "B%02d".format(number))) }
        (1..20).forEach { number -> execute("UPDATE AiBookmark SET content_id = ? WHERE content_id = ?", listOf("FILE-%02d".format(number), "D%02d".format(number))) }
    }

    private fun summary(version: String, changed: Boolean) = LocalAcademyInstallResult(version, books = count("AiBook").toInt(), knowledgeFiles = count("AiKnowledgeFile").toInt(), changed = changed)
    private fun count(table: String): Long = driver.executeQuery(null, "SELECT COUNT(*) FROM $table", { QueryResult.Value(if (it.next().value) it.getLong(0) ?: 0 else 0) }, 0).value
    private fun stringQuery(sql: String): String? = driver.executeQuery(null, sql, { QueryResult.Value(if (it.next().value) it.getString(0) else null) }, 0).value
    private fun execute(sql: String, values: List<String>) = driver.execute(null, sql, values.size) { values.forEachIndexed { index, value -> bindString(index, value) } }
    private fun now(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        val documentSeeds = listOf(
            doc("BOOK-01", "Python Foundations for Artificial Intelligence", "Core Book · Offline reference", "content/nous/books/book_01_python_foundations_for_artificial_intelligence.pdf", 150, LibraryKind.BOOK, "nous_book_01"),
            doc("BOOK-02", "Mathematics for Machine Learning", "Core Book · Offline reference", "content/nous/books/book_02_mathematics_for_machine_learning.pdf", 150, LibraryKind.BOOK, "nous_book_02"),
            doc("BOOK-03", "Algorithms, Data Structures, and AI Problem Solving", "Core Book · Offline reference", "content/nous/books/book_03_algorithms_data_structures_and_ai_problem_solving.pdf", 150, LibraryKind.BOOK, "nous_book_03"),
            doc("BOOK-04", "Machine Learning from First Principles", "Core Book · Offline reference", "content/nous/books/book_04_machine_learning_from_first_principles.pdf", 150, LibraryKind.BOOK, "nous_book_04"),
            doc("BOOK-05", "Deep Learning, Generative AI, and Responsible Deployment", "Core Book · Offline reference", "content/nous/books/book_05_deep_learning_generative_ai_and_responsible_deployment.pdf", 150, LibraryKind.BOOK, "nous_book_05"),
            doc("FILE-01", "Advanced Python Patterns", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_01_advanced_python_patterns.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_01"),
            doc("FILE-02", "NumPy for Numerical Computing", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_02_numpy_for_numerical_computing.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_02"),
            doc("FILE-03", "Pandas for Real Data", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_03_pandas_for_real_data.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_03"),
            doc("FILE-04", "SQL for Data and AI", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_04_sql_for_data_and_ai.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_04"),
            doc("FILE-05", "Linear Algebra Deep Dive", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_05_linear_algebra_deep_dive.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_05"),
            doc("FILE-06", "Probability and Statistics Deep Dive", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_06_probability_and_statistics_deep_dive.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_06"),
            doc("FILE-07", "Calculus and Optimization", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_07_calculus_and_optimization.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_07"),
            doc("FILE-08", "Data Structures in Production", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_08_data_structures_in_production.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_08"),
            doc("FILE-09", "Algorithm Design Patterns", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_09_algorithm_design_patterns.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_09"),
            doc("FILE-10", "Exploratory Analysis and Data Visualization", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_10_exploratory_analysis_and_visualization.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_10"),
            doc("FILE-11", "Preprocessing and Feature Engineering", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_11_preprocessing_and_feature_engineering.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_11"),
            doc("FILE-12", "Supervised Learning", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_12_supervised_learning.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_12"),
            doc("FILE-13", "Unsupervised Learning", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_13_unsupervised_learning.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_13"),
            doc("FILE-14", "Time Series Forecasting", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_14_time_series_forecasting.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_14"),
            doc("FILE-15", "Deep Learning with PyTorch", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_15_deep_learning_with_pytorch.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_15"),
            doc("FILE-16", "Computer Vision Systems", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_16_computer_vision_systems.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_16"),
            doc("FILE-17", "Natural Language Processing", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_17_natural_language_processing.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_17"),
            doc("FILE-18", "Transformers, LLMs, and Retrieval", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_18_transformers_llms_and_retrieval.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_18"),
            doc("FILE-19", "Reinforcement Learning", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_19_reinforcement_learning.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_19"),
            doc("FILE-20", "MLOps and Responsible AI", "Intensive File · Offline technical reference", "content/nous/intensive_files/deep_20_mlops_and_responsible_ai.pdf", 50, LibraryKind.INTENSIVE_FILE, "nous_file_20")
        )
        private fun doc(id: String, title: String, detail: String, path: String, pages: Int, kind: LibraryKind, cover: String) = AcademyLibraryItem(id, title, detail, path, pages, kind, cover)
        val schemaStatements = listOf(
            "CREATE TABLE IF NOT EXISTS AcademyContentPack (pack_id TEXT PRIMARY KEY, version TEXT NOT NULL, source_hash TEXT NOT NULL, installed_at INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS AiBook (id TEXT PRIMARY KEY, title TEXT NOT NULL, current_version INTEGER NOT NULL, status TEXT NOT NULL, source_path TEXT NOT NULL)",
            "CREATE TABLE IF NOT EXISTS AiBookSection (id TEXT PRIMARY KEY, book_id TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, content_path TEXT NOT NULL, estimated_pages INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS AiKnowledgeFile (id TEXT PRIMARY KEY, title TEXT NOT NULL, summary TEXT NOT NULL, current_version INTEGER NOT NULL, status TEXT NOT NULL, source_path TEXT NOT NULL, required INTEGER NOT NULL DEFAULT 0)",
            "CREATE TABLE IF NOT EXISTS AiBookmark (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, content_type TEXT NOT NULL, content_id TEXT NOT NULL, location TEXT NOT NULL, created_at INTEGER NOT NULL)",
            "CREATE TABLE IF NOT EXISTS NousReaderState (user_id TEXT NOT NULL, content_id TEXT NOT NULL, current_page INTEGER NOT NULL DEFAULT 1, zoom REAL NOT NULL DEFAULT 1, updated_at INTEGER NOT NULL, PRIMARY KEY (user_id, content_id))"
        )
    }
}
