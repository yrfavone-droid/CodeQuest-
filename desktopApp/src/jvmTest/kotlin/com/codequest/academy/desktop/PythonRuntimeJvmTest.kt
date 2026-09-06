package com.codequest.academy.desktop

import com.codequest.academy.shared.labs.*
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.*

/** Real packaged runtime + AppContainer; no mocked execution result. Windows release gate. */
class PythonRuntimeJvmTest {
    @Test fun everyExerciseRunsRealPublicAndHiddenTests() = runBlocking {
        val previousHome = System.getProperty("user.home")
        val profile = Files.createTempDirectory("nous-python-runtime-test-").toFile()
        try {
            System.setProperty("user.home", profile.absolutePath)
            assertNotNull(javaClass.getResource("/python-lab/runtime.zip"), "Bundled Python is missing")
            assertNotNull(javaClass.getResource("/font/jetbrains_mono_variable.ttf"), "Bundled editor font is missing")
            assertTrue(PythonRunner.available(), "Windows x64 runtime must be available for this release")
            for (exercise in PythonExercises.all) {
                val result = PythonRunner.run(PythonRequest(exercise.solution, tests = exercise.tests))
                assertEquals("success", result.status, "${exercise.id}: ${result.error}")
                assertEquals(exercise.tests.size, result.tests.size, exercise.id)
                assertTrue(result.tests.all { it.passed }, "${exercise.id}: ${result.tests}")
                val incorrect = PythonRunner.run(PythonRequest("pass", tests = exercise.tests))
                assertEquals("test_failure", incorrect.status, exercise.id)
                assertTrue(incorrect.tests.any { !it.passed }, exercise.id)
            }
            val syntax = PythonRunner.run(PythonRequest("if :"))
            assertEquals("syntax_error", syntax.status)
            assertTrue(profile.resolve(".nous-ai-academy/lab-runs").listFiles().orEmpty().isEmpty(), "Run workspaces must be cleaned")
        } finally {
            System.setProperty("user.home", previousHome)
            // Retain this generated profile as diagnostic evidence; never touch an existing profile.
        }
    }
}
