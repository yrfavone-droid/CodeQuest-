package com.codequest.academy.shared.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Local-only Academy storage. It deliberately has no network dependency: the
 * bundled source pack and every learner-owned record stay in SQLite.
 */
data class AcademyTrackRecord(
    val id: String,
    val title: String,
    val position: Int,
    val problemSlots: Int,
    val status: String
)

data class AcademyLessonRecord(
    val id: String,
    val objectiveId: String,
    val title: String,
    val estimatedMinutes: Int,
    val contentJson: String
)

data class AcademySearchResult(val contentType: String, val contentId: String, val title: String, val excerpt: String)

data class AcademyLibraryItem(val id: String, val title: String, val detail: String, val sourcePath: String)

data class LocalAcademyInstallResult(
    val version: String,
    val plannedProblemSlots: Int,
    val publishedProblems: Int,
    val tracks: Int,
    val lessons: Int,
    val books: Int,
    val knowledgeFiles: Int,
    val changed: Boolean
)

@Serializable
private data class LessonContent(
    val objectives: List<String>,
    val prerequisites: List<String>,
    val explanation: String,
    val workedExample: String,
    val guidedPractice: String,
    val independentPractice: String,
    val hints: List<String>,
    val commonMistakes: List<String>,
    val masteryCheck: String,
    val accessibilityText: String,
    val sources: List<String>
)

class LocalAcademyStore(private val driver: SqlDriver) {
    private val json = Json { encodeDefaults = true }

    fun ensureSchema() {
        schemaStatements.forEach { driver.execute(null, it, 0) }
        // FTS is intentionally local: it indexes bundled learning material and
        // learner-readable metadata without uploading a search query anywhere.
        driver.execute(null, "CREATE VIRTUAL TABLE IF NOT EXISTS AcademySearch USING fts5(content_type, content_id UNINDEXED, title, body)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_ai_problem_status ON AiProblem(status)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_ai_lesson_objective ON AiLesson(objective_id)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_ai_attempt_user_problem ON AiAttempt(user_id, problem_id)", 0)
        driver.execute(null, "CREATE INDEX IF NOT EXISTS idx_local_event_user_time ON LocalAnalyticsEvent(user_id, occurred_at)", 0)
    }

    fun installBundledContent(manifestCsv: String): LocalAcademyInstallResult {
        val version = "local-ai-academy-2026.08.30"
        val sourceHash = "manifest-${manifestCsv.length}-${manifestCsv.hashCode()}"
        val installed = stringQuery("SELECT version FROM AcademyContentPack WHERE pack_id = 'codequest-ai-academy'")
        if (installed == version && longQuery("SELECT COUNT(*) FROM AiProblem") >= 10_000L) {
            return summary(version, changed = false)
        }

        val rows = manifestCsv.lineSequence().drop(1).filter { it.isNotBlank() }.map(::parseManifestRow).toList()
        require(rows.size == 10_000) { "Expected exactly 10,000 local problem slots, found ${rows.size}." }
        require(rows.map { it.problemId }.toSet().size == rows.size) { "Problem slot identifiers must be unique." }

        driver.execute(null, "BEGIN IMMEDIATE", 0)
        try {
            rows.groupBy { it.trackId }.entries.sortedBy { it.key }.forEachIndexed { index, entry ->
                val first = entry.value.first()
                execute("INSERT OR REPLACE INTO AiTrack(id, title, position, problem_slots, status) VALUES (?, ?, ?, ?, 'published')", listOf(first.trackId, first.trackTitle, (index + 1).toString(), entry.value.size.toString()))
            }
            rows.groupBy { it.trackId to it.moduleTitle }.entries.sortedBy { it.key.first + it.key.second }.forEachIndexed { index, entry ->
                val (trackId, moduleTitle) = entry.key
                execute("INSERT OR IGNORE INTO AiModule(id, track_id, title, position, status) VALUES (?, ?, ?, ?, 'published')", listOf(moduleId(trackId, moduleTitle), trackId, moduleTitle, (index + 1).toString()))
            }
            rows.groupBy { it.objectiveId }.forEach { (objectiveId, items) ->
                val first = items.first()
                execute("INSERT OR IGNORE INTO AiObjective(id, module_id, title, level, prerequisite_ids_json) VALUES (?, ?, ?, 'beginner', '[]')", listOf(objectiveId, moduleId(first.trackId, first.moduleTitle), "Apply ${first.moduleTitle}"))
            }
            rows.forEach { row ->
                execute(
                    "INSERT OR IGNORE INTO AiProblem(id, objective_id, track_id, module_title, problem_type, difficulty, current_version, status) VALUES (?, ?, ?, ?, ?, ?, 0, 'planned')",
                    listOf(row.problemId, row.objectiveId, row.trackId, row.moduleTitle, row.problemType, row.difficulty)
                )
            }
            seedRealFoundationContent()
            seedBooksAndKnowledge()
            execute("INSERT OR REPLACE INTO AcademyContentPack(pack_id, version, source_hash, installed_at) VALUES ('codequest-ai-academy', ?, ?, ?)", listOf(version, sourceHash, now().toString()))
            rebuildSearchIndex()
            driver.execute(null, "COMMIT", 0)
        } catch (error: Throwable) {
            driver.execute(null, "ROLLBACK", 0)
            throw error
        }
        return summary(version, changed = true)
    }

    fun tracks(): List<AcademyTrackRecord> = driver.executeQuery(null,
        "SELECT id, title, position, problem_slots, status FROM AiTrack ORDER BY position", {
            QueryResult.Value(buildList {
                while (it.next().value) add(AcademyTrackRecord(requireNotNull(it.getString(0)), requireNotNull(it.getString(1)), it.getLong(2)?.toInt() ?: 0, it.getLong(3)?.toInt() ?: 0, requireNotNull(it.getString(4))))
            })
        }, 0
    ).value

    fun lessons(): List<AcademyLessonRecord> = driver.executeQuery(null,
        "SELECT AiLesson.id, AiLesson.objective_id, AiLesson.title, AiLesson.estimated_minutes, AiLessonVersion.content_json FROM AiLesson JOIN AiLessonVersion ON AiLesson.id = AiLessonVersion.lesson_id AND AiLesson.current_version = AiLessonVersion.version WHERE AiLesson.status = 'published' ORDER BY AiLesson.id", {
            QueryResult.Value(buildList {
                while (it.next().value) add(AcademyLessonRecord(requireNotNull(it.getString(0)), requireNotNull(it.getString(1)), requireNotNull(it.getString(2)), it.getLong(3)?.toInt() ?: 0, requireNotNull(it.getString(4))))
            })
        }, 0
    ).value

    fun search(query: String): List<AcademySearchResult> {
        if (query.trim().length < 2) return emptyList()
        return driver.executeQuery(null,
            "SELECT content_type, content_id, title, snippet(AcademySearch, 3, '[', ']', '…', 12) FROM AcademySearch WHERE AcademySearch MATCH ? ORDER BY rank LIMIT 20", {
                QueryResult.Value(buildList {
                    while (it.next().value) add(AcademySearchResult(requireNotNull(it.getString(0)), requireNotNull(it.getString(1)), requireNotNull(it.getString(2)), requireNotNull(it.getString(3))))
                })
            }, 1
        ) { bindString(0, query.trim()) }.value
    }

    fun books(): List<AcademyLibraryItem> = driver.executeQuery(null,
        "SELECT id, title, '100-page local reading plan', source_path FROM AiBook WHERE status = 'published' ORDER BY id", {
            QueryResult.Value(buildList {
                while (it.next().value) add(AcademyLibraryItem(requireNotNull(it.getString(0)), requireNotNull(it.getString(1)), requireNotNull(it.getString(2)), requireNotNull(it.getString(3))))
            })
        }, 0
    ).value

    fun knowledge(): List<AcademyLibraryItem> = driver.executeQuery(null,
        "SELECT id, title, summary, source_path FROM AiKnowledgeFile WHERE status = 'published' ORDER BY id", {
            QueryResult.Value(buildList {
                while (it.next().value) add(AcademyLibraryItem(requireNotNull(it.getString(0)), requireNotNull(it.getString(1)), requireNotNull(it.getString(2)), requireNotNull(it.getString(3))))
            })
        }, 0
    ).value

    fun recordAttempt(userId: String, problemId: String, answerJson: String, correct: Boolean, hintsUsed: Int, misconception: String? = null) {
        val timestamp = now()
        val attemptId = "$userId:$problemId:$timestamp"
        execute("INSERT INTO AiAttempt(id, user_id, problem_id, answer_json, correct, hints_used, execution_json, created_at) VALUES (?, ?, ?, ?, ?, ?, NULL, ?)", listOf(attemptId, userId, problemId, answerJson, if (correct) "1" else "0", hintsUsed.toString(), timestamp.toString()))
        val objective = stringQuery("SELECT objective_id FROM AiProblem WHERE id = ?", problemId) ?: return
        val oldScore = driver.executeQuery(null, "SELECT score FROM AiMastery WHERE user_id = ? AND objective_id = ?", { QueryResult.Value(if (it.next().value) it.getDouble(0) ?: 0.0 else 0.0) }, 2) { bindString(0, userId); bindString(1, objective) }.value
        val adjusted = if (correct) oldScore + (100.0 - oldScore) * if (hintsUsed == 0) 0.28 else 0.14 else oldScore * 0.82
        val reviewDays = if (correct && hintsUsed == 0) 3 else 1
        execute("INSERT OR REPLACE INTO AiMastery(user_id, objective_id, score, evidence_count, last_evidence_at, next_review_at) VALUES (?, ?, ?, COALESCE((SELECT evidence_count FROM AiMastery WHERE user_id = ? AND objective_id = ?), 0) + 1, ?, ?)", listOf(userId, objective, adjusted.coerceIn(0.0, 100.0).toString(), userId, objective, timestamp.toString(), (timestamp + reviewDays * 86_400_000L).toString()))
        execute("INSERT OR REPLACE INTO AiReviewQueue(user_id, objective_id, due_at, reason) VALUES (?, ?, ?, ?)", listOf(userId, objective, (timestamp + reviewDays * 86_400_000L).toString(), if (correct) "spaced review" else "incorrect response"))
        if (!correct) execute("INSERT INTO AiMistakeNotebook(id, user_id, problem_id, misconception_tag, note, created_at, resolved_at) VALUES (?, ?, ?, ?, ?, ?, NULL)", listOf("$attemptId:mistake", userId, problemId, misconception ?: "unclassified", "Review this attempt and explain the corrected reasoning.", timestamp.toString()))
        execute("INSERT INTO LocalAnalyticsEvent(id, user_id, event_type, content_id, payload_json, occurred_at) VALUES (?, ?, 'problem_attempt', ?, ?, ?)", listOf("$attemptId:event", userId, problemId, "{\"correct\":$correct,\"hintsUsed\":$hintsUsed}", timestamp.toString()))
    }

    private fun summary(version: String, changed: Boolean) = LocalAcademyInstallResult(
        version = version,
        plannedProblemSlots = longQuery("SELECT COUNT(*) FROM AiProblem WHERE status = 'planned'").toInt(),
        publishedProblems = longQuery("SELECT COUNT(*) FROM AiProblem WHERE status = 'published'").toInt(),
        tracks = longQuery("SELECT COUNT(*) FROM AiTrack").toInt(),
        lessons = longQuery("SELECT COUNT(*) FROM AiLesson WHERE status = 'published'").toInt(),
        books = longQuery("SELECT COUNT(*) FROM AiBook").toInt(),
        knowledgeFiles = longQuery("SELECT COUNT(*) FROM AiKnowledgeFile").toInt(),
        changed = changed
    )

    private fun seedRealFoundationContent() {
        val lessons = listOf(
            FoundationLesson("T01-L01", "T01-M01-O01", "What AI can and cannot do", 18, LessonContent(
                objectives = listOf("Distinguish pattern-based prediction from reasoning and truth.", "Name one risk that needs human review."), prerequisites = emptyList(),
                explanation = "AI systems learn statistical patterns from examples. They can make useful predictions, but a fluent answer is not proof of correctness, fairness, or safety.",
                workedExample = "A model predicts a house price from floor area and location. It estimates from prior examples; it does not inspect the future sale price or understand a family’s circumstances.",
                guidedPractice = "Classify each task as prediction, generation, or a decision requiring human accountability.", independentPractice = "Write two acceptance checks that a human should review before an AI-generated recommendation is used.",
                hints = listOf("Ask what the system learned from.", "Separate a prediction from the consequence of acting on it.", "Consider who is harmed if the output is wrong."),
                commonMistakes = listOf("Treating a confident response as verified evidence.", "Assuming automation removes accountability."), masteryCheck = "Explain why a model’s plausible output still needs an evaluation plan.",
                accessibilityText = "No visual is required. The worked example is fully described in text.", sources = listOf("academy/source/CURRICULUM/CURRICULUM_MAP.md", "academy/source/MASTER_AGENT_PROMPT.md")
            )),
            FoundationLesson("T01-L02", "T01-M03-O01", "Decompose an AI problem before building", 22, LessonContent(
                objectives = listOf("Break a vague request into inputs, outputs, constraints, and evaluation.", "Identify missing data before selecting a model."), prerequisites = listOf("T01-M01-O01"),
                explanation = "A useful AI project begins with a testable problem statement. Define the decision, the input data, the expected output, the success metric, and the failure cases before selecting a model.",
                workedExample = "For a study-planner assistant: inputs are available hours and deadlines; output is a proposed schedule; success is on-time completion without overload; a failure case is a missing deadline.",
                guidedPractice = "Fill an input-output-constraint table for a library book recommendation task.", independentPractice = "Turn ‘build an AI for students’ into a measurable problem statement with one metric and two limitations.",
                hints = listOf("Start with the decision, not the model.", "Write what is observable.", "Add a case where the system must defer to a person."),
                commonMistakes = listOf("Choosing an algorithm before defining success.", "Calling unavailable or biased data a reliable signal."), masteryCheck = "Decompose a real request into five testable parts.",
                accessibilityText = "The decomposition table is represented as labelled text fields.", sources = listOf("academy/source/CURRICULUM/CURRICULUM_MAP.md")
            )),
            FoundationLesson("T02-L01", "T02-M01-O01", "Python values, variables, and types", 25, LessonContent(
                objectives = listOf("Create variables with clear names.", "Predict the type and result of simple Python expressions."), prerequisites = listOf("T01-M03-O01"),
                explanation = "Python variables refer to values. Common foundation types are integers, floats, strings, booleans, and lists. Clear names reduce mistakes when data moves into later AI workflows.",
                workedExample = "temperature_c = 24.5 stores a float. is_raining = False stores a boolean. A formula can convert temperature_c to Fahrenheit without changing its meaning.",
                guidedPractice = "Match five values to their Python types and correct a variable name that hides its unit.", independentPractice = "Write a short snippet that stores study hours and calculates a weekly total. Explain the type of each variable.",
                hints = listOf("Look at quotes to spot a string.", "A decimal number is usually a float.", "Include units in a variable name when they matter."),
                commonMistakes = listOf("Mixing a numeric value with its text label.", "Using a variable before assigning it."), masteryCheck = "Predict the output and types in a three-line Python program.",
                accessibilityText = "All code examples are plain text and do not rely on color to communicate meaning.", sources = listOf("academy/source/CURRICULUM/CURRICULUM_MAP.md", "academy/source/BOOKS/prompts/B01_python_foundations_for_artificial_intelligence.md")
            ))
        )
        lessons.forEach { lesson ->
            execute("INSERT OR REPLACE INTO AiLesson(id, objective_id, title, current_version, status, estimated_minutes) VALUES (?, ?, ?, 1, 'published', ?)", listOf(lesson.id, lesson.objectiveId, lesson.title, lesson.estimatedMinutes.toString()))
            execute("INSERT OR REPLACE INTO AiLessonVersion(lesson_id, version, content_json, source_path, created_at) VALUES (?, 1, ?, 'academy/source/CURRICULUM/CURRICULUM_MAP.md', ?)", listOf(lesson.id, json.encodeToString(lesson.content), now().toString()))
        }
        val problems = listOf(
            PublishedProblem("CQAI-00001", "T01-M01-O01", "multiple_choice", "novice", "Which statement best describes an AI model?", "{\"choices\":[\"A source of guaranteed truth\",\"A pattern-based system that needs evaluation\",\"A replacement for accountability\"],\"answer\":1,\"explanation\":\"Models learn patterns; a useful output still needs evaluation and accountable use.\",\"hints\":[\"Think about what examples can and cannot prove.\"]}", "1"),
            PublishedProblem("CQAI-00003", "T01-M03-O01", "multiple_choice", "novice", "What should be defined before choosing a model?", "{\"choices\":[\"A logo\",\"Inputs, output, metric, and constraints\",\"The most complex algorithm\"],\"answer\":1,\"explanation\":\"A measurable problem definition comes before model selection.\",\"hints\":[\"Start with the decision and how success will be checked.\"]}", "1"),
            PublishedProblem("CQAI-00401", "T02-M01-O01", "code_output", "beginner", "What is the type of 24.5 in Python?", "{\"answer\":\"float\",\"explanation\":\"A number with a decimal point is represented as a float.\",\"hints\":[\"Look for the decimal point.\"]}", "\"float\"")
        )
        problems.forEach { problem ->
            execute("UPDATE AiProblem SET objective_id = ?, problem_type = ?, difficulty = ?, current_version = 1, status = 'published' WHERE id = ?", listOf(problem.objectiveId, problem.type, problem.difficulty, problem.id))
            execute("INSERT OR REPLACE INTO AiProblemVersion(problem_id, version, content_json, canonical_answer_json, source_path, created_at) VALUES (?, 1, ?, ?, 'academy/source/CURRICULUM/CURRICULUM_MAP.md', ?)", listOf(problem.id, problem.contentJson, problem.answerJson, now().toString()))
            execute("INSERT OR REPLACE INTO AiProblemTest(id, problem_id, visibility, input_json, expected_output_json, weight) VALUES (?, ?, 'public', NULL, ?, 1)", listOf("${problem.id}:public", problem.id, problem.answerJson))
        }
    }

    private fun seedBooksAndKnowledge() {
        books.forEach { book ->
            execute("INSERT OR REPLACE INTO AiBook(id, title, current_version, status, source_path) VALUES (?, ?, 1, 'published', ?)", listOf(book.id, book.title, book.sourcePath))
            execute("INSERT OR REPLACE INTO AiBookSection(id, book_id, title, position, content_path, estimated_pages) VALUES (?, ?, '100-page production plan', 1, ?, 100)", listOf("${book.id}:plan", book.id, book.pagePlanPath))
        }
        knowledge.forEach { file -> execute("INSERT OR REPLACE INTO AiKnowledgeFile(id, title, summary, current_version, status, source_path, required) VALUES (?, ?, ?, 1, 'published', ?, 0)", listOf(file.id, file.title, file.summary, file.sourcePath)) }
    }

    private fun rebuildSearchIndex() {
        driver.execute(null, "DELETE FROM AcademySearch", 0)
        execute("INSERT INTO AcademySearch(content_type, content_id, title, body) SELECT 'lesson', AiLesson.id, AiLesson.title, AiLessonVersion.content_json FROM AiLesson JOIN AiLessonVersion ON AiLesson.id = AiLessonVersion.lesson_id AND AiLesson.current_version = AiLessonVersion.version", emptyList())
        execute("INSERT INTO AcademySearch(content_type, content_id, title, body) SELECT 'book', id, title, source_path FROM AiBook", emptyList())
        execute("INSERT INTO AcademySearch(content_type, content_id, title, body) SELECT 'knowledge', id, title, summary FROM AiKnowledgeFile", emptyList())
    }

    private fun execute(sql: String, values: List<String>) {
        driver.execute(null, sql, values.size) { values.forEachIndexed { index, value -> bindString(index, value) } }
    }

    private fun longQuery(sql: String): Long = driver.executeQuery(null, sql, { QueryResult.Value(if (it.next().value) it.getLong(0) ?: 0 else 0) }, 0).value
    private fun stringQuery(sql: String, value: String? = null): String? = if (value == null) {
        driver.executeQuery(null, sql, { QueryResult.Value(if (it.next().value) it.getString(0) else null) }, 0).value
    } else {
        driver.executeQuery(null, sql, { QueryResult.Value(if (it.next().value) it.getString(0) else null) }, 1) { bindString(0, value) }.value
    }
    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
    private fun moduleId(trackId: String, title: String) = "$trackId-${title.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')}"
    private fun parseManifestRow(line: String): ManifestRow {
        val fields = line.split(',')
        require(fields.size == 9) { "Invalid local manifest row: $line" }
        return ManifestRow(fields[0], fields[1], fields[2], fields[3], fields[4], fields[5], fields[6])
    }

    private data class ManifestRow(val problemId: String, val trackId: String, val trackTitle: String, val moduleTitle: String, val objectiveId: String, val difficulty: String, val problemType: String)
    private data class FoundationLesson(val id: String, val objectiveId: String, val title: String, val estimatedMinutes: Int, val content: LessonContent)
    private data class PublishedProblem(val id: String, val objectiveId: String, val type: String, val difficulty: String, val title: String, val contentJson: String, val answerJson: String)
    private data class BookSeed(val id: String, val title: String, val sourcePath: String, val pagePlanPath: String)
    private data class KnowledgeSeed(val id: String, val title: String, val summary: String, val sourcePath: String)

    private val books = listOf(
        BookSeed("B01", "Python Foundations for Artificial Intelligence", "academy/source/BOOKS/blueprint_pdfs/B01_python_foundations_for_artificial_intelligence_100_page_blueprint.pdf", "academy/source/BOOKS/page_plans/B01_100_page_plan.csv"),
        BookSeed("B02", "Mathematics for Machine Learning, Visually and Practically", "academy/source/BOOKS/blueprint_pdfs/B02_mathematics_for_machine_learning_visually_and_practically_100_page_blueprint.pdf", "academy/source/BOOKS/page_plans/B02_100_page_plan.csv"),
        BookSeed("B03", "Algorithms, Data Structures, and AI Problem Solving", "academy/source/BOOKS/blueprint_pdfs/B03_algorithms_data_structures_and_ai_problem_solving_100_page_blueprint.pdf", "academy/source/BOOKS/page_plans/B03_100_page_plan.csv"),
        BookSeed("B04", "Machine Learning from First Principles", "academy/source/BOOKS/blueprint_pdfs/B04_machine_learning_from_first_principles_100_page_blueprint.pdf", "academy/source/BOOKS/page_plans/B04_100_page_plan.csv"),
        BookSeed("B05", "Deep Learning, Generative AI, and Responsible Deployment", "academy/source/BOOKS/blueprint_pdfs/B05_deep_learning_generative_ai_and_responsible_deployment_100_page_blueprint.pdf", "academy/source/BOOKS/page_plans/B05_100_page_plan.csv")
    )

    private val knowledge = listOf(
        "D01|Advanced Python Patterns|Iterators, generators, decorators, context managers, typing, async concepts, profiling, and package design.",
        "D02|NumPy for High-Performance Numerical Work|Shapes, broadcasting, vectorization, memory layout, numerical stability, and benchmarking.",
        "D03|Pandas for Real Data|Indexing, joins, reshaping, time data, missing values, performance, and reproducible cleaning.",
        "D04|SQL for Data and AI|Relational modeling, joins, windows, CTEs, query plans, feature tables, and leakage prevention.",
        "D05|Linear Algebra Deep Dive|Vector spaces, bases, projections, decompositions, eigen concepts, SVD, and ML applications.",
        "D06|Probability and Statistics Deep Dive|Distributions, expectation, Bayesian reasoning, estimation, confidence, hypothesis tests, and calibration.",
        "D07|Calculus and Optimization Deep Dive|Multivariable derivatives, chain rule, constrained optimization, convexity, optimizers, and diagnostics.",
        "D08|Data Structures in Production|Implementation tradeoffs, caching, indexes, trees, graphs, approximate search, and memory behavior.",
        "D09|Algorithm Design Patterns|Divide-and-conquer, greedy proofs, dynamic programming, graph search, backtracking, randomized and approximation methods.",
        "D10|Exploratory Analysis and Data Visualization|Question-driven EDA, visual encodings, uncertainty, accessibility, dashboards, and misleading-chart prevention.",
        "D11|Data Preprocessing and Feature Engineering|Splits, leakage, encoding, scaling, missingness, outliers, feature selection, and pipelines.",
        "D12|Supervised Learning Deep Dive|Loss functions, bias-variance, regularization, linear models, trees, kernels, ensembles, and diagnostics.",
        "D13|Unsupervised Learning Deep Dive|Clustering, embeddings, manifold methods, anomaly detection, validation, and interpretation.",
        "D14|Time Series Forecasting|Temporal splits, baselines, stationarity, features, classical models, deep models, uncertainty, and drift.",
        "D15|Deep Learning with PyTorch|Tensors, autograd, modules, data loaders, training loops, mixed precision concepts, checkpoints, and debugging.",
        "D16|Computer Vision Systems|Image pipelines, CNNs, augmentation, transfer learning, detection, segmentation, evaluation, and deployment constraints.",
        "D17|Natural Language Processing|Tokenization, classical features, embeddings, sequence labeling, evaluation, multilingual issues, and error analysis.",
        "D18|Transformers, LLMs, and Retrieval|Attention, transformers, embeddings, RAG, tool use, structured output, evaluation, hallucination controls, and cost.",
        "D19|Reinforcement Learning|Bandits, MDPs, value methods, policy methods, offline concerns, reward design, simulation, and safe evaluation.",
        "D20|MLOps and Responsible AI Deployment|Versioning, tests, serving, monitoring, drift, privacy, security, fairness, model cards, incident response, and governance."
    ).map { row ->
        val (id, title, summary) = row.split('|', limit = 3)
        KnowledgeSeed(id, title, summary, "academy/source/DEEP_DIVES/prompts/${id}_${title.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_')}.md")
    }

    private val schemaStatements = listOf(
        "CREATE TABLE IF NOT EXISTS AcademyContentPack (pack_id TEXT PRIMARY KEY, version TEXT NOT NULL, source_hash TEXT NOT NULL, installed_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiTrack (id TEXT PRIMARY KEY, title TEXT NOT NULL, position INTEGER NOT NULL, problem_slots INTEGER NOT NULL, status TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiModule (id TEXT PRIMARY KEY, track_id TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, status TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiObjective (id TEXT PRIMARY KEY, module_id TEXT NOT NULL, title TEXT NOT NULL, level TEXT NOT NULL, prerequisite_ids_json TEXT NOT NULL DEFAULT '[]')",
        "CREATE TABLE IF NOT EXISTS AiLesson (id TEXT PRIMARY KEY, objective_id TEXT NOT NULL, title TEXT NOT NULL, current_version INTEGER NOT NULL, status TEXT NOT NULL, estimated_minutes INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiLessonVersion (lesson_id TEXT NOT NULL, version INTEGER NOT NULL, content_json TEXT NOT NULL, source_path TEXT NOT NULL, created_at INTEGER NOT NULL, PRIMARY KEY (lesson_id, version))",
        "CREATE TABLE IF NOT EXISTS AiProblem (id TEXT PRIMARY KEY, objective_id TEXT NOT NULL, track_id TEXT NOT NULL, module_title TEXT NOT NULL, problem_type TEXT NOT NULL, difficulty TEXT NOT NULL, current_version INTEGER NOT NULL, status TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiProblemVersion (problem_id TEXT NOT NULL, version INTEGER NOT NULL, content_json TEXT NOT NULL, canonical_answer_json TEXT, source_path TEXT NOT NULL, created_at INTEGER NOT NULL, PRIMARY KEY (problem_id, version))",
        "CREATE TABLE IF NOT EXISTS AiProblemTest (id TEXT PRIMARY KEY, problem_id TEXT NOT NULL, visibility TEXT NOT NULL, input_json TEXT, expected_output_json TEXT, weight REAL NOT NULL DEFAULT 1)",
        "CREATE TABLE IF NOT EXISTS AiAttempt (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, problem_id TEXT NOT NULL, answer_json TEXT NOT NULL, correct INTEGER, hints_used INTEGER NOT NULL DEFAULT 0, execution_json TEXT, created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiMastery (user_id TEXT NOT NULL, objective_id TEXT NOT NULL, score REAL NOT NULL DEFAULT 0, evidence_count INTEGER NOT NULL DEFAULT 0, last_evidence_at INTEGER, next_review_at INTEGER, PRIMARY KEY (user_id, objective_id))",
        "CREATE TABLE IF NOT EXISTS AiReviewQueue (user_id TEXT NOT NULL, objective_id TEXT NOT NULL, due_at INTEGER NOT NULL, reason TEXT NOT NULL, PRIMARY KEY (user_id, objective_id))",
        "CREATE TABLE IF NOT EXISTS AiMistakeNotebook (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, problem_id TEXT NOT NULL, misconception_tag TEXT, note TEXT NOT NULL, created_at INTEGER NOT NULL, resolved_at INTEGER)",
        "CREATE TABLE IF NOT EXISTS AiProject (id TEXT PRIMARY KEY, track_id TEXT NOT NULL, title TEXT NOT NULL, rubric_json TEXT NOT NULL, status TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiProjectSubmission (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, project_id TEXT NOT NULL, artifact_json TEXT NOT NULL, reflection TEXT NOT NULL, rubric_result_json TEXT, submitted_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiBook (id TEXT PRIMARY KEY, title TEXT NOT NULL, current_version INTEGER NOT NULL, status TEXT NOT NULL, source_path TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiBookSection (id TEXT PRIMARY KEY, book_id TEXT NOT NULL, title TEXT NOT NULL, position INTEGER NOT NULL, content_path TEXT NOT NULL, estimated_pages INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiBookmark (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, content_type TEXT NOT NULL, content_id TEXT NOT NULL, location TEXT NOT NULL, created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiHighlight (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, content_type TEXT NOT NULL, content_id TEXT NOT NULL, quote TEXT NOT NULL, color TEXT NOT NULL, created_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiNote (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, content_type TEXT NOT NULL, content_id TEXT NOT NULL, body TEXT NOT NULL, updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS AiKnowledgeFile (id TEXT PRIMARY KEY, title TEXT NOT NULL, summary TEXT NOT NULL, current_version INTEGER NOT NULL, status TEXT NOT NULL, source_path TEXT NOT NULL, required INTEGER NOT NULL DEFAULT 0)",
        "CREATE TABLE IF NOT EXISTS LocalAnalyticsEvent (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, event_type TEXT NOT NULL, content_id TEXT, payload_json TEXT NOT NULL, occurred_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS LocalBackupRecord (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, file_name TEXT NOT NULL, checksum TEXT NOT NULL, created_at INTEGER NOT NULL, imported_at INTEGER)",
        "CREATE TABLE IF NOT EXISTS LegacyContentMapping (legacy_content_type TEXT NOT NULL, legacy_content_id TEXT NOT NULL, academy_content_type TEXT NOT NULL, academy_content_id TEXT NOT NULL, migration_version TEXT NOT NULL, created_at INTEGER NOT NULL, PRIMARY KEY (legacy_content_type, legacy_content_id))"
    )
}
