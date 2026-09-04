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
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

private const val BUNDLED_PACKAGE = "/learning_hub/Nous_AI_Academy_Learning_Hub_v1.zip"
private const val BUNDLED_SECTION1_PACKAGE = "/learning_hub/Nous_AI_Academy_Section_01_Deep_Unit_v2.zip"
private const val BUNDLED_DEEP_PATCH = "/learning_hub/Nous_AI_Academy_Sections_02_to_20_Deep_Curriculum_v2.zip"
private const val DEEP_PATCH_SHA256 = "38b5880b9fc43902bd4a21702c63bf5508bc10795b7072e8f17f5d8efa464184"

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

@kotlinx.serialization.Serializable
private data class DeepArticleBlocksFile(
    @kotlinx.serialization.SerialName("lesson_id") val lessonId: String,
    val version: String,
    val pages: List<String>,
    val blocks: List<LearningHubArticleBlock>
)

@kotlinx.serialization.Serializable
private data class DeepLessonManifest(
    val id: String,
    @kotlinx.serialization.SerialName("section_id") val sectionId: String,
    val position: Int,
    val title: String,
    val objective: String,
    val principle: String,
    @kotlinx.serialization.SerialName("article_path") val articlePath: String,
    @kotlinx.serialization.SerialName("summary_path") val summaryPath: String,
    @kotlinx.serialization.SerialName("quiz_path") val quizPath: String,
    @kotlinx.serialization.SerialName("answers_path") val answersPath: String,
    @kotlinx.serialization.SerialName("pdf_path") val pdfPath: String,
    @kotlinx.serialization.SerialName("article_words") val articleWords: Int,
    @kotlinx.serialization.SerialName("summary_words") val summaryWords: Int,
    val pages: List<String>,
    @kotlinx.serialization.SerialName("unit_count") val unitCount: Int,
    @kotlinx.serialization.SerialName("practice_count") val practiceCount: Int,
    @kotlinx.serialization.SerialName("quiz_count") val quizCount: Int,
    @kotlinx.serialization.SerialName("quiz_type") val quizType: String,
    @kotlinx.serialization.SerialName("problem_id_start") val problemIdStart: String,
    @kotlinx.serialization.SerialName("problem_id_end") val problemIdEnd: String
)

@kotlinx.serialization.Serializable
private data class DeepSectionManifest(
    val id: String,
    val position: Int,
    val title: String,
    val level: String,
    val description: String,
    @kotlinx.serialization.SerialName("lesson_count") val lessonCount: Int,
    val version: String,
    @kotlinx.serialization.SerialName("unit_count") val unitCount: Int,
    @kotlinx.serialization.SerialName("practice_count") val practiceCount: Int,
    @kotlinx.serialization.SerialName("quiz_count") val quizCount: Int,
    @kotlinx.serialization.SerialName("problem_count") val problemCount: Int,
    @kotlinx.serialization.SerialName("master_pdf") val masterPdf: String,
    val lessons: List<DeepLessonManifest>
)

@kotlinx.serialization.Serializable
private data class DeepCurriculumPatch(
    val format: String,
    val version: String,
    @kotlinx.serialization.SerialName("requires_section1_version") val requiresSection1Version: String,
    val brand: String,
    val offline: Boolean,
    val scope: List<String>,
    @kotlinx.serialization.SerialName("preserve_ids") val preserveIds: Boolean,
    @kotlinx.serialization.SerialName("section_count") val sectionCount: Int,
    @kotlinx.serialization.SerialName("lesson_count") val lessonCount: Int,
    @kotlinx.serialization.SerialName("unit_count") val unitCount: Int,
    @kotlinx.serialization.SerialName("practice_count") val practiceCount: Int,
    @kotlinx.serialization.SerialName("quiz_count") val quizCount: Int,
    @kotlinx.serialization.SerialName("problem_count") val problemCount: Int,
    @kotlinx.serialization.SerialName("lesson_pdf_count") val lessonPdfCount: Int,
    @kotlinx.serialization.SerialName("section_master_pdf_count") val sectionMasterPdfCount: Int,
    @kotlinx.serialization.SerialName("pages_per_lesson") val pagesPerLesson: List<String>,
    val sections: List<DeepSectionManifest>
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
    private var section1Version: String? = null
    private var deepRoot: File? = null
    private var deepPatch: DeepCurriculumPatch? = null
    private var deepBlocks: Map<String, List<LearningHubArticleBlock>> = emptyMap()
    private var searchTextByLesson: Map<String, String> = emptyMap()
    private var databasePath: String? = null
    private var validatedDeepPath: String? = null
    private var validatedDeepPatch: DeepCurriculumPatch? = null

    @Synchronized
    actual fun initialize(databasePath: String) {
        if (curriculum != null) return
        this.databasePath = databasePath
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
            val deepPackageRoot = loadDeepPatchOverride(storage, databasePath)
            mutableState.value = LearningHubContentState(
                loading = false, curriculum = curriculum, packagePath = deepPackageRoot.absolutePath,
                curriculumVersion = deepPatch?.version, canRollback = File(storage, "sections-S02-S20-previous").isFile
            )
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
        if (archive.isFile && archive.name.contains("Sections_02_to_20", ignoreCase = true)) {
            val storage = File(System.getProperty("user.home"), ".nous-ai-academy/learning-hub").apply { mkdirs() }
            mutableState.value = mutableState.value.copy(updateMessage = "Validating local curriculum package…")
            val installed = installDeepPatchArchive(archive, storage, databasePath)
            loadDeepPatch(installed)
            mutableState.value = mutableState.value.copy(
                curriculum = curriculum, packagePath = installed.absolutePath, curriculumVersion = deepPatch?.version,
                updateMessage = "Curriculum ${deepPatch?.version} installed successfully.", canRollback = File(storage, "sections-S02-S20-previous").isFile
            )
            return@runCatching true
        }
        val storage = File(System.getProperty("user.home"), ".nous-ai-academy/learning-hub").apply { mkdirs() }
        val versionDir = installArchive(File(packagePath), storage, databasePath)
        load(versionDir)
        root = versionDir
        mutableState.value = LearningHubContentState(false, curriculum, versionDir.absolutePath, null)
        true
    }.getOrElse { error ->
        mutableState.value = mutableState.value.copy(updateMessage = "Curriculum update rejected: ${error.message}")
        false
    }

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

    private fun loadDeepPatchOverride(storage: File, databasePath: String): File {
        val pointer = File(storage, "sections-S02-S20-active")
        val activeName = pointer.takeIf { it.isFile }?.readText()?.trim()?.takeIf(String::isNotEmpty)
        val active = activeName?.let { File(storage, "sections-S02-S20-versions/$it") }
        val packageRoot = if (active?.isDirectory == true) {
            runCatching { verifyChecksums(active); parseAndValidateDeep(active); active }.getOrElse { failure ->
                val previousName = File(storage, "sections-S02-S20-previous").takeIf { it.isFile }?.readText()?.trim()
                val previous = previousName?.let { File(storage, "sections-S02-S20-versions/$it") }
                if (previous?.isDirectory == true) {
                    verifyChecksums(previous); parseAndValidateDeep(previous)
                    val temporary = File(storage, "sections-S02-S20-active.recovery.tmp").apply { writeText(previousName) }
                    moveAtomic(temporary.toPath(), pointer.toPath())
                    previous
                } else {
                    mutableState.value = mutableState.value.copy(updateMessage = "Active curriculum was invalid; restored the bundled version (${failure.message}).")
                    installBundledDeepPatch(storage, databasePath)
                }
            }
        } else installBundledDeepPatch(storage, databasePath)
        loadDeepPatch(packageRoot)
        return packageRoot
    }

    private fun installBundledDeepPatch(storage: File, databasePath: String): File {
        val staging = File(storage, "sections-staging/${UUID.randomUUID()}").apply { mkdirs() }
        val archive = File(staging, "package.zip")
        val stream = requireNotNull(LearningHubContent::class.java.getResourceAsStream(BUNDLED_DEEP_PATCH)) { "Bundled Sections 02-20 package is missing" }
        stream.use { input -> archive.outputStream().use { input.copyTo(it) } }
        check(sha256(archive) == DEEP_PATCH_SHA256) { "Bundled Sections 02-20 package hash is invalid" }
        return installDeepPatchArchive(archive, storage, databasePath, staging)
    }

    private fun installDeepPatchArchive(archive: File, storage: File, databasePath: String, existingStaging: File? = null): File {
        check(archive.isFile) { "Curriculum archive does not exist" }
        val sourceHash = sha256(archive)
        check(sourceHash == DEEP_PATCH_SHA256) { "Curriculum archive SHA-256 does not match the approved package" }
        val staging = existingStaging ?: File(storage, "sections-staging/${UUID.randomUUID()}").apply { mkdirs() }
        val extracted = File(staging, "content").apply { mkdirs() }
        extractZip(archive.toPath(), extracted.toPath())
        val packageRoot = File(extracted, "curriculum_patch.json").takeIf(File::isFile)?.let { extracted }
            ?: extracted.listFiles()?.singleOrNull { it.isDirectory && File(it, "curriculum_patch.json").isFile }
            ?: error("Package must contain curriculum_patch.json at its root")
        verifyChecksums(packageRoot)
        val patch = parseAndValidateDeep(packageRoot)
        val versionKey = "${patch.version}-${sourceHash.take(12)}"
        val versionDir = File(storage, "sections-S02-S20-versions/$versionKey")
        if (!versionDir.exists()) {
            versionDir.parentFile.mkdirs()
            moveAtomic(packageRoot.toPath(), versionDir.toPath())
        }
        validatedDeepPath = versionDir.absolutePath
        validatedDeepPatch = patch
        installDeepMetadata(databasePath, patch, sourceHash, versionKey)
        val pointer = File(storage, "sections-S02-S20-active")
        val current = pointer.takeIf(File::isFile)?.readText()?.trim()?.takeIf(String::isNotEmpty)
        if (current != null && current != versionKey) File(storage, "sections-S02-S20-previous").writeText(current)
        val temporary = File(storage, "sections-S02-S20-active.$versionKey.tmp").apply { writeText(versionKey) }
        moveAtomic(temporary.toPath(), pointer.toPath())
        if (existingStaging == null) staging.deleteRecursively()
        return versionDir
    }

    private fun installDeepMetadata(databasePath: String, patch: DeepCurriculumPatch, sourceHash: String, versionKey: String) {
        DriverManager.getConnection("jdbc:sqlite:${File(databasePath).absolutePath.replace("\\", "/")}").use { connection ->
            connection.autoCommit = false
            try {
                connection.createStatement().use { statement ->
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS LearningHubSection(id TEXT PRIMARY KEY, position INTEGER NOT NULL, title TEXT NOT NULL, description TEXT NOT NULL, status TEXT NOT NULL DEFAULT 'awaiting_import', content_version TEXT, source_checksum TEXT)")
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS LearningHubLesson(id TEXT PRIMARY KEY, section_id TEXT NOT NULL, topic_id TEXT, title TEXT NOT NULL, position INTEGER NOT NULL, status TEXT NOT NULL DEFAULT 'awaiting_import', current_version INTEGER NOT NULL DEFAULT 0, estimated_minutes INTEGER)")
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS LearningHubContentValidation(id TEXT PRIMARY KEY, content_version TEXT NOT NULL, status TEXT NOT NULL, errors_json TEXT NOT NULL DEFAULT '[]', validated_at INTEGER)")
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS LearningHubPatchVersion(version_key TEXT PRIMARY KEY, content_version TEXT NOT NULL, source_hash TEXT NOT NULL, section_count INTEGER NOT NULL, lesson_count INTEGER NOT NULL, problem_count INTEGER NOT NULL, installed_at INTEGER NOT NULL)")
                    statement.executeUpdate("CREATE TABLE IF NOT EXISTS LearningHubActivePatch(singleton INTEGER PRIMARY KEY CHECK(singleton = 1), version_key TEXT NOT NULL)")
                }
                connection.prepareStatement("INSERT OR REPLACE INTO LearningHubPatchVersion(version_key, content_version, source_hash, section_count, lesson_count, problem_count, installed_at) VALUES (?, ?, ?, ?, ?, ?, ?)").use { statement ->
                    statement.setString(1, versionKey); statement.setString(2, patch.version); statement.setString(3, sourceHash)
                    statement.setInt(4, patch.sectionCount); statement.setInt(5, patch.lessonCount); statement.setInt(6, patch.problemCount); statement.setLong(7, System.currentTimeMillis()); statement.executeUpdate()
                }
                patch.sections.forEach { section ->
                    connection.prepareStatement("INSERT OR REPLACE INTO LearningHubSection(id, position, title, description, status, content_version, source_checksum) VALUES (?, ?, ?, ?, 'active', ?, ?)").use { statement ->
                        statement.setString(1, section.id); statement.setInt(2, section.position); statement.setString(3, section.title); statement.setString(4, section.description)
                        statement.setString(5, patch.version); statement.setString(6, sourceHash); statement.executeUpdate()
                    }
                    section.lessons.forEach { lesson ->
                        connection.prepareStatement("INSERT OR REPLACE INTO LearningHubLesson(id, section_id, topic_id, title, position, status, current_version, estimated_minutes) VALUES (?, ?, NULL, ?, ?, 'active', 2, NULL)").use { statement ->
                            statement.setString(1, lesson.id); statement.setString(2, lesson.sectionId); statement.setString(3, lesson.title); statement.setInt(4, lesson.position); statement.executeUpdate()
                        }
                    }
                }
                connection.prepareStatement("INSERT OR REPLACE INTO LearningHubContentValidation(id, content_version, status, errors_json, validated_at) VALUES (?, ?, 'valid', '[]', ?)").use { statement ->
                    statement.setString(1, "sections-S02-S20-$versionKey"); statement.setString(2, patch.version); statement.setLong(3, System.currentTimeMillis()); statement.executeUpdate()
                }
                connection.prepareStatement("INSERT OR REPLACE INTO LearningHubActivePatch(singleton, version_key) VALUES (1, ?)").use { statement -> statement.setString(1, versionKey); statement.executeUpdate() }
                connection.commit()
            } catch (failure: Throwable) { connection.rollback(); throw failure }
        }
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
        section1Version = manifest.version
        section1Lessons = manifest.lessons.associateBy { it.id }
        section1Blocks = manifest.lessons.associate { lesson -> lesson.id to json.decodeFromString<Section1BlocksFile>(File(packageRoot, "content/${lesson.id}/article_blocks.json").readText()).blocks }
        section1Practice = manifest.lessons.associate { lesson -> lesson.id to readSection1Problems(File(packageRoot, "content/${lesson.id}/practice_80.jsonl")) }
        section1Quiz = manifest.lessons.associate { lesson -> lesson.id to readSection1Problems(File(packageRoot, "content/${lesson.id}/quiz_20.json"), array = true) }
        val glossary = json.parseToJsonElement(File(packageRoot, "content/section1_glossary.json").readText()).jsonObject
        section1Glossary = glossary.entries.map { (term, definition) -> Section1GlossaryEntry(term, definition.jsonPrimitive.content) }
    }

    private fun parseAndValidateDeep(packageRoot: File): DeepCurriculumPatch {
        if (validatedDeepPath == packageRoot.absolutePath) validatedDeepPatch?.let { return it }
        val patch = json.decodeFromString<DeepCurriculumPatch>(File(packageRoot, "curriculum_patch.json").readText())
        check(patch.format == "nous-learning-hub-deep-curriculum-patch") { "Unsupported deep curriculum format" }
        check(patch.version == "2.0.0-sections-02-20" && patch.brand == "Nous AI Academy" && patch.offline && patch.preserveIds) { "Deep curriculum identity contract failed" }
        check(patch.requiresSection1Version == "2.0.0-s01-pilot") { "This curriculum requires the verified Section 01 package" }
        check(patch.sectionCount == 19 && patch.lessonCount == 95 && patch.unitCount == 760) { "Deep curriculum section, lesson, or unit counts are invalid" }
        check(patch.practiceCount == 7_600 && patch.quizCount == 1_900 && patch.problemCount == 9_500) { "Deep curriculum problem counts are invalid" }
        check(patch.lessonPdfCount == 95 && patch.sectionMasterPdfCount == 19) { "Deep curriculum PDF counts are invalid" }
        check(patch.sections.map { it.id } == (2..20).map { "S${it.toString().padStart(2, '0')}" }) { "Expected ordered sections S02 through S20" }
        val allIds = linkedSetOf<String>()
        var units = 0; var practice = 0; var quizzes = 0
        patch.sections.forEach { section ->
            check(section.position in 2..20 && section.lessonCount == 5 && section.lessons.size == 5) { "Invalid section manifest: ${section.id}" }
            check(section.unitCount == 40 && section.practiceCount == 400 && section.quizCount == 100 && section.problemCount == 500) { "Invalid counts for ${section.id}" }
            check(File(packageRoot, section.masterPdf).isFile) { "Missing section PDF for ${section.id}" }
            section.lessons.forEachIndexed { index, lesson ->
                check(lesson.sectionId == section.id && lesson.position == index + 1) { "Invalid lesson ordering for ${lesson.id}" }
                check(lesson.id == "${section.id}-L${(index + 1).toString().padStart(2, '0')}") { "Invalid stable lesson ID ${lesson.id}" }
                check(lesson.pages == listOf("article", "detailed_summary", "quiz")) { "Invalid page contract for ${lesson.id}" }
                check(lesson.unitCount == 8 && lesson.practiceCount == 80 && lesson.quizCount == 20 && lesson.quizType == "multiple_choice") { "Invalid lesson counts for ${lesson.id}" }
                listOf(lesson.articlePath, lesson.summaryPath, lesson.quizPath, lesson.answersPath, lesson.pdfPath).forEach { relative ->
                    check(File(packageRoot, relative).isFile) { "Missing resource $relative" }
                }
                val lessonRoot = File(packageRoot, "content/${section.id}/${lesson.id}")
                val blockFile = json.decodeFromString<DeepArticleBlocksFile>(File(lessonRoot, "article_blocks.json").readText())
                check(blockFile.lessonId == lesson.id && blockFile.version == patch.version && blockFile.pages == lesson.pages) { "Article contract failed for ${lesson.id}" }
                check(blockFile.blocks.count { it.type == "heading" } == 8 && blockFile.blocks.count { it.type == "knowledge_check" } == 8) { "Expected eight article units for ${lesson.id}" }
                val practiceProblems = readDeepProblems(File(lessonRoot, "practice_80.jsonl"), section.id, array = false)
                val quizProblems = readDeepProblems(File(packageRoot, lesson.quizPath), section.id, array = true)
                check(practiceProblems.size == 80 && quizProblems.size == 20) { "Problem count failed for ${lesson.id}" }
                check(quizProblems.all { it.answerType == "multiple_choice" && it.options.size == 4 && it.answer.jsonPrimitive.int in 0..3 }) { "Quiz contract failed for ${lesson.id}" }
                (practiceProblems + quizProblems).forEach { problem ->
                    check(problem.lessonId == lesson.id && allIds.add(problem.id)) { "Duplicate or misrouted problem ${problem.id}" }
                }
                units += 8; practice += practiceProblems.size; quizzes += quizProblems.size
            }
        }
        check(units == 760 && practice == 7_600 && quizzes == 1_900 && allIds.size == 9_500) { "Deep curriculum aggregate counts failed" }
        check(allIds.first() == "NAA-02-01-001" && allIds.last() == "NAA-20-05-100") { "Deep curriculum problem ID range failed" }
        val indexed = File(packageRoot, "database/sections_02_20_deep_content.db")
        check(indexed.isFile && indexed.length() > 1_000_000L) { "Offline content index is missing or truncated" }
        validatedDeepPath = packageRoot.absolutePath
        validatedDeepPatch = patch
        return patch
    }

    private fun readDeepProblems(file: File, sectionId: String, array: Boolean): List<LearningHubProblem> {
        val elements = if (array) json.parseToJsonElement(file.readText()).jsonArray
        else file.readLines().filter(String::isNotBlank).map(json::parseToJsonElement)
        return elements.map { element ->
            val obj = element.jsonObject
            LearningHubProblem(
                id = obj.getValue("id").jsonPrimitive.content,
                sectionId = sectionId,
                lessonId = obj.getValue("lesson_id").jsonPrimitive.content,
                sequence = obj["sequence"]?.jsonPrimitive?.int,
                difficulty = obj["difficulty"]?.jsonPrimitive?.content,
                mode = "deep-curriculum",
                prompt = obj.getValue("prompt").jsonPrimitive.content,
                answerType = obj.getValue("type").jsonPrimitive.content,
                answer = obj.getValue("answer"),
                explanation = obj.getValue("explanation").jsonPrimitive.content,
                options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
                estimatedMinutes = obj["estimated_minutes"]?.jsonPrimitive?.int
            )
        }
    }

    private fun loadDeepPatch(packageRoot: File) {
        val patch = parseAndValidateDeep(packageRoot)
        check(section1Version == patch.requiresSection1Version) { "Installed Section 01 version is incompatible with ${patch.version}" }
        val loadedBlocks = linkedMapOf<String, List<LearningHubArticleBlock>>()
        val loadedProblems = linkedMapOf<String, List<LearningHubProblem>>()
        val searchable = linkedMapOf<String, String>()
        val deepSections = patch.sections.map { section ->
            val lessons = section.lessons.map { meta ->
                val lessonRoot = File(packageRoot, "content/${section.id}/${meta.id}")
                val blocks = json.decodeFromString<DeepArticleBlocksFile>(File(lessonRoot, "article_blocks.json").readText()).blocks
                loadedBlocks[meta.id] = blocks
                val practice = readDeepProblems(File(lessonRoot, "practice_80.jsonl"), section.id, false)
                val quiz = readDeepProblems(File(packageRoot, meta.quizPath), section.id, true)
                loadedProblems[meta.id] = (practice + quiz).sortedBy { it.sequence ?: Int.MAX_VALUE }
                val article = File(packageRoot, meta.articlePath).readText()
                val summary = File(packageRoot, meta.summaryPath).readText()
                searchable[meta.id] = listOf(section.title, meta.title, meta.objective, meta.principle, article, summary).joinToString("\n")
                LearningHubLesson(
                    id = meta.id, sectionId = meta.sectionId, position = meta.position, title = meta.title,
                    objective = meta.objective, principle = meta.principle,
                    workedExample = blocks.firstOrNull { it.type == "worked_example" }?.text.orEmpty(),
                    lab = blocks.firstOrNull { it.type == "project" }?.text.orEmpty(),
                    contentPath = meta.articlePath, answerPath = meta.answersPath,
                    problemStartId = meta.problemIdStart, problemEndId = meta.problemIdEnd, problemCount = 100,
                    summaryPath = meta.summaryPath, pdfPath = meta.pdfPath, articleWords = meta.articleWords,
                    unitCount = meta.unitCount, practiceCount = meta.practiceCount, quizCount = meta.quizCount,
                    contentVersion = patch.version
                )
            }
            LearningHubSection(section.id, section.position, section.title, section.level, section.description, 5, lessons, section.masterPdf, patch.version)
        }
        val sectionOne = requireNotNull(curriculum).sections.single { it.id == "S01" }
        curriculum = requireNotNull(curriculum).copy(
            version = "${section1Version ?: "2.0.0-s01-pilot"}+${patch.version}",
            sectionCount = 20, lessonCount = 100, problemCount = 10_000,
            sections = listOf(sectionOne) + deepSections
        )
        deepRoot = packageRoot
        deepPatch = patch
        deepBlocks = loadedBlocks
        problemsByLesson = problemsByLesson.filterKeys { it.startsWith("S01-") } + loadedProblems
        searchTextByLesson = searchable
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
        val listed = linkedSetOf<String>()
        checksumFile.forEachLine { line ->
            if (line.isBlank()) return@forEachLine
            val match = Regex("^([0-9a-fA-F]{64})\\s+(.+)$").matchEntire(line.trim()) ?: error("Invalid checksum entry")
            val expected = match.groupValues[1].lowercase()
            val relative = match.groupValues[2].trim().removePrefix("*").replace('\\', '/')
            check(relative.isNotBlank() && !relative.startsWith('/') && ':' !in relative) { "Unsafe checksum path: $relative" }
            check(listed.add(relative)) { "Duplicate checksum entry: $relative" }
            val file = root.toPath().resolve(relative.replace('/', File.separatorChar)).normalize().toFile()
            check(file.toPath().startsWith(root.toPath())) { "Checksum path escapes package" }
            check(file.isFile) { "Checksum target is missing: $relative" }
            check(sha256(file) == expected) { "Checksum mismatch: $relative" }
        }
        val actual = root.walkTopDown().filter(File::isFile).map { it.relativeTo(root).invariantSeparatorsPath }.filter { it != "checksums.sha256" }.toSet()
        check(actual == listed) {
            val missing = (listed - actual).take(3)
            val unlisted = (actual - listed).take(3)
            "Checksum coverage failed; missing=$missing unlisted=$unlisted"
        }
    }

    private fun extractZip(archive: Path, destination: Path) {
        val allowed = setOf("md", "json", "jsonl", "csv", "pdf", "db", "png", "svg", "sha256")
        val forbidden = setOf("exe", "dll", "bat", "cmd", "ps1", "com", "msi", "jar", "class", "sh", "js", "vbs", "scr")
        var entries = 0
        var expandedBytes = 0L
        ZipInputStream(Files.newInputStream(archive)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                entries++
                check(entries <= 20_000) { "Archive contains too many entries" }
                val name = entry.name.replace('\\', '/')
                check(name.isNotBlank() && !name.startsWith('/') && ':' !in name && name.split('/').none { it == ".." }) { "Unsafe archive entry: ${entry.name}" }
                val extension = name.substringAfterLast('.', "").lowercase()
                if (!entry.isDirectory) {
                    check(extension !in forbidden && extension in allowed) { "Unexpected file type in curriculum package: ${entry.name}" }
                }
                val target = destination.resolve(entry.name).normalize()
                check(target.startsWith(destination)) { "Archive entry escapes staging directory" }
                if (entry.isDirectory) Files.createDirectories(target) else {
                    Files.createDirectories(target.parent)
                    Files.newOutputStream(target).use { output ->
                        val buffer = ByteArray(64 * 1024)
                        while (true) {
                            val read = zip.read(buffer)
                            if (read < 0) break
                            expandedBytes += read
                            check(expandedBytes <= 1_000_000_000L) { "Expanded curriculum package is too large" }
                            output.write(buffer, 0, read)
                        }
                    }
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

    private fun contentRoot(lesson: LearningHubLesson): File? = if (lesson.contentVersion == deepPatch?.version) deepRoot else root
    actual fun lessonMarkdown(lesson: LearningHubLesson): String = contentRoot(lesson)?.let { File(it, lesson.contentPath).takeIf(File::isFile)?.readText() } ?: ""
    actual fun lessonArticleBlocks(lesson: LearningHubLesson): List<LearningHubArticleBlock> = deepBlocks[lesson.id].orEmpty()
    actual fun lessonReview(lesson: LearningHubLesson): String = lesson.summaryPath?.let { relative ->
        contentRoot(lesson)?.let { File(it, relative).takeIf(File::isFile)?.readText() }
    } ?: """
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
    actual fun answerKey(lesson: LearningHubLesson): String = contentRoot(lesson)?.let { File(it, lesson.answerPath).takeIf(File::isFile)?.readText() } ?: ""
    actual fun firstProblems(lesson: LearningHubLesson, limit: Int): List<LearningHubProblem> = problemsByLesson[lesson.id].orEmpty().take(limit)
    actual fun allPractice(lesson: LearningHubLesson): List<LearningHubProblem> = problemsByLesson[lesson.id].orEmpty().filter { it.sequence in 1..80 }
    actual fun lessonQuiz(lesson: LearningHubLesson, limit: Int): List<LearningHubProblem> =
        problemsByLesson[lesson.id].orEmpty().filter { problem ->
            problem.sequence in 81..100 && problem.answerType == "multiple_choice" &&
                problem.options.size >= 2 &&
                runCatching { problem.answer.jsonPrimitive.int in problem.options.indices }.getOrDefault(false)
        }.take(limit)
    actual fun lessonPdfPath(lesson: LearningHubLesson): String? = lesson.pdfPath?.let { relative -> contentRoot(lesson)?.let { File(it, relative).takeIf(File::isFile)?.absolutePath } }
    actual fun openLessonPdf(lesson: LearningHubLesson): Boolean = openFile(lessonPdfPath(lesson))
    actual fun chooseAndSaveLessonPdf(lesson: LearningHubLesson): Boolean = lessonPdfPath(lesson)?.let { chooseAndSave(File(it), "${lesson.id}-${safeName(lesson.title)}.pdf") } ?: false
    actual fun sectionPdfPath(section: LearningHubSection): String? = section.pdfPath?.let { relative ->
        (if (section.contentVersion == deepPatch?.version) deepRoot else root)?.let { File(it, relative).takeIf(File::isFile)?.absolutePath }
    } ?: root?.let { base -> File(base, "practice_sheets").listFiles()?.firstOrNull { file -> file.name.startsWith(section.id, ignoreCase = true) && file.extension.equals("pdf", true) }?.absolutePath }
    actual fun openSectionPdf(section: LearningHubSection): Boolean = runCatching { sectionPdfPath(section)?.let { java.awt.Desktop.getDesktop().open(File(it)); true } ?: false }.getOrDefault(false)
    actual fun saveSectionPdf(section: LearningHubSection, destinationPath: String): Boolean = runCatching {
        val source = sectionPdfPath(section)?.let(::File) ?: return false
        val destination = if (File(destinationPath).isAbsolute) File(destinationPath) else File(File(System.getProperty("user.home"), "Downloads"), destinationPath)
        destination.parentFile?.mkdirs(); source.copyTo(destination, overwrite = true); true
    }.getOrDefault(false)
    actual fun chooseAndSaveSectionPdf(section: LearningHubSection): Boolean = sectionPdfPath(section)?.let { chooseAndSave(File(it), "${section.id}-${safeName(section.title)}-Master.pdf") } ?: false

    actual fun searchLessons(query: String, limit: Int): List<LearningHubSearchResult> {
        val needle = query.trim().lowercase()
        if (needle.length < 2) return emptyList()
        val terms = needle.split(Regex("[^a-z0-9]+" )).filter { it.length >= 2 }
        if (terms.isEmpty()) return emptyList()
        val sections = curriculum?.sections.orEmpty().associateBy { it.id }
        return searchTextByLesson.entries.asSequence().filter { entry -> terms.all { it in entry.value.lowercase() } }.take(limit).mapNotNull { (lessonId, body) ->
            val sectionId = lessonId.substringBefore('-')
            val section = sections[sectionId] ?: return@mapNotNull null
            val lesson = section.lessons.firstOrNull { it.id == lessonId } ?: return@mapNotNull null
            val plain = body.replace(Regex("[#*|`>\\[\\]]"), " ").replace(Regex("\\s+"), " ")
            val at = plain.lowercase().indexOf(terms.first()).coerceAtLeast(0)
            LearningHubSearchResult(lessonId, sectionId, section.title, lesson.title, plain.substring(maxOf(0, at - 55), minOf(plain.length, at + terms.first().length + 115)).trim())
        }.toList()
    }

    actual fun selectAndInstallCurriculum(): Boolean {
        val chooser = JFileChooser().apply {
            dialogTitle = "Select a verified Nous curriculum package"
            fileFilter = FileNameExtensionFilter("ZIP curriculum packages", "zip")
        }
        if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) return false
        val db = databasePath ?: return false
        return installPackage(chooser.selectedFile.absolutePath, db)
    }

    @Synchronized
    actual fun rollbackCurriculum(): Boolean = runCatching {
        val storage = File(System.getProperty("user.home"), ".nous-ai-academy/learning-hub")
        val activeFile = File(storage, "sections-S02-S20-active")
        val previousFile = File(storage, "sections-S02-S20-previous")
        val active = activeFile.readText().trim()
        val previous = previousFile.readText().trim()
        val target = File(storage, "sections-S02-S20-versions/$previous")
        verifyChecksums(target); parseAndValidateDeep(target)
        val temporary = File(storage, "sections-S02-S20-active.rollback.tmp").apply { writeText(previous) }
        moveAtomic(temporary.toPath(), activeFile.toPath())
        previousFile.writeText(active)
        loadDeepPatch(target)
        mutableState.value = mutableState.value.copy(curriculum = curriculum, packagePath = target.absolutePath, curriculumVersion = deepPatch?.version, updateMessage = "Rolled back to curriculum ${deepPatch?.version}.", canRollback = true)
        true
    }.getOrElse { failure -> mutableState.value = mutableState.value.copy(updateMessage = "Rollback failed: ${failure.message}"); false }

    private fun openFile(path: String?): Boolean = runCatching { path?.let { java.awt.Desktop.getDesktop().open(File(it)); true } ?: false }.getOrDefault(false)
    private fun safeName(value: String): String = value.replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('-')
    private fun chooseAndSave(source: File, suggestedName: String): Boolean = runCatching {
        val chooser = JFileChooser().apply { dialogTitle = "Save verified curriculum PDF"; selectedFile = File(suggestedName); fileFilter = FileNameExtensionFilter("PDF document", "pdf") }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) return false
        val destination = chooser.selectedFile.let { if (it.extension.equals("pdf", true)) it else File(it.parentFile, "${it.name}.pdf") }
        if (destination.exists() && JOptionPane.showConfirmDialog(null, "${destination.name} already exists. Replace it?", "Confirm replace", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE) != JOptionPane.YES_OPTION) return false
        source.copyTo(destination, overwrite = true)
        check(source.length() == destination.length() && sha256(source) == sha256(destination)) { "Saved PDF verification failed" }
        true
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
