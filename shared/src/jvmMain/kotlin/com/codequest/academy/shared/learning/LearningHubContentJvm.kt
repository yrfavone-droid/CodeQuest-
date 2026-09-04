package com.codequest.academy.shared.learning

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.*
import java.io.File
import java.io.InputStream
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import java.sql.DriverManager
import java.util.UUID
import java.util.zip.ZipInputStream

private const val BUNDLED_PACKAGE = "/learning_hub/Nous_AI_Academy_Learning_Hub_v1.zip"
private const val BUNDLED_SECTION1_PACKAGE = "/learning_hub/Nous_AI_Academy_Section_01_Deep_Unit_v2.zip"

@kotlinx.serialization.Serializable
private data class Section1BlocksFile(val blocks: List<Section1ArticleBlock> = emptyList())

@kotlinx.serialization.Serializable
private data class Section1Manifest(
    val format: String,
    val version: String,
    val brand: String,
    @kotlinx.serialization.SerialName("section_id") val sectionId: String,
    @kotlinx.serialization.SerialName("lesson_count") val lessonCount: Int,
    @kotlinx.serialization.SerialName("unit_count") val unitCount: Int,
    @kotlinx.serialization.SerialName("practice_count") val practiceCount: Int,
    @kotlinx.serialization.SerialName("quiz_count") val quizCount: Int,
    @kotlinx.serialization.SerialName("problem_count") val problemCount: Int,
    @kotlinx.serialization.SerialName("pdf_count") val pdfCount: Int,
    val lessons: List<Section1LessonMeta>
)

actual object LearningHubContent {
    private val mutableState = MutableStateFlow(LearningHubContentState())
    actual val state: StateFlow<LearningHubContentState> = mutableState
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var root: File? = null
    private var curriculum: LearningHubCurriculum? = null
    private var problemsByLesson: Map<String, List<LearningHubProblem>> = emptyMap()
    private var section1Root: File? = null
    private var section1Lessons: Map<String, Section1LessonMeta> = emptyMap()
    private var section1Blocks: Map<String, List<Section1ArticleBlock>> = emptyMap()
    private var section1Practice: Map<String, List<LearningHubProblem>> = emptyMap()
    private var section1Quiz: Map<String, List<LearningHubProblem>> = emptyMap()
    private var section1Glossary: List<Section1GlossaryEntry> = emptyList()

    @Synchronized
    actual fun initialize(databasePath: String) {
        if (curriculum != null) return
        runCatching {
            val storage = File(System.getProperty("user.home"), ".nous-ai-academy/learning-hub")
            storage.mkdirs()
            val pointer = File(storage, "active-version")
            val activeName = pointer.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
            val active = activeName?.let { File(storage, "versions/$it") }
            val packageRoot = if (active?.isDirectory == true) {
                runCatching { verifyChecksums(active); active }.getOrElse {
                    val previousName = File(storage, "previous-version").takeIf { it.isFile }?.readText()?.trim()
                    val previous = previousName?.let { File(storage, "versions/$it") }
                    if (previous?.isDirectory == true) {
                        verifyChecksums(previous)
                        File(storage, "active-version.tmp").writeText(previousName)
                        moveAtomic(File(storage, "active-version.tmp").toPath(), pointer.toPath())
                        previous
                    } else throw it
                }
            } else installBundled(storage, databasePath)
            load(packageRoot)
            root = packageRoot
            loadSection1Override(storage, databasePath)
            mutableState.value = LearningHubContentState(false, curriculum, packageRoot.absolutePath, null)
        }.onFailure { error ->
            mutableState.value = LearningHubContentState(false, null, null, error.message ?: "Learning Hub package could not be loaded")
        }
    }

    @Synchronized
    actual fun installPackage(packagePath: String, databasePath: String): Boolean = runCatching {
        val archive = File(packagePath)
        if (archive.isFile && archive.name.contains("Section_01", ignoreCase = true)) {
            val storage = File(System.getProperty("user.home"), ".nous-ai-academy/learning-hub").apply { mkdirs() }
            installSection1Archive(archive, storage, databasePath)
            loadSection1Override(storage, databasePath)
            return@runCatching true
        }
        val storage = File(System.getProperty("user.home"), ".nous-ai-academy/learning-hub").apply { mkdirs() }
        val versionDir = installArchive(File(packagePath), storage, databasePath)
        load(versionDir)
        root = versionDir
        mutableState.value = LearningHubContentState(false, curriculum, versionDir.absolutePath, null)
        true
    }.getOrElse { false }

    private fun installBundled(storage: File, databasePath: String): File {
        val staging = File(storage, "staging/${UUID.randomUUID()}").apply { mkdirs() }
        val archive = File(staging, "package.zip")
        val stream = requireNotNull(LearningHubContent::class.java.getResourceAsStream(BUNDLED_PACKAGE)) { "Bundled Learning Hub package is missing" }
        stream.use { input -> archive.outputStream().use { input.copyTo(it) } }
        val versionDir = installArchive(archive, storage, databasePath, staging)
        staging.deleteRecursively()
        return versionDir
    }

    private fun loadSection1Override(storage: File, databasePath: String) {
        val pointer = File(storage, "section-S01-active")
        val activeName = pointer.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }
        val active = activeName?.let { File(storage, "section-S01-versions/$it") }
        val packageRoot = if (active?.isDirectory == true) {
            runCatching { verifyChecksums(active); active }.getOrElse { installBundledSection1(storage, databasePath) }
        } else installBundledSection1(storage, databasePath)
        parseSection1(packageRoot)
        section1Root = packageRoot
    }

    private fun installBundledSection1(storage: File, databasePath: String): File {
        val staging = File(storage, "section-staging/${UUID.randomUUID()}").apply { mkdirs() }
        val archive = File(staging, "section.zip")
        val stream = requireNotNull(LearningHubContent::class.java.getResourceAsStream(BUNDLED_SECTION1_PACKAGE)) { "Bundled Section 1 package is missing" }
        stream.use { input -> archive.outputStream().use { input.copyTo(it) } }
        return installSection1Archive(archive, storage, databasePath, staging)
    }

    private fun installSection1Archive(archive: File, storage: File, databasePath: String, existingStaging: File? = null): File {
        val staging = existingStaging ?: File(storage, "section-staging/${UUID.randomUUID()}").apply { mkdirs() }
        val extracted = File(staging, "content").apply { mkdirs() }
        extractZip(archive.toPath(), extracted.toPath())
        val packageRoot = File(extracted, "section_manifest.json").takeIf { it.isFile }?.let { extracted }
            ?: extracted.listFiles()?.singleOrNull { it.isDirectory && File(it, "section_manifest.json").isFile }
            ?: error("Section package must contain section_manifest.json at its root")
        verifyChecksums(packageRoot)
        val manifest = parseAndValidateSection1(packageRoot)
        val sourceHash = sha256(archive)
        val versionKey = "${manifest.version}-${sourceHash.take(12)}"
        val versionDir = File(storage, "section-S01-versions/$versionKey")
        if (!versionDir.exists()) {
            versionDir.parentFile.mkdirs()
            moveAtomic(packageRoot.toPath(), versionDir.toPath())
        }
        installSection1Metadata(databasePath, manifest, sourceHash, versionKey)
        val pointer = File(storage, "section-S01-active")
        pointer.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }?.let { File(storage, "section-S01-previous").writeText(it) }
        val tempPointer = File(storage, "section-S01-active.$versionKey.tmp")
        tempPointer.writeText(versionKey)
        moveAtomic(tempPointer.toPath(), pointer.toPath())
        if (existingStaging != null) staging.deleteRecursively()
        return versionDir
    }

    private fun installSection1Metadata(databasePath: String, manifest: Section1Manifest, sourceHash: String, versionKey: String) {
        DriverManager.getConnection("jdbc:sqlite:${File(databasePath).absolutePath.replace("\\", "/")}").use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS LearningHubSectionOverride(section_id TEXT PRIMARY KEY, version TEXT NOT NULL, source_hash TEXT NOT NULL, installed_at INTEGER NOT NULL)")
                }
                connection.prepareStatement("INSERT OR REPLACE INTO LearningHubSectionOverride(section_id, version, source_hash, installed_at) VALUES ('S01', ?, ?, ?)").use { statement ->
                    statement.setString(1, versionKey); statement.setString(2, sourceHash); statement.setLong(3, System.currentTimeMillis()); statement.executeUpdate()
                }
                connection.commit()
            } catch (failure: Throwable) { connection.rollback(); throw failure }
        }
    }

    private fun parseAndValidateSection1(packageRoot: File): Section1Manifest {
        val manifest = json.decodeFromString<Section1Manifest>(File(packageRoot, "section_manifest.json").readText())
        check(manifest.format == "nous-learning-hub-section-replacement" && manifest.brand == "Nous AI Academy" && manifest.sectionId == "S01") { "Section 1 identity contract failed" }
        check(manifest.lessonCount == 5 && manifest.lessons.size == 5 && manifest.unitCount == 40 && manifest.practiceCount == 400 && manifest.quizCount == 100 && manifest.problemCount == 500 && manifest.pdfCount == 6) { "Section 1 counts are invalid" }
        check(manifest.lessons.map { it.id } == (1..5).map { "S01-L${it.toString().padStart(2, '0')}" }) { "Section 1 lesson IDs are invalid" }
        manifest.lessons.forEach { lesson ->
            check(lesson.unitCount == 8 && lesson.practiceCount == 80 && lesson.quizCount == 20) { "Invalid counts for ${lesson.id}" }
            check(File(packageRoot, lesson.articlePath).isFile && File(packageRoot, lesson.notesPath).isFile && File(packageRoot, lesson.quickSheetPath).isFile && File(packageRoot, lesson.answersPath).isFile && File(packageRoot, lesson.pdfPath).isFile) { "Missing Section 1 resource for ${lesson.id}" }
            val blocks = json.decodeFromString<Section1BlocksFile>(File(packageRoot, "content/${lesson.id}/article_blocks.json").readText()).blocks
            check(blocks.count { it.type == "heading" } == 8 && blocks.count { it.type == "knowledge_check" } == 8) { "Expected eight article units for ${lesson.id}" }
        }
        val ids = mutableSetOf<String>(); var practiceCount = 0; var quizCount = 0
        manifest.lessons.forEach { lesson ->
            File(packageRoot, "content/${lesson.id}/practice_80.jsonl").forEachLine { line -> if (line.isNotBlank()) { val id = json.parseToJsonElement(line).jsonObject["id"]?.jsonPrimitive?.content ?: error("Problem ID missing"); check(id.startsWith("NAA-01-")); check(ids.add(id)); practiceCount++ } }
            json.parseToJsonElement(File(packageRoot, "content/${lesson.id}/quiz_20.json").readText()).jsonArray.forEach { element -> val id = element.jsonObject["id"]?.jsonPrimitive?.content ?: error("Quiz ID missing"); check(id.startsWith("NAA-01-")); check(ids.add(id)); quizCount++ }
        }
        check(practiceCount == 400 && quizCount == 100 && ids.size == 500) { "Section 1 problem IDs/counts are invalid" }
        return manifest
    }

    private fun parseSection1(packageRoot: File) {
        val manifest = json.decodeFromString<Section1Manifest>(File(packageRoot, "section_manifest.json").readText())
        section1Lessons = manifest.lessons.associateBy { it.id }
        section1Blocks = manifest.lessons.associate { lesson -> lesson.id to json.decodeFromString<Section1BlocksFile>(File(packageRoot, "content/${lesson.id}/article_blocks.json").readText()).blocks }
        section1Practice = manifest.lessons.associate { lesson -> lesson.id to readSection1Problems(File(packageRoot, "content/${lesson.id}/practice_80.jsonl")) }
        section1Quiz = manifest.lessons.associate { lesson -> lesson.id to readSection1Problems(File(packageRoot, "content/${lesson.id}/quiz_20.json"), array = true) }
        val glossary = json.parseToJsonElement(File(packageRoot, "content/section1_glossary.json").readText()).jsonObject
        section1Glossary = glossary.entries.map { (term, definition) -> Section1GlossaryEntry(term, definition.jsonPrimitive.content) }
    }

    private fun readSection1Problems(file: File, array: Boolean = false): List<LearningHubProblem> {
        val elements = if (array) json.parseToJsonElement(file.readText()).jsonArray else file.readLines().filter { it.isNotBlank() }.map { json.parseToJsonElement(it) }
        return elements.map { element ->
            val obj = element.jsonObject
            LearningHubProblem(id = obj.getValue("id").jsonPrimitive.content, sectionId = "S01", lessonId = obj.getValue("lesson_id").jsonPrimitive.content, sequence = obj["sequence"]?.jsonPrimitive?.int, difficulty = obj["difficulty"]?.jsonPrimitive?.content, mode = "section1", context = obj["scenario"]?.jsonPrimitive?.content, prompt = obj.getValue("prompt").jsonPrimitive.content, answerType = obj.getValue("type").jsonPrimitive.content, answer = obj.getValue("answer"), explanation = obj.getValue("explanation").jsonPrimitive.content, options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty())
        }
    }

    private fun installArchive(archive: File, storage: File, databasePath: String, existingStaging: File? = null): File {
        val staging = existingStaging ?: File(storage, "staging/${UUID.randomUUID()}").apply { mkdirs() }
        val extracted = File(staging, "content").apply { mkdirs() }
        extractZip(archive.toPath(), extracted.toPath())
        val packageRoot = File(extracted, "curriculum.json").takeIf { it.isFile }
            ?.let { extracted }
            ?: extracted.listFiles()?.singleOrNull { it.isDirectory && File(it, "curriculum.json").isFile }
            ?: error("Package must contain curriculum.json at its root")
        verifyChecksums(packageRoot)
        val manifest = parseAndValidate(packageRoot)
        val sourceHash = sha256(archive)
        val versionKey = "${manifest.version}-${sourceHash.take(12)}"
        val versionDir = File(storage, "versions/$versionKey")
        if (!versionDir.exists()) {
            versionDir.parentFile.mkdirs()
            moveAtomic(packageRoot.toPath(), versionDir.toPath())
        }
        // Content metadata is committed independently from the pointer. Learner
        // tables are never touched, and the pointer changes only after commit.
        installMetadata(databasePath, manifest, sourceHash, versionKey)
        val pointer = File(storage, "active-version")
        pointer.takeIf { it.isFile }?.readText()?.trim()?.takeIf { it.isNotEmpty() }?.let { File(storage, "previous-version").writeText(it) }
        val tempPointer = File(storage, "active-version.$versionKey.tmp")
        tempPointer.writeText(versionKey)
        moveAtomic(tempPointer.toPath(), pointer.toPath())
        return versionDir
    }

    private fun installMetadata(databasePath: String, manifest: LearningHubCurriculum, sourceHash: String, versionKey: String) {
        DriverManager.getConnection("jdbc:sqlite:${File(databasePath).absolutePath.replace("\\", "/")}").use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS LearningHubContentVersion (version TEXT PRIMARY KEY, source_hash TEXT NOT NULL, section_count INTEGER NOT NULL, lesson_count INTEGER NOT NULL, problem_count INTEGER NOT NULL, installed_at INTEGER NOT NULL)")
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS LearningHubActivePointer (singleton INTEGER PRIMARY KEY CHECK(singleton = 1), version TEXT NOT NULL)")
                }
                connection.prepareStatement("INSERT OR REPLACE INTO LearningHubContentVersion(version, source_hash, section_count, lesson_count, problem_count, installed_at) VALUES (?, ?, ?, ?, ?, ?)").use { statement ->
                    statement.setString(1, manifest.version); statement.setString(2, sourceHash)
                    statement.setInt(3, manifest.sectionCount); statement.setInt(4, manifest.lessonCount); statement.setInt(5, manifest.problemCount); statement.setLong(6, System.currentTimeMillis()); statement.executeUpdate()
                }
                connection.prepareStatement("INSERT OR REPLACE INTO LearningHubActivePointer(singleton, version) VALUES (1, ?)").use { statement ->
                    statement.setString(1, versionKey); statement.executeUpdate()
                }
                connection.commit()
            } catch (failure: Throwable) {
                connection.rollback()
                throw failure
            }
        }
    }

    private fun load(packageRoot: File) {
        val manifest = parseAndValidate(packageRoot)
        val problemFile = File(packageRoot, "problems/problem_bank_10000.jsonl")
        val grouped = buildMap<String, MutableList<LearningHubProblem>> {
            problemFile.forEachLine { line ->
                if (line.isBlank()) return@forEachLine
                val problem = json.decodeFromString<LearningHubProblem>(line)
                getOrPut(problem.lessonId) { mutableListOf() }.add(problem)
            }
        }
        check(grouped.values.sumOf { it.size } == manifest.problemCount) { "Problem bank count does not match curriculum" }
        curriculum = manifest
        problemsByLesson = grouped.mapValues { (_, values) -> values.sortedBy { it.sequence ?: Int.MAX_VALUE } }
    }

    private fun parseAndValidate(root: File): LearningHubCurriculum {
        val manifest = json.decodeFromString<LearningHubCurriculum>(File(root, "curriculum.json").readText())
        check(manifest.format == "nous-learning-hub") { "Unsupported curriculum format" }
        check(manifest.version.startsWith("1.")) { "Unsupported curriculum version ${manifest.version}" }
        check(manifest.brand == "Nous AI Academy" && manifest.language == "en" && manifest.offline) { "Curriculum identity/offline contract failed" }
        check(manifest.sectionCount == 20 && manifest.sections.size == 20) { "Expected exactly 20 sections" }
        check(manifest.lessonCount == 100 && manifest.sections.sumOf { it.lessons.size } == 100) { "Expected exactly 100 lessons" }
        check(manifest.sections.map { it.id }.toSet().size == 20) { "Section IDs are not unique" }
        check(manifest.sections.map { it.position } == (1..20).toList()) { "Section ordering is invalid" }
        val lessons = manifest.sections.flatMap { section ->
            check(section.lessonCount == 5 && section.lessons.size == 5) { "Section ${section.id} must contain five lessons" }
            check(section.lessons.map { it.position } == (1..5).toList()) { "Lesson ordering is invalid in ${section.id}" }
            section.lessons.onEach { lesson ->
                check(lesson.sectionId == section.id) { "Lesson ${lesson.id} has wrong section" }
                check(lesson.problemCount == 100) { "Lesson ${lesson.id} must contain 100 problems" }
                check(File(root, lesson.contentPath).isFile && File(root, lesson.answerPath).isFile) { "Missing lesson source for ${lesson.id}" }
            }
        }
        check(lessons.map { it.id }.toSet().size == 100) { "Lesson IDs are not unique" }
        return manifest
    }

    private fun verifyChecksums(root: File) {
        val checksumFile = File(root, "checksums.sha256")
        check(checksumFile.isFile) { "checksums.sha256 is missing" }
        checksumFile.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val match = Regex("^([0-9a-fA-F]{64})\\s+(.+)$").matchEntire(line.trim()) ?: error("Invalid checksum entry")
            val expected = match.groupValues[1].lowercase()
            val relative = match.groupValues[2].trim().removePrefix("*")
            val file = root.toPath().resolve(relative.replace('/', File.separatorChar)).normalize().toFile()
            check(file.toPath().startsWith(root.toPath())) { "Checksum path escapes package" }
            check(file.isFile) { "Checksum target is missing: $relative" }
            check(sha256(file) == expected) { "Checksum mismatch: $relative" }
        }
    }

    private fun extractZip(archive: Path, destination: Path) {
        ZipInputStream(Files.newInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val target = destination.resolve(entry.name).normalize()
                check(target.startsWith(destination)) { "Archive entry escapes staging directory" }
                if (entry.isDirectory) Files.createDirectories(target) else {
                    Files.createDirectories(target.parent)
                    Files.newOutputStream(target).use { zip.copyTo(it) }
                }
            }
        }
    }

    private fun moveAtomic(source: Path, target: Path) {
        try { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE) }
        catch (_: AtomicMoveNotSupportedException) { Files.move(source, target, StandardCopyOption.REPLACE_EXISTING) }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    actual fun lessonMarkdown(lesson: LearningHubLesson): String = root?.let { File(it, lesson.contentPath).readText() } ?: ""
    actual fun lessonReview(lesson: LearningHubLesson): String = """
        # Detailed Summary & Review

        ## Learning objective

        ${lesson.objective}

        ## Core principle

        ${lesson.principle}

        ## Worked example

        ${lesson.workedExample}

        ## Applied review

        ${lesson.lab}

        ## Key points

        - Explain the core principle in your own words before using a tool or formula.
        - Identify the input, transformation, expected output, and a concrete verification check.
        - Separate what the method guarantees from assumptions about data, scale, and context.
        - Test a normal case, a boundary case, and a failure case before trusting the result.
        - Preserve evidence: record the expected result, observed result, and reason for any difference.

        ## Quick self-review

        1. What problem does **${lesson.title}** solve?
        2. Which assumption matters most, and how would you test it?
        3. Reproduce the worked example without notes and explain every step.
        4. Name one common shortcut that could produce a plausible but incorrect result.
        5. Apply the idea to the guided task: ${lesson.lab}
    """.trimIndent()
    actual fun answerKey(lesson: LearningHubLesson): String = root?.let { File(it, lesson.answerPath).readText() } ?: ""
    actual fun firstProblems(lesson: LearningHubLesson, limit: Int): List<LearningHubProblem> = problemsByLesson[lesson.id].orEmpty().take(limit)
    actual fun lessonQuiz(lesson: LearningHubLesson, limit: Int): List<LearningHubProblem> =
        problemsByLesson[lesson.id].orEmpty().filter { problem ->
            problem.answerType == "multiple_choice" &&
                problem.options.size >= 2 &&
                runCatching { problem.answer.jsonPrimitive.int in problem.options.indices }.getOrDefault(false)
        }.take(limit)
    actual fun sectionPdfPath(section: LearningHubSection): String? = root?.let { base -> File(base, "practice_sheets").listFiles()?.firstOrNull { file -> file.name.startsWith(section.id, ignoreCase = true) && file.extension.equals("pdf", true) }?.absolutePath }
    actual fun openSectionPdf(section: LearningHubSection): Boolean = runCatching { sectionPdfPath(section)?.let { java.awt.Desktop.getDesktop().open(File(it)); true } ?: false }.getOrDefault(false)
    actual fun saveSectionPdf(section: LearningHubSection, destinationPath: String): Boolean = runCatching {
        val source = sectionPdfPath(section)?.let(::File) ?: return false
        val destination = if (File(destinationPath).isAbsolute) File(destinationPath) else File(File(System.getProperty("user.home"), "Downloads"), destinationPath)
        destination.parentFile?.mkdirs(); source.copyTo(destination, overwrite = true); true
    }.getOrDefault(false)

    actual fun section1Lesson(lessonId: String): Section1LessonMeta? = section1Lessons[lessonId]
    actual fun section1ArticleBlocks(lessonId: String): List<Section1ArticleBlock> = section1Blocks[lessonId].orEmpty()
    actual fun section1Review(lessonId: String): String = runCatching {
        val base = section1Root ?: return ""
        val meta = section1Lessons[lessonId] ?: return ""
        File(base, meta.quickSheetPath).readText()
    }.getOrDefault("")
    actual fun section1Problems(lessonId: String, quiz: Boolean, limit: Int): List<LearningHubProblem> =
        (if (quiz) section1Quiz[lessonId] else section1Practice[lessonId]).orEmpty().take(limit)
    actual fun section1Glossary(): List<Section1GlossaryEntry> = section1Glossary
    actual fun section1Note(lessonId: String): String = runCatching {
        File(System.getProperty("user.home"), ".nous-ai-academy/section1-notes/$lessonId.md").takeIf { it.isFile }?.readText().orEmpty()
    }.getOrDefault("")
    actual fun saveSection1Note(lessonId: String, note: String) {
        runCatching {
            val file = File(System.getProperty("user.home"), ".nous-ai-academy/section1-notes/$lessonId.md")
            file.parentFile.mkdirs()
            file.writeText(note)
        }
    }
    private fun section1PdfFile(lessonId: String): File? = section1Root?.let { root -> section1Lessons[lessonId]?.let { File(root, it.pdfPath) }?.takeIf(File::isFile) }
    actual fun openSection1Pdf(lessonId: String): Boolean = runCatching {
        section1PdfFile(lessonId)?.let { java.awt.Desktop.getDesktop().open(it); true } ?: false
    }.getOrDefault(false)
    actual fun saveSection1Pdf(lessonId: String, destinationPath: String): Boolean = runCatching {
        val source = section1PdfFile(lessonId) ?: return false
        val destination = if (File(destinationPath).isAbsolute) File(destinationPath) else File(File(System.getProperty("user.home"), "Downloads"), destinationPath)
        destination.parentFile?.mkdirs(); source.copyTo(destination, overwrite = true); true
    }.getOrDefault(false)
}
