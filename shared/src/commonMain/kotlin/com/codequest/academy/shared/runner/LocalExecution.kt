package com.codequest.academy.shared.runner

data class LocalExecutionResult(
    val stdout: String,
    val stderr: String,
    val exitCode: Int,
    val timedOut: Boolean,
    val languageAvailable: Boolean
)

/** Runs code only through explicitly supported local runtimes. */
expect suspend fun executeLocalCode(language: String, code: String, timeoutMillis: Long = 5_000): LocalExecutionResult
