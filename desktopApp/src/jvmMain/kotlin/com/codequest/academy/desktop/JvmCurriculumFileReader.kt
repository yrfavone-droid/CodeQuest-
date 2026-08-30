package com.codequest.academy.desktop

import com.codequest.academy.shared.data.CurriculumFileReader
class JvmCurriculumFileReader : CurriculumFileReader {
    override fun readAsset(path: String): String {
        val resource = javaClass.classLoader.getResource("curriculum/$path")
            ?: throw IllegalArgumentException("Resource not found: $path")
        return resource.readText()
    }

}
