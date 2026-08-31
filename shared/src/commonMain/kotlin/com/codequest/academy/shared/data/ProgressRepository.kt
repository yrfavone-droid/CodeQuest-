package com.codequest.academy.shared.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.QueryResult
import com.codequest.academy.database.AppDatabase
import com.codequest.academy.shared.auth.LocalPasswordHasher
import com.codequest.academy.shared.auth.PasswordRecord
import com.codequest.academy.shared.auth.normalizeLocalEmail
import com.codequest.academy.shared.auth.validateLocalDisplayName
import com.codequest.academy.shared.auth.validateLocalEmail
import com.codequest.academy.shared.auth.validateLocalPassword

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
            """CREATE TABLE IF NOT EXISTS AppSetting (user_id TEXT NOT NULL, setting_key TEXT NOT NULL, setting_value TEXT NOT NULL, PRIMARY KEY (user_id, setting_key))""",
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
    /** Clears old application-provided content while retaining learner-owned data. */
    fun prepareCleanLibrary(): CleanLibrarySummary = academyStore.prepareEmptyLibrary()

    /** Registers the verified offline Nous catalogue without replacing any learner-owned state. */
    fun installVerifiedLibrary(resources: List<OfflineLibraryResource>) = academyStore.installVerifiedLibrary(resources)

    fun libraryResources(kind: LibraryKind? = null): List<OfflineLibraryResource> = academyStore.libraryResources(kind)

    fun readingState(resource: OfflineLibraryResource): LibraryReadingState? = getUserId()?.let { academyStore.readingState(it, resource) }

    fun readingStates(): List<LibraryReadingState> = getUserId()?.let(academyStore::readingStates) ?: emptyList()

    fun saveReadingPage(resource: OfflineLibraryResource, page: Int): Boolean {
        val userId = getUserId() ?: return false
        academyStore.saveReadingPage(userId, resource, page)
        return true
    }

    fun addBookmark(resource: OfflineLibraryResource, page: Int): LibraryBookmark? = getUserId()?.let { academyStore.addBookmark(it, resource, page) }

    fun bookmarks(): List<LibraryBookmark> = getUserId()?.let(academyStore::bookmarks) ?: emptyList()

    fun setSetting(key: String, value: String) {
        val userId = getUserId() ?: return
        queries.setSetting(userId, key, value)
    }

    fun getSetting(key: String, default: String): String {
        val userId = getUserId() ?: return default
        return queries.getSetting(userId, key).executeAsOneOrNull() ?: default
    }

}
