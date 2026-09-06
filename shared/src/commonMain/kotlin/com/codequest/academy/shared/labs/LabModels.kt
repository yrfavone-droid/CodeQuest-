package com.codequest.academy.shared.labs

import kotlinx.serialization.Serializable

@Serializable data class PythonTest(val name: String, val code: String, val hidden: Boolean = false)
@Serializable data class PythonRequest(val source: String, val stdin: String = "", val tests: List<PythonTest> = emptyList())
@Serializable data class PythonTestResult(val name: String, val passed: Boolean, val message: String = "")
@Serializable data class PythonResult(val status: String, val output: String = "", val error: String = "", val tests: List<PythonTestResult> = emptyList())
data class PythonExercise(val id: String, val lessonId: String, val title: String, val objective: String, val statement: String, val starter: String, val solution: String, val tests: List<PythonTest>, val hints: List<String>, val explanation: String)
data class LabProject(val id: String, val name: String, val source: String, val updatedAt: Long)
data class LabSummary(val attempts: Int = 0, val correct: Int = 0, val mastery: Int = 0)

expect object PythonRunner {
    fun available(): Boolean
    suspend fun run(request: PythonRequest): PythonResult
    fun stop()
    fun chooseOpen(): String?
    fun chooseSave(source: String): Boolean
}
