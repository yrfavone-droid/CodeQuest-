package com.codequest.academy.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.codequest.academy.database.AppDatabase
import kotlin.test.*

class LocalAccountRepositoryTest {
    private fun repository(): Pair<ProgressRepository, JdbcSqliteDriver> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return ProgressRepository(driver) to driver
    }

    @Test
    fun createsHashedAccountAndPersistsActiveSession() {
        val (repository, driver) = repository()
        val result = repository.createLocalAccount("Ada Lovelace", "Ada@Example.COM", "securePass1")
        assertIs<AccountResult.Success>(result)
        val account = result.account
        assertEquals("ada@example.com", account.email)
        assertTrue(repository.hasActiveSession())
        val row = AppDatabase(driver).appDatabaseQueries.getUserByEmail("ada@example.com").executeAsOne()
        assertNotEquals("securePass1", row.password_hash)
        assertTrue(row.password_hash!!.isNotBlank())
        assertTrue(row.password_salt!!.isNotBlank())
        assertEquals("PBKDF2-HMAC-SHA256", row.password_algorithm)
    }

    @Test
    fun validatesFieldsAndDuplicateEmailCaseInsensitively() {
        val (repository, _) = repository()
        assertIs<AccountResult.Error>(repository.createLocalAccount("", "a@example.com", "password1"))
        assertIs<AccountResult.Error>(repository.createLocalAccount("A", "not-an-email", "password1"))
        assertIs<AccountResult.Error>(repository.createLocalAccount("Valid Name", "valid@example.com", "weakpass"))
        assertIs<AccountResult.Success>(repository.createLocalAccount("Valid Name", "valid@example.com", "password1A"))
        val duplicate = repository.createLocalAccount("Other Name", "VALID@EXAMPLE.COM", "password1A")
        assertEquals("An account with this email already exists. Sign in instead.", (duplicate as AccountResult.Error).message)
    }

    @Test
    fun signInSignOutAndChangePasswordBehaveSecurely() {
        val (repository, _) = repository()
        val created = repository.createLocalAccount("Grace Hopper", "grace@example.com", "correct1A") as AccountResult.Success
        assertIs<AccountResult.Error>(repository.signIn("grace@example.com", "wrong1A"))
        repository.signOut()
        assertFalse(repository.hasActiveSession())
        assertIs<AccountResult.Success>(repository.signIn("GRACE@EXAMPLE.COM", "correct1A"))
        assertIs<AccountResult.Success>(repository.changePassword("correct1A", "newSecret2B", "newSecret2B"))
        repository.signOut()
        assertIs<AccountResult.Error>(repository.signIn("grace@example.com", "correct1A"))
        val signedIn = repository.signIn("grace@example.com", "newSecret2B") as AccountResult.Success
        assertEquals(created.account.userId, signedIn.account.userId)
    }

    @Test
    fun progressAndSettingsAreIsolatedBetweenProfiles() {
        val (repository, _) = repository()
        val first = repository.createLocalAccount("First Learner", "first@example.com", "firstPass1") as AccountResult.Success
        repository.setSetting("theme", "dark")
        repository.saveProjectDraft("project-1", "first draft")
        repository.signOut()
        val second = repository.createLocalAccount("Second Learner", "second@example.com", "secondPass1") as AccountResult.Success
        assertNotEquals(first.account.userId, second.account.userId)
        assertEquals("light", repository.getSetting("theme", "light"))
        assertNull(repository.getProjectDraft("project-1"))
        repository.signOut()
        repository.signIn("first@example.com", "firstPass1")
        assertEquals("dark", repository.getSetting("theme", "light"))
        assertEquals("first draft", repository.getProjectDraft("project-1")?.notes)
    }

    @Test
    fun legacyProfileKeepsItsIdAndDataWhenCredentialsAreAttached() {
        val (repository, _) = repository()
        repository.createProfile("Legacy Learner")
        val legacyId = repository.getUserId()!!
        repository.setSetting("legacy_setting", "kept")
        repository.saveProjectDraft("legacy-project", "existing work")
        val migrated = repository.completeLegacySetup(legacyId, "Migrated Learner", "legacy@example.com", "legacyPass1")
        assertIs<AccountResult.Success>(migrated)
        assertEquals(legacyId, migrated.account.userId)
        assertEquals("kept", repository.getSetting("legacy_setting", "missing"))
        assertEquals("existing work", repository.getProjectDraft("legacy-project")?.notes)
        assertIs<AccountResult.Error>(repository.completeLegacySetup(legacyId, "Migrated Learner", "other@example.com", "legacyPass1"))
    }
}
