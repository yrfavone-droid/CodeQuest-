package com.codequest.academy.desktop

import com.codequest.academy.shared.data.CurriculumFileReader
class JvmCurriculumFileReader : CurriculumFileReader {
    override fun readAsset(path: String): String {
        val resource = javaClass.classLoader.getResource("curriculum/$path")
            ?: throw IllegalArgumentException("Resource not found: $path")
        return resource.readText()
    }

    override fun listPaths(): List<String> {
        val catalogJson = readAsset("curriculum_catalog.json")
        val regex = "\"asset\"\\s*:\\s*\"(paths/[^\"]+\\.json)\"".toRegex()
        val paths = regex.findAll(catalogJson).map { it.groupValues[1] }.toList()
        require(paths.size == 10) { "Curriculum catalog must reference exactly 10 path assets; found ${paths.size}" }
        return paths
    }
}
