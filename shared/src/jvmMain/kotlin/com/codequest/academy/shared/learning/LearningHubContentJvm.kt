package com.codequest.academy.shared.learning

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json
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

actual object LearningHubContent {
    private val mutableState = MutableStateFlow(LearningHubContentState())
    actual val state: StateFlow<LearningHubContentState> = mutableState
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }
    private var root: File? = null
    private var curriculum: LearningHubCurriculum? = null
    private var problemsByLesson: Map<String, List<LearningHubProblem>> = emptyMap()

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
            mutableState.value = LearningHubContentState(false, curriculum, packageRoot.absolutePath, null)
        }.onFailure { error ->
            mutableState.value = LearningHubContentState(false, null, null, error.message ?: "Learning Hub package could not be loaded")
        }
    }

    @Synchronized
    actual fun installPackage(packagePath: String, databasePath: String): Boolean = runCatching {
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
    actual fun answerKey(lesson: LearningHubLesson): String = root?.let { File(it, lesson.answerPath).readText() } ?: ""
    actual fun firstProblems(lesson: LearningHubLesson, limit: Int): List<LearningHubProblem> = problemsByLesson[lesson.id].orEmpty().take(limit)
    actual fun sectionPdfPath(section: LearningHubSection): String? = root?.let { base -> File(base, "practice_sheets").listFiles()?.firstOrNull { file -> file.name.startsWith(section.id, ignoreCase = true) && file.extension.equals("pdf", true) }?.absolutePath }
    actual fun openSectionPdf(section: LearningHubSection): Boolean = runCatching { sectionPdfPath(section)?.let { java.awt.Desktop.getDesktop().open(File(it)); true } ?: false }.getOrDefault(false)
    actual fun saveSectionPdf(section: LearningHubSection, destinationPath: String): Boolean = runCatching {
        val source = sectionPdfPath(section)?.let(::File) ?: return false
        val destination = if (File(destinationPath).isAbsolute) File(destinationPath) else File(File(System.getProperty("user.home"), "Downloads"), destinationPath)
        destination.parentFile?.mkdirs(); source.copyTo(destination, overwrite = true); true
    }.getOrDefault(false)
}
