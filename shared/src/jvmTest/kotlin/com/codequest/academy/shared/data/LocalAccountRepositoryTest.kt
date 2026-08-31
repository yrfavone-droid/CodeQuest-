package com.codequest.academy.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.QueryResult
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
    fun settingsAreIsolatedBetweenProfiles() {
        val (repository, _) = repository()
        val first = repository.createLocalAccount("First Learner", "first@example.com", "firstPass1") as AccountResult.Success
        repository.setSetting("theme", "dark")
        repository.signOut()
        val second = repository.createLocalAccount("Second Learner", "second@example.com", "secondPass1") as AccountResult.Success
        assertNotEquals(first.account.userId, second.account.userId)
        assertEquals("light", repository.getSetting("theme", "light"))
        repository.signOut()
        repository.signIn("first@example.com", "firstPass1")
        assertEquals("dark", repository.getSetting("theme", "light"))
    }

    @Test
    fun legacyProfileKeepsItsIdAndDataWhenCredentialsAreAttached() {
        val (repository, _) = repository()
        repository.createProfile("Legacy Learner")
        val legacyId = repository.getUserId()!!
        repository.setSetting("legacy_setting", "kept")
        val migrated = repository.completeLegacySetup(legacyId, "Migrated Learner", "legacy@example.com", "legacyPass1")
        assertIs<AccountResult.Success>(migrated)
        assertEquals(legacyId, migrated.account.userId)
        assertEquals("kept", repository.getSetting("legacy_setting", "missing"))
        assertIs<AccountResult.Error>(repository.completeLegacySetup(legacyId, "Migrated Learner", "other@example.com", "legacyPass1"))
    }

    @Test
    fun cleanLibraryArchivesCatalogueWithoutErasingBookmarksOrNotes() {
        val (repository, driver) = repository()
        val account = repository.createLocalAccount("Library Learner", "library@example.com", "libraryPass1") as AccountResult.Success
        driver.execute(null, "INSERT INTO AiBook VALUES ('legacy-book', 'Legacy book', 1, 'published', 'legacy-book.pdf')", 0)
        driver.execute(null, "INSERT INTO AiKnowledgeFile VALUES ('legacy-file', 'Legacy file', 'summary', 1, 'published', 'legacy-file.pdf', 0)", 0)
        driver.execute(null, "INSERT INTO AiBookmark VALUES ('bookmark-1', '${account.account.userId}', 'book', 'legacy-book', 'page-1', 1)", 0)
        driver.execute(null, "INSERT INTO AiNote VALUES ('note-1', '${account.account.userId}', 'book', 'legacy-book', 'private note', 1)", 0)

        val result = repository.prepareCleanLibrary()

        assertEquals(1, result.archivedBooks)
        assertEquals(1, result.archivedFiles)
        assertEquals(0, rowCount(driver, "AiBook"))
        assertEquals(0, rowCount(driver, "AiKnowledgeFile"))
        assertEquals(1, rowCount(driver, "AiBookmark"))
        assertEquals(1, rowCount(driver, "AiNote"))
        assertEquals(2, rowCount(driver, "LegacyLibraryArchive"))
    }

    @Test
    fun verifiedLibraryKeepsPrivateReaderStateAndBookmarksLocal() {
        val (repository, _) = repository()
        repository.createLocalAccount("Offline Reader", "reader@example.com", "readerPass1")
        repository.installVerifiedLibrary(NousLibraryCatalog.resources)

        val books = repository.libraryResources(LibraryKind.BOOK)
        val files = repository.libraryResources(LibraryKind.INTENSIVE_FILE)
        assertEquals(5, books.size)
        assertEquals(20, files.size)
        assertEquals(1750, (books + files).sumOf { it.pageCount })

        val book = books.first()
        assertTrue(repository.saveReadingPage(book, 42))
        assertEquals(42, repository.readingState(book)?.currentPage)
        val bookmark = repository.addBookmark(book, 42)
        assertNotNull(bookmark)
        assertEquals(42, repository.bookmarks().single().page)
        assertFailsWith<IllegalArgumentException> { repository.saveReadingPage(book, 151) }
    }

    private fun rowCount(driver: JdbcSqliteDriver, table: String): Long =
        driver.executeQuery(null, "SELECT COUNT(*) FROM $table", {
            QueryResult.Value(if (it.next().value) it.getLong(0) ?: 0 else 0)
        }, 0).value
}
