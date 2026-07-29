package com.codequest.academy.shared.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement

@Serializable
data class PathAsset(
    val schema_version: String,
    val track_id: String,
    val path: Path,
    val levels: List<Level>
)

@Serializable
data class Path(
    val id: String,
    val title: String,
    val track_id: String,
    val level_codes: List<String>,
    val runtime_policy: String? = null,
    val synthesized_fallback_allowed: Boolean = false,
    val initial_progress: Int = 0,
    val progress_migration_policy: String? = null
)

@Serializable
data class Level(
    val id: String,
    val track_id: String,
    val path_id: String,
    val code: String,
    val level_number: Int,
    val title: String,
    val difficulty: String,
    val goal: String,
    val estimated_days: Int,
    val estimated_total_minutes: Int,
    val estimated_project_minutes: Int,
    val prerequisite_skill_ids: List<String>,
    val learning_objectives: List<String>,
    val skill_ids: List<String>,
    val concept_titles: List<String>,
    val mastery_tags: List<String>,
    val unlock_rule: UnlockRule? = null,
    val weekly_plan: List<WeeklyPlan>? = null,
    val diagnostic: Diagnostic? = null,
    val cheat_sheet: JsonElement? = null,
    val lessons: List<JsonElement> = emptyList(),
    val practices: List<JsonElement> = emptyList(),
    val challenges: List<JsonElement> = emptyList(),
    val mixed_reviews: List<JsonElement> = emptyList(),
    val adaptive_review: JsonElement? = null,
    val final_quiz: JsonElement? = null,
    val project: JsonElement? = null,
    val project_reflection: JsonElement? = null,
    val optional_mastery_challenge: JsonElement? = null,
    val timeline_nodes: List<TimelineNode> = emptyList(),
    val next_level_unlock: JsonElement? = null
)

@Serializable
data class TimelineNode(
    val id: String,
    val type: String,
    val content_ref: String,
    val required: Boolean = true,
    val order: Int
)

@Serializable
data class UnlockRule(
    val requires: List<String>,
    val condition: String
)

@Serializable
data class WeeklyPlan(
    val day: Int,
    val activities: List<String>
)

@Serializable
data class Diagnostic(
    val id: String,
    val title: String,
    val estimated_minutes: Int,
    val purpose: String,
    val attempt_policy: String,
    val questions: List<Question>
)

@Serializable
data class Question(
    val id: String,
    val track_id: String,
    val path_id: String,
    val level_id: String,
    val level_code: String,
    val lesson_id: String? = null,
    val skill_id: String? = null,
    val type: String,
    val difficulty: Int,
    val prompt: String,
    val misconception_tags: List<String> = emptyList(),
    val review_tags: List<String> = emptyList(),
    val xp: Int,
    val interaction_data: JsonElement? = null,
    val options: List<Option> = emptyList(),
    val correct_answer: JsonElement? = null,
    val explanation: String? = null,
    val hints: List<String> = emptyList()
)

@Serializable
data class Option(
    val id: String,
    val text: String,
    val is_correct: Boolean = false,
    val feedback: String? = null
)

// We'll expand these as we implement more specific parsers for lessons, etc.
