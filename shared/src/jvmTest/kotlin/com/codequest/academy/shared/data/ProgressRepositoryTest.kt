package com.codequest.academy.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.QueryResult
import com.codequest.academy.database.AppDatabase
import com.codequest.academy.shared.models.PathAsset
import com.codequest.academy.shared.ui.viewmodels.quizPassed
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.*

class ProgressRepositoryTest {
    private val json = Json { ignoreUnknownKeys = true }

    private fun assets(): List<PathAsset> {
        val directory = File("../curriculum_package/codequest_curriculum/assets/paths")
        assertTrue(directory.isDirectory, "Curriculum path directory must exist")
        return directory.listFiles { _, name -> name.endsWith(".json") }!!.sortedBy { it.name }.map {
            json.decodeFromString<PathAsset>(it.readText())
        }
    }

    private fun repository(): ProgressRepository {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return ProgressRepository(driver)
    }

    @Test
    fun curriculumHasExactProductionCounts() {
        val assets = assets()
        assertEquals(5, assets.map { it.track_id }.distinct().size)
        assertEquals(10, assets.size)
        assertEquals(50, assets.sumOf { it.levels.size })
        assertEquals(1650, assets.sumOf { path -> path.levels.sumOf { it.timeline_nodes.size } })
        assertTrue(assets.flatMap { it.levels }.all { it.timeline_nodes.size == 33 })
    }

    @Test
    fun seedingIsIdempotentAndNewProfileStartsAtZero() {
        val repository = repository()
        val curriculum = assets()
        val first = repository.seedCurriculum("1.0.0", curriculum)
        val second = repository.seedCurriculum("1.0.0", curriculum)
        assertTrue(first.changed)
        assertFalse(second.changed)
        assertEquals(5, repository.getPaths().map { it.track_id }.distinct().size)
        assertEquals(10, repository.getPaths().size)
        assertEquals(50, repository.getAllLevels().size)
        repository.createProfile("Test Learner")
        assertEquals(0, repository.getProgressSummary().completedNodes)
        assertTrue(listOf("web_development", "app_development", "cybersecurity", "problem_solving", "ai_machine_learning").all { repository.getTrackProgress(it) == 0f })
    }

    @Test
    fun requiredLearningChainUnlocksInOrder() {
        val repository = repository()
        repository.seedCurriculum("1.0.0", assets())
        repository.createProfile("Test Learner")
        val level = repository.getAllLevels().first { it.code == "FE-101" }
        val user = repository.getUserId()!!
        var states = repository.getLevelNodeStates(level.id)
        assertEquals("available", states["FE-101-DIAG"])
        assertEquals("locked", states["FE-101-CS"])
        repository.updateNodeState(user, "FE-101-DIAG", "completed")
        states = repository.getLevelNodeStates(level.id)
        assertEquals("available", states["FE-101-CS"])
        repository.updateNodeState(user, "FE-101-CS", "completed")
        assertEquals("available", repository.getLevelNodeStates(level.id)["FE-101-L01"])
        repository.updateNodeState(user, "FE-101-L01", "completed")
        assertEquals("available", repository.getLevelNodeStates(level.id)["FE-101-L01-PR"])
        repository.updateNodeState(user, "FE-101-L01-PR", "completed")
        assertEquals("available", repository.getLevelNodeStates(level.id)["FE-101-L01-CH"])
        repository.updateNodeState(user, "FE-101-L01-CH", "completed")
        assertEquals("available", repository.getLevelNodeStates(level.id)["FE-101-L02"])
    }

    @Test
    fun finalQuizAndProjectRespectPrerequisites() {
        val repository = repository()
        val curriculum = assets()
        repository.seedCurriculum("1.0.0", curriculum)
        repository.createProfile("Test Learner")
        val level = curriculum.flatMap { it.levels }.first { it.code == "FE-101" }
        val user = repository.getUserId()!!
        level.timeline_nodes.takeWhile { it.id != "FE-101-QUIZ" }.filter { it.required }.forEach { repository.updateNodeState(user, it.id, "completed") }
        var states = repository.getLevelNodeStates(level.id)
        assertEquals("available", states["FE-101-QUIZ"])
        assertEquals("locked", states["FE-101-PROJECT"])
        assertFalse(quizPassed(22, 30))
        assertTrue(quizPassed(23, 30))
        repository.updateNodeState(user, "FE-101-QUIZ", "completed")
        states = repository.getLevelNodeStates(level.id)
        assertEquals("available", states["FE-101-PROJECT"])
        repository.updateNodeState(user, "FE-101-PROJECT", "completed")
        assertEquals("available", repository.getLevelNodeStates(level.id)["FE-101-REFLECTION"])
    }

    @Test
    fun malformedCurriculumProducesAParseFailure() {
        assertFails { CurriculumLoader().parsePath("{\"schema_version\":\"1.0.0\"}") }
    }

    @Test
    fun legacyProfileSchemaIsMigratedWithoutSuppressingErrors() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        driver.execute(null, "CREATE TABLE UserProfile (user_id TEXT PRIMARY KEY, name TEXT NOT NULL, is_offline_profile INTEGER NOT NULL DEFAULT 1)", 0)

        ProgressRepository(driver)

        val columns = driver.executeQuery(null, "PRAGMA table_info(UserProfile)", {
            QueryResult.Value(buildSet {
                while (it.next().value) add(requireNotNull(it.getString(1)))
            })
        }, 0).value
        assertTrue(setOf("normalized_email", "password_hash", "password_salt", "email", "last_login_at").all(columns::contains))
    }

}
