package com.codequest.academy.shared.data

import com.codequest.academy.shared.models.PathAsset
import kotlinx.serialization.json.Json

class CurriculumLoader {
    private val json = Json { ignoreUnknownKeys = true }
    
    fun parsePath(jsonString: String): PathAsset {
        val asset = json.decodeFromString<PathAsset>(jsonString)
        validate(asset)
        return asset
    }
    
    private fun validate(asset: PathAsset) {
        require(asset.levels.size == 5) { "Each path must have exactly 5 levels" }
        require(asset.levels.all { it.code.isNotBlank() }) { "Level codes must not be blank" }
        
        // Ensure ID uniqueness
        val levelIds = asset.levels.map { it.id }
        require(levelIds.size == levelIds.toSet().size) { "Level IDs must be unique" }
        
        // Other structural requirements as per prompt:
        // "Ten practice questions per practice, one challenge per practice, four activities per challenge"
        // Validated at deeper levels once those objects are fully hydrated.
    }
}
