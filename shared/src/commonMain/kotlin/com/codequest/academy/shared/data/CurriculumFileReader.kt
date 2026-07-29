package com.codequest.academy.shared.data

interface CurriculumFileReader {
    fun readAsset(path: String): String
    fun listPaths(): List<String>
}
