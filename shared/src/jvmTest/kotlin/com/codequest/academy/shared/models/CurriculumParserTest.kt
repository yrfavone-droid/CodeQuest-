package com.codequest.academy.shared.models

import kotlinx.serialization.json.Json
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CurriculumParserTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testParseAllPaths() {
        val pathsDir = File("../curriculum_package/codequest_curriculum/assets/paths")
        assertTrue(pathsDir.exists(), "Paths directory must exist")
        
        val jsonFiles = pathsDir.listFiles { _, name -> name.endsWith(".json") }
        assertTrue(jsonFiles != null && jsonFiles.isNotEmpty(), "Should find JSON files")
        
        var parsedPaths = 0
        jsonFiles.forEach { file ->
            val content = file.readText()
            val asset = json.decodeFromString<PathAsset>(content)
            
            assertEquals("1.0.0", asset.schema_version)
            assertTrue(asset.levels.size == 5, "Every path should have exactly 5 levels")
            
            parsedPaths++
        }
        
        assertEquals(10, parsedPaths, "Should parse exactly 10 path assets")
    }
}
