package com.codequest.academy.shared.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import com.codequest.academy.database.AppDatabase
import com.codequest.academy.shared.models.Level
import com.codequest.academy.shared.models.PathAsset
import com.codequest.academy.shared.auth.LocalPasswordHasher
import com.codequest.academy.shared.auth.PasswordRecord
import com.codequest.academy.shared.auth.normalizeLocalEmail
import com.codequest.academy.shared.auth.validateLocalDisplayName
import com.codequest.academy.shared.auth.validateLocalEmail
import com.codequest.academy.shared.auth.validateLocalPassword
import kotlinx.serialization.json.Json

data class CurriculumSeedResult(
    val version: String,
    val trackCount: Int,
    val pathCount: Int,
    val levelCount: Int,
    val nodeCount: Int,
    val changed: Boolean
)

data class LearningProgressSummary(
    val tracksStarted: Int,
    val levelsCompleted: Int,
    val completedNodes: Int,
    val projectsSubmitted: Int,
    val totalNodes: Int
)

data class ActivityRecord(
    val nodeId: String,
    val title: String,
    val eventType: String,
    val occurredAt: Long
)

data class ProjectDraftRecord(
    val projectId: String,
    val notes: String,
    val updatedAt: Long,
    val submitted: Boolean
)

data class LocalAccount(
    val userId: String,
    val displayName: String,
    val email: String,
    val createdAt: Long,
    val lastLoginAt: Long
)

sealed interface AccountResult {
    data class Success(val account: LocalAccount) : AccountResult
    data class Error(val message: String) : AccountResult
}

class ProgressRepository(private val sqlDriver: SqlDriver) {
    private val database = AppDatabase(sqlDriver)
    private val queries = database.appDatabaseQueries
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }
    private val academyStore = LocalAcademyStore(sqlDriver)

    init {
        ensureSupplementalSchema()
        academyStore.ensureSchema()
    }

    /**
     * Applies the one-time compatibility migration for prototype databases.
     * Every failure is surfaced: continuing with a partially migrated learner database risks data loss.
     */
    private fun ensureSupplementalSchema() {
        val statements = listOf(
            """CREATE TABLE IF NOT EXISTS CurriculumNode (id TEXT PRIMARY KEY, level_id TEXT NOT NULL, node_type TEXT NOT NULL, node_order INTEGER NOT NULL, required INTEGER NOT NULL)""",
            """CREATE TABLE IF NOT EXISTS ActivityEvent (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, node_id TEXT NOT NULL, title TEXT NOT NULL, event_type TEXT NOT NULL, occurred_at INTEGER NOT NULL)""",
            """CREATE TABLE IF NOT EXISTS AppSetting (user_id TEXT NOT NULL, setting_key TEXT NOT NULL, setting_value TEXT NOT NULL, PRIMARY KEY (user_id, setting_key))""",
            """CREATE TABLE IF NOT EXISTS ProjectDraft (user_id TEXT NOT NULL, project_id TEXT NOT NULL, notes TEXT NOT NULL, updated_at INTEGER NOT NULL, submitted INTEGER NOT NULL DEFAULT 0, PRIMARY KEY (user_id, project_id))""",
            """CREATE TABLE IF NOT EXISTS AssessmentAttempt (id TEXT PRIMARY KEY, user_id TEXT NOT NULL, node_id TEXT NOT NULL, score INTEGER NOT NULL, total INTEGER NOT NULL, answers_json TEXT NOT NULL, completed_at INTEGER NOT NULL)""",
            """CREATE TABLE IF NOT EXISTS ActiveSession (session_id INTEGER PRIMARY KEY, user_id TEXT NOT NULL, updated_at INTEGER NOT NULL)""",
            """CREATE TABLE IF NOT EXISTS SchemaMigration (version INTEGER PRIMARY KEY, applied_at INTEGER NOT NULL)"""
        )
        statements.forEach { statement -> sqlDriver.execute(null, statement, 0) }

        val existingColumns = sqlDriver.executeQuery(null, "PRAGMA table_info(UserProfile)", {
            QueryResult.Value(buildSet {
                while (it.next().value) add(requireNotNull(it.getString(1)))
            })
        }, 0).value
        val requiredColumns = listOf(
            "normalized_email TEXT",
            "password_hash TEXT",
            "password_salt TEXT",
            "password_algorithm TEXT",
            "password_parameters TEXT",
            "email TEXT",
            "email_verified INTEGER NOT NULL DEFAULT 0",
            "avatar_url TEXT",
            "created_at INTEGER NOT NULL DEFAULT 0",
            "updated_at INTEGER NOT NULL DEFAULT 0",
            "last_login_at INTEGER NOT NULL DEFAULT 0"
        )
        requiredColumns.forEach { column ->
            val name = column.substringBefore(' ')
            if (name !in existingColumns) sqlDriver.execute(null, "ALTER TABLE UserProfile ADD COLUMN $column", 0)
        }
        sqlDriver.execute(null, "CREATE UNIQUE INDEX IF NOT EXISTS idx_user_profile_email ON UserProfile(normalized_email) WHERE normalized_email IS NOT NULL", 0)
        sqlDriver.execute(null, "INSERT OR IGNORE INTO SchemaMigration(version, applied_at) VALUES (2, ${System.currentTimeMillis()})", 0)
    }

    fun seedCurriculum(version: String, paths: List<PathAsset>): CurriculumSeedResult {
        require(paths.size == 10) { "Expected 10 curriculum paths, found ${paths.size}" }
        require(paths.sumOf { it.levels.size } == 50) { "Expected 50 curriculum levels" }
        val expectedNodes = paths.sumOf { path -> path.levels.sumOf { it.timeline_nodes.size } }
        val installedVersion = queries.getCurriculumVersion().executeAsOneOrNull()?.version_id
        val isCurrent = installedVersion == version &&
            queries.getAllTracks().executeAsList().size == 5 &&
            queries.getAllPaths().executeAsList().size == 10 &&
            queries.getAllLevels().executeAsList().size == 50 &&
            queries.getAllCurriculumNodes().executeAsList().size == expectedNodes

        if (isCurrent) {
            return CurriculumSeedResult(version, 5, 10, 50, expectedNodes, false)
        }

        database.transaction {
            paths.map { it.track_id }.distinct().forEach { trackId ->
                queries.insertTrack(trackId, trackTitle(trackId))
            }
            paths.forEach { asset ->
                queries.insertPath(asset.path.id, asset.track_id, asset.path.title)
                asset.levels.forEach { level ->
                    queries.insertLevel(
                        level.id,
                        level.path_id,
                        level.code,
                        level.level_number.toLong(),
                        level.title,
                        level.goal,
                        json.encodeToString(Level.serializer(), level)
                    )
                    level.timeline_nodes.forEach { node ->
                        queries.insertCurriculumNode(
                            node.id,
                            level.id,
                            node.type,
                            node.order.toLong(),
                            if (node.required) 1L else 0L
                        )
                    }
                }
            }
            queries.insertCurriculumVersion(version, System.currentTimeMillis())
        }
        return CurriculumSeedResult(version, 5, 10, 50, expectedNodes, true)
    }

    fun getNodeState(userId: String, nodeId: String): String =
        queries.getNodeProgress(userId, nodeId).executeAsOneOrNull()?.state ?: "locked"

    fun updateNodeState(userId: String, nodeId: String, state: String, title: String = nodeId) {
        val now = System.currentTimeMillis()
        queries.updateNodeProgress(nodeId, userId, state, now)
        if (state == "completed" || state == "failed") {
            queries.insertActivity("$userId:$nodeId:$now", userId, nodeId, title, state, now)
        }
    }

    fun hasProfile(): Boolean = hasActiveSession()

    fun hasAnyProfiles(): Boolean = queries.getAllUserProfiles().executeAsList().isNotEmpty()

    fun hasActiveSession(): Boolean {
        val id = queries.getActiveUserId().executeAsOneOrNull() ?: return false
        return queries.getUserProfileById(id).executeAsOneOrNull() != null
    }

    fun hasLegacyProfiles(): Boolean = queries.getProfilesNeedingCredentials().executeAsList().isNotEmpty()

    /** Returns the first profile that still needs local credentials for one-time migration. */
    fun legacyAccount(): LocalAccount? = queries.getProfilesNeedingCredentials().executeAsList().firstOrNull()?.let(::toLocalAccount)

    fun activeAccount(): LocalAccount? = queries.getUserProfile().executeAsOneOrNull()?.let(::toLocalAccount)

    fun getAllAccounts(): List<LocalAccount> = queries.getAllUserProfiles().executeAsList().map(::toLocalAccount)

    fun createLocalAccount(displayName: String, email: String, password: String): AccountResult {
        validateLocalDisplayName(displayName)?.let { return AccountResult.Error(it) }
        validateLocalEmail(email)?.let { return AccountResult.Error(it) }
        validateLocalPassword(password)?.let { return AccountResult.Error(it) }
        val normalized = normalizeLocalEmail(email)
        if (queries.getUserByEmail(normalized).executeAsOneOrNull() != null) {
            return AccountResult.Error("An account with this email already exists. Sign in instead.")
        }
        val now = System.currentTimeMillis()
        val credentials = LocalPasswordHasher.create(password)
        // The random salt also makes the local identifier collision-resistant
        // when two addresses are created in the same millisecond.
        val userId = "local_${now}_${credentials.salt.take(12)}"
        return runCatching {
            database.transaction {
                queries.insertLocalProfile(userId, displayName.trim(), normalized, credentials.hash, credentials.salt, credentials.algorithm, credentials.parameters, normalized, now, now)
                queries.activateUser(userId, now)
            }
            AccountResult.Success(toLocalAccount(queries.getUserProfileById(userId).executeAsOne()))
        }.getOrElse { AccountResult.Error("We could not save this account. Please try again.") }
    }

    fun signIn(email: String, password: String): AccountResult {
        val account = queries.getUserByEmail(normalizeLocalEmail(email)).executeAsOneOrNull()
        val valid = account?.let {
            val hash = it.password_hash
            val salt = it.password_salt
            if (hash == null || salt == null) false else LocalPasswordHasher.verify(password, PasswordRecord(hash, salt, it.password_algorithm.orEmpty(), it.password_parameters.orEmpty()))
        } ?: false
        if (!valid || account == null) return AccountResult.Error("Incorrect email or password.")
        val now = System.currentTimeMillis()
        return runCatching {
            database.transaction { queries.updateLocalLogin(now, now, account.user_id); queries.activateUser(account.user_id, now) }
            AccountResult.Success(toLocalAccount(queries.getUserProfileById(account.user_id).executeAsOne()))
        }.getOrElse { AccountResult.Error("We could not sign you in. Please try again.") }
    }

    fun completeLegacySetup(userId: String, displayName: String, email: String, password: String): AccountResult {
        val account = queries.getUserProfileById(userId).executeAsOneOrNull() ?: return AccountResult.Error("The local profile could not be found.")
        if (account.password_hash != null) return AccountResult.Error("This profile already has local credentials.")
        validateLocalDisplayName(displayName)?.let { return AccountResult.Error(it) }
        validateLocalEmail(email)?.let { return AccountResult.Error(it) }
        validateLocalPassword(password)?.let { return AccountResult.Error(it) }
        val normalized = normalizeLocalEmail(email)
        val existing = queries.getUserByEmail(normalized).executeAsOneOrNull()
        if (existing != null && existing.user_id != userId) return AccountResult.Error("An account with this email already exists. Sign in instead.")
        val now = System.currentTimeMillis(); val credentials = LocalPasswordHasher.create(password)
        return runCatching {
            database.transaction {
                queries.updateLocalProfile(displayName.trim(), normalized, normalized, now, userId)
                queries.updateLocalCredentials(normalized, normalized, credentials.hash, credentials.salt, credentials.algorithm, credentials.parameters, now, userId)
                queries.activateUser(userId, now)
            }
            AccountResult.Success(toLocalAccount(queries.getUserProfileById(userId).executeAsOne()))
        }.getOrElse { AccountResult.Error("We could not migrate this profile. Please try again.") }
    }

    fun updateLocalProfile(displayName: String, email: String, currentPassword: String? = null): AccountResult {
        val current = activeAccount() ?: return AccountResult.Error("No signed-in account was found.")
        validateLocalDisplayName(displayName)?.let { return AccountResult.Error(it) }
        validateLocalEmail(email)?.let { return AccountResult.Error(it) }
        val normalized = normalizeLocalEmail(email)
        val existing = queries.getUserByEmail(normalized).executeAsOneOrNull()
        if (existing != null && existing.user_id != current.userId) return AccountResult.Error("An account with this email already exists.")
        if (normalized != current.email) {
            val row = queries.getUserProfileById(current.userId).executeAsOne()
            val valid = currentPassword != null && row.password_hash != null && row.password_salt != null && LocalPasswordHasher.verify(currentPassword, PasswordRecord(row.password_hash, row.password_salt, row.password_algorithm.orEmpty(), row.password_parameters.orEmpty()))
            if (!valid) return AccountResult.Error("Enter your current password to change email.")
        }
        val now = System.currentTimeMillis()
        return runCatching { queries.updateLocalProfile(displayName.trim(), normalized, normalized, now, current.userId); AccountResult.Success(toLocalAccount(queries.getUserProfileById(current.userId).executeAsOne())) }.getOrElse { AccountResult.Error("We could not save your profile.") }
    }

    fun changePassword(currentPassword: String, newPassword: String, confirmation: String): AccountResult {
        val current = activeAccount() ?: return AccountResult.Error("No signed-in account was found.")
        val row = queries.getUserProfileById(current.userId).executeAsOne()
        if (row.password_hash == null || row.password_salt == null || !LocalPasswordHasher.verify(currentPassword, PasswordRecord(row.password_hash, row.password_salt, row.password_algorithm.orEmpty(), row.password_parameters.orEmpty()))) return AccountResult.Error("Current password is incorrect.")
        validateLocalPassword(newPassword)?.let { return AccountResult.Error(it) }
        if (newPassword != confirmation) return AccountResult.Error("The passwords do not match.")
        val credentials = LocalPasswordHasher.create(newPassword); val now = System.currentTimeMillis()
        return runCatching { queries.updateLocalPassword(credentials.hash, credentials.salt, credentials.algorithm, credentials.parameters, now, current.userId); AccountResult.Success(toLocalAccount(queries.getUserProfileById(current.userId).executeAsOne())) }.getOrElse { AccountResult.Error("We could not change your password.") }
    }

    fun createProfile(name: String) {
        val normalized = name.trim().ifEmpty { "Learner" }
        val existing = queries.getUserProfile().executeAsOneOrNull()
        if (existing == null) {
            val userId = "offline_user_${System.currentTimeMillis()}"
            queries.insertUserProfile(userId, normalized, 1L)
            queries.activateUser(userId, System.currentTimeMillis())
        } else {
            queries.updateUserProfileName(normalized, existing.user_id)
            queries.activateUser(existing.user_id, System.currentTimeMillis())
        }
    }

    /** Explicitly selects a profile without touching any user-owned progress rows. */
    fun activateProfile(userId: String): Boolean {
        if (queries.getUserProfileById(userId).executeAsOneOrNull() == null) return false
        queries.activateUser(userId, System.currentTimeMillis())
        return true
    }

    fun signOut() {
        queries.clearActiveSession()
    }

    private fun toLocalAccount(row: com.codequest.academy.database.UserProfile): LocalAccount = LocalAccount(row.user_id, row.name, row.email ?: "", row.created_at, row.last_login_at)

    fun updateProfileName(name: String) {
        val userId = getUserId() ?: return
        queries.updateUserProfileName(name.trim().ifEmpty { "Learner" }, userId)
    }

    fun getProfileName(): String = queries.getUserProfile().executeAsOneOrNull()?.name ?: "Learner"
    fun getUserId(): String? = queries.getUserProfile().executeAsOneOrNull()?.user_id
    fun getAllNodeProgress(userId: String) = queries.getAllNodeProgress(userId).executeAsList()
    fun getPaths() = queries.getAllPaths().executeAsList()
    fun getPathById(pathId: String) = queries.getPathById(pathId).executeAsOneOrNull()
    fun getLevelsForPath(pathId: String) = queries.getLevelsForPath(pathId).executeAsList()
    fun getAllLevels() = queries.getAllLevels().executeAsList()
    fun getLevelById(levelId: String) = queries.getLevelById(levelId).executeAsOneOrNull()
    fun getCurriculumVersion(): String = queries.getCurriculumVersion().executeAsOneOrNull()?.version_id ?: "Not installed"

    /** Clears old application-provided content while retaining learner-owned data. */
    fun prepareCleanLibrary(): CleanLibrarySummary = academyStore.prepareEmptyLibrary()

    fun isCurriculumCurrent(version: String): Boolean =
        queries.getCurriculumVersion().executeAsOneOrNull()?.version_id == version &&
            queries.getAllTracks().executeAsList().size == 5 &&
            queries.getAllPaths().executeAsList().size == 10 &&
            queries.getAllLevels().executeAsList().size == 50 &&
            queries.getAllCurriculumNodes().executeAsList().size >= 1650

    fun getTrackProgress(trackId: String): Float {
        val userId = getUserId() ?: return 0f
        val nodes = queries.getNodesForTrack(userId, trackId).executeAsList().filter { it.required == 1L }
        if (nodes.isEmpty()) return 0f
        return nodes.count { it.state == "completed" }.toFloat() / nodes.size
    }

    fun getPathProgress(pathId: String): Float {
        val userId = getUserId() ?: return 0f
        val nodes = queries.getNodesForPath(userId, pathId).executeAsList().filter { it.required == 1L }
        if (nodes.isEmpty()) return 0f
        return nodes.count { it.state == "completed" }.toFloat() / nodes.size
    }

    fun getLevelNodeStates(levelId: String): Map<String, String> {
        val nodes = queries.getNodesForLevel(levelId).executeAsList()
        val userId = getUserId()
        val saved = if (userId == null) emptyMap() else getAllNodeProgress(userId).associate { it.node_id to it.state }
        var prerequisitesComplete = true
        return buildMap {
            nodes.forEach { node ->
                val persisted = saved[node.id]
                val state = when {
                    persisted == "completed" -> "completed"
                    persisted == "in_progress" -> "in_progress"
                    persisted == "failed" -> "failed"
                    prerequisitesComplete -> "available"
                    else -> "locked"
                }
                put(node.id, state)
                if (node.required == 1L && persisted != "completed") prerequisitesComplete = false
            }
        }
    }

    fun getNextAvailableNode(): Pair<String, String>? {
        val levels = getAllLevels().sortedWith(compareBy<com.codequest.academy.database.Level> { if (it.code == "FE-101") 0 else 1 }.thenBy { it.code })
        levels.forEach { level ->
            val states = getLevelNodeStates(level.id)
            val next = queries.getNodesForLevel(level.id).executeAsList().firstOrNull { states[it.id] in setOf("available", "in_progress", "failed") }
            if (next != null) return level.id to next.id
        }
        return null
    }

    fun getProgressSummary(): LearningProgressSummary {
        val userId = getUserId() ?: return LearningProgressSummary(0, 0, 0, 0, queries.getAllCurriculumNodes().executeAsList().count { it.required == 1L })
        val completed = getAllNodeProgress(userId).filter { it.state == "completed" }
        val startedTracks = listOf("web_development", "app_development", "cybersecurity", "problem_solving", "ai_machine_learning")
            .count { getTrackProgress(it) > 0f }
        return LearningProgressSummary(
            tracksStarted = startedTracks,
            levelsCompleted = completed.count { it.node_id.endsWith("-REFLECTION") },
            completedNodes = completed.size,
            projectsSubmitted = completed.count { it.node_id.endsWith("-PROJECT") },
            totalNodes = queries.getAllCurriculumNodes().executeAsList().count { it.required == 1L }
        )
    }

    fun getRecentActivity(limit: Long = 8): List<ActivityRecord> {
        val userId = getUserId() ?: return emptyList()
        return queries.getRecentActivity(userId, limit).executeAsList().map {
            ActivityRecord(it.node_id, it.title, it.event_type, it.occurred_at)
        }
    }

    fun setSetting(key: String, value: String) {
        val userId = getUserId() ?: return
        queries.setSetting(userId, key, value)
    }

    fun getSetting(key: String, default: String): String {
        val userId = getUserId() ?: return default
        return queries.getSetting(userId, key).executeAsOneOrNull() ?: default
    }

    fun saveProjectDraft(projectId: String, notes: String, submitted: Boolean = false) {
        val userId = getUserId() ?: return
        queries.saveProjectDraft(userId, projectId, notes, System.currentTimeMillis(), if (submitted) 1L else 0L)
    }

    fun getProjectDraft(projectId: String): ProjectDraftRecord? {
        val userId = getUserId() ?: return null
        return queries.getProjectDraft(userId, projectId).executeAsOneOrNull()?.let {
            ProjectDraftRecord(it.project_id, it.notes, it.updated_at, it.submitted == 1L)
        }
    }

    fun getProjectDrafts(): List<ProjectDraftRecord> {
        val userId = getUserId() ?: return emptyList()
        return queries.getProjectDrafts(userId).executeAsList().map {
            ProjectDraftRecord(it.project_id, it.notes, it.updated_at, it.submitted == 1L)
        }
    }

    fun saveAssessmentAttempt(nodeId: String, score: Int, total: Int, answersJson: String) {
        val userId = getUserId() ?: return
        val now = System.currentTimeMillis()
        queries.insertAssessmentAttempt("$userId:$nodeId:$now", userId, nodeId, score.toLong(), total.toLong(), answersJson, now)
    }

    private fun trackTitle(trackId: String): String = when (trackId) {
        "web_development" -> "Web Development"
        "app_development" -> "App Development"
        "cybersecurity" -> "Cybersecurity"
        "problem_solving" -> "Problem Solving"
        "ai_machine_learning" -> "AI and Machine Learning"
        else -> trackId.replace('_', ' ')
    }
}
