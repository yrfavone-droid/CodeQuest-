package com.codequest.academy.shared.runner

actual suspend fun executeLocalCode(language: String, code: String, timeoutMillis: Long): LocalExecutionResult {
    val result = CodeRunner().runCode(language, code, timeoutMillis)
    val unavailable = result.exitCode == -1 && result.stderr.contains("not installed", ignoreCase = true)
    return LocalExecutionResult(result.stdout, result.stderr, result.exitCode, result.timedOut, !unavailable)
}
