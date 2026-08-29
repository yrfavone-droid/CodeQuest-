package com.codequest.academy.shared.runner

import java.io.File
import java.io.InputStream
import java.nio.file.Files
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class RunResult(val stdout: String, val stderr: String, val exitCode: Int, val timedOut: Boolean)

class CodeRunner {
    // This legacy runner starts a host process and is therefore disabled by
    // default. Local Academy releases must not execute learner code in the
    // Compose process until a verified local sandbox is packaged.
    private val enabled = System.getProperty("codequest.enableCodeRunner", "false").toBoolean()
    private val maxOutputBytes = 1_000_000

    // Explicit language allowlist
    private val allowedLanguages = setOf("javascript", "python")

    suspend fun runCode(language: String, code: String, timeoutMillis: Long = 5000): RunResult = withContext(Dispatchers.IO) {
        if (!enabled) {
            return@withContext RunResult("", "Execution is not available in this local release. Your files are saved locally; a verified no-network runtime is required before code can run.", -1, false)
        }
        if (language !in allowedLanguages) {
            return@withContext RunResult("", "Language $language is not allowed or supported.", -1, false)
        }

        val tempDir = Files.createTempDirectory("codequest_run_").toFile()
        try {
            val command = when (language) {
                "javascript" -> {
                    val file = File(tempDir, "script.js").apply { writeText(code) }
                    listOf("node", file.absolutePath)
                }
                "python" -> {
                    val file = File(tempDir, "script.py").apply { writeText(code) }
                    listOf("python", file.absolutePath)
                }
                "dart" -> {
                    val file = File(tempDir, "script.dart").apply { writeText(code) }
                    listOf("dart", "run", file.absolutePath)
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
            // Drain both pipes immediately. Waiting first can deadlock when a child fills an OS pipe.
            val stdoutFuture = CompletableFuture.supplyAsync { readLimited(process.inputStream) }
            val stderrFuture = CompletableFuture.supplyAsync { readLimited(process.errorStream) }

            val exited = process.waitFor(timeoutMillis.coerceIn(100, 30_000), TimeUnit.MILLISECONDS)
            if (!exited) {
                // Strict timeout & process-tree termination
                process.descendants().forEach { it.destroyForcibly() }
                process.destroyForcibly()
                process.waitFor(1, TimeUnit.SECONDS)
                return@withContext RunResult(stdoutFuture.getNow(""), "Execution timed out.\n${stderrFuture.getNow("")}", -1, true)
            }

            val stdout = stdoutFuture.get()
            val stderr = stderrFuture.get()

            RunResult(stdout, stderr, process.exitValue(), false)

        } catch (e: java.io.IOException) {
            RunResult("", "The $language runtime is not installed on this computer. Install it, then try again.", -1, false)
        } catch (e: Exception) {
            RunResult("", "Execution error: ${e.message}", -1, false)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    private fun readLimited(stream: InputStream): String {
        stream.use {
            val buffer = ByteArray(8192)
            val output = StringBuilder()
            var remaining = maxOutputBytes
            while (true) {
                val count = it.read(buffer)
                if (count == -1) break
                if (remaining > 0) {
                    val accepted = minOf(remaining, count)
                    output.append(buffer.decodeToString(0, accepted))
                    remaining -= accepted
                }
            }
            return if (remaining == 0) "$output\n[Output truncated at $maxOutputBytes bytes.]" else output.toString()
        }
    }
}
