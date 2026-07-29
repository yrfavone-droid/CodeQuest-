package com.codequest.academy.shared.models

import androidx.compose.ui.graphics.Color
import com.codequest.academy.shared.ui.theme.TrackColors

enum class TrackIdentity(
    val id: String,
    val title: String,
    val description: String,
    val primaryColor: Color,
    val softColor: Color,
    val icon: String
) {
    WEB_DEV(
        "web_development",
        "Web Development",
        "Build complete web experiences by understanding pages as trees, layouts as constraints, interfaces as state transitions, and servers as request-processing systems.",
        TrackColors.WebDev,
        TrackColors.WebDevSoft,
        "🌐"
    ),
    APP_DEV(
        "app_development",
        "App Development",
        "Design mobile applications through widget trees, component relationships, navigation graphs, state transitions, asynchronous timelines, and layered data flow.",
        TrackColors.AppDev,
        TrackColors.AppDevSoft,
        "📱"
    ),
    CYBERSECURITY(
        "cybersecurity",
        "Cybersecurity",
        "Understand security through assets, threats, vulnerabilities, impact, controls, trust boundaries, evidence, detection, authorization, and remediation.",
        TrackColors.Cyber,
        TrackColors.CyberSoft,
        "🛡️"
    ),
    PROBLEM_SOLVING(
        "problem_solving",
        "Problem Solving",
        "Solve difficult problems by modeling data, tracing invariants, recognizing patterns, comparing brute-force and optimized approaches, and explaining correctness and complexity.",
        TrackColors.ProblemSolving,
        TrackColors.ProblemSolvingSoft,
        "🧩"
    ),
    AI_ML(
        "ai_machine_learning",
        "AI and Machine Learning",
        "Understand intelligent systems as data representations, functions, vectors, distributions, losses, transformations, evaluation decisions, and responsible production systems.",
        TrackColors.AiMl,
        TrackColors.AiMlSoft,
        "🧠"
    );

    companion object {
        fun fromId(id: String): TrackIdentity? = values().find {
            it.id == id || it.name.equals(id, ignoreCase = true)
        }
    }
}
