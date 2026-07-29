package com.codequest.academy.shared.runner

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RunResult(val stdout: String, val stderr: String, val exitCode: Int, val timedOut: Boolean)

class CodeRunner {
    
    // Explicit language allowlist
    private val allowedLanguages = setOf("javascript", "python", "dart")

    suspend fun runCode(language: String, code: String, timeoutMillis: Long = 5000): RunResult = withContext(Dispatchers.IO) {
        if (language !in allowedLanguages) {
            return@withContext RunResult("", "Language $language is not allowed or supported.", -1, false)
        }

        val tempDir = Files.createTempDirectory("codequest_run_").toFile()
        try {
            val (command, sourceFile) = when (language) {
                "javascript" -> {
                    val file = File(tempDir, "script.js").apply { writeText(code) }
                    listOf("node", file.absolutePath) to file
                }
                "python" -> {
                    val file = File(tempDir, "script.py").apply { writeText(code) }
                    listOf("python", file.absolutePath) to file
                }
                "dart" -> {
                    val file = File(tempDir, "script.dart").apply { writeText(code) }
                    listOf("dart", "run", file.absolutePath) to file
                }
                else -> throw IllegalStateException("Unsupported language")
            }

            val processBuilder = ProcessBuilder(command)
                .directory(tempDir)
                
            // Environment variable filtering (clear all except PATH, etc.)
            val env = processBuilder.environment()
            val safeEnv = mapOf("PATH" to (env["PATH"] ?: ""))
            env.clear()
            env.putAll(safeEnv)

            val process = processBuilder.start()

            val exited = process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)
            if (!exited) {
                // Strict timeout & process-tree termination
                process.descendants().forEach { it.destroyForcibly() }
                process.destroyForcibly()
                return@withContext RunResult("", "Execution timed out.", -1, true)
            }

            val stdout = process.inputStream.bufferedReader().use { it.readText() }
            val stderr = process.errorStream.bufferedReader().use { it.readText() }

            RunResult(stdout, stderr, process.exitValue(), false)

        } catch (e: Exception) {
            RunResult("", "Execution error: ${e.message}", -1, false)
        } finally {
            tempDir.deleteRecursively()
        }
    }
}
