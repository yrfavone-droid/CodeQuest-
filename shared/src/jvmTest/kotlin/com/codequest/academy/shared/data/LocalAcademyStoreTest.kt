package com.codequest.academy.shared.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.cash.sqldelight.db.QueryResult
import com.codequest.academy.database.AppDatabase
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LocalAcademyStoreTest {
    private fun repository(): Pair<ProgressRepository, JdbcSqliteDriver> {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AppDatabase.Schema.create(driver)
        return ProgressRepository(driver) to driver
    }

    private fun manifest(): String {
        val file = File("../_incoming_ai_transformation/CodeQuest_AI_Transformation_Agent_Package/CURRICULUM/problem_manifest_10000.csv")
        assertTrue(file.isFile, "The supplied 10,000-slot manifest must remain available as an offline source.")
        return file.readText()
    }

    @Test
    fun importsSuppliedManifestAsLocalPlannedSlotsAndIndexesPublishedFoundationContent() {
        val (repository, _) = repository()

        val first = repository.installLocalAcademyContent(manifest())
        val second = repository.installLocalAcademyContent(manifest())

        assertTrue(first.changed)
        assertEquals(9_997, first.plannedProblemSlots)
        assertEquals(3, first.publishedProblems)
        assertEquals(12, first.tracks)
        assertEquals(3, first.lessons)
        assertEquals(5, first.books)
        assertEquals(20, first.knowledgeFiles)
        assertFalse(second.changed)
        assertTrue(repository.getAcademyLessons().any { it.title == "Python values, variables, and types" })
        assertTrue(repository.searchLocalAcademy("Python").isNotEmpty())
    }

    @Test
    fun realLocalAttemptCreatesEvidenceMasteryReviewAndMistakeRecord() {
        val (repository, driver) = repository()
        repository.installLocalAcademyContent(manifest())
        repository.createProfile("Offline learner")

        repository.recordAcademyAttempt("CQAI-00001", "{\"selected\":0}", correct = false, hintsUsed = 0, misconception = "ai-certainty")

        assertEquals(1L, count(driver, "SELECT COUNT(*) FROM AiAttempt"))
        assertEquals(1L, count(driver, "SELECT COUNT(*) FROM AiMastery"))
        assertEquals(1L, count(driver, "SELECT COUNT(*) FROM AiReviewQueue"))
        assertEquals(1L, count(driver, "SELECT COUNT(*) FROM AiMistakeNotebook"))
        assertEquals(1L, count(driver, "SELECT COUNT(*) FROM LocalAnalyticsEvent"))
    }

    private fun count(driver: JdbcSqliteDriver, sql: String): Long = driver.executeQuery(null, sql, {
        QueryResult.Value(if (it.next().value) it.getLong(0) ?: 0L else 0L)
    }, 0).value
}
