package com.codequest.academy.shared.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.models.TrackIdentity
import com.codequest.academy.shared.runner.executeLocalCode
import com.codequest.academy.shared.ui.components.PrimaryButton
import com.codequest.academy.shared.ui.components.SecondaryButton
import com.codequest.academy.shared.ui.navigation.Navigation
import com.codequest.academy.shared.ui.theme.AppTypography
import com.codequest.academy.shared.ui.theme.DisplayStyle
import com.codequest.academy.shared.ui.theme.Theme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LevelProjectItem(
    val id: String,
    val levelCode: String,
    val title: String,
    val trackName: String,
    val trackIcon: String,
    val levelName: String,
    var language: String,
    var starterCode: String,
    val isCapstone: Boolean = false,
    var status: String = "available" // available, in_progress, submitted, locked
)

data class TrackCategoryGroup(
    val identity: TrackIdentity,
    val name: String,
    val icon: String,
    val levels: List<LevelProjectItem>,
    val capstone: LevelProjectItem
)

@Composable
fun CodeEditorScreen(navigation: Navigation, repository: ProgressRepository) {
    val coroutineScope = rememberCoroutineScope()
    // 1. Fetch Real Track & Level Data from Repository
    var trackGroups by remember { mutableStateOf<List<TrackCategoryGroup>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val languages = remember {
        listOf(
            "Python", "JavaScript", "TypeScript", "Java", "C++", "C#", "Go",
            "Rust", "Kotlin", "SQL", "HTML / CSS", "MATLAB", "R", "Scala",
            "Groovy", "PHP", "Swift", "Ruby"
        )
    }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            val allLevels = repository.getAllLevels()
            val drafts = repository.getProjectDrafts().associateBy { it.projectId }
            val userId = repository.getUserId()
            val allNodeProgress = if (userId != null) repository.getAllNodeProgress(userId).associate { it.node_id to it.state } else emptyMap()

            val groups = TrackIdentity.values().map { trackEnum ->
                val trackLevels = allLevels.filter { level ->
                    when (trackEnum) {
                        TrackIdentity.WEB_DEV -> level.code.startsWith("FE") || level.code.startsWith("BE")
                        TrackIdentity.APP_DEV -> level.code.startsWith("MB") || level.code.startsWith("DSK")
                        TrackIdentity.CYBERSECURITY -> level.code.startsWith("SEC") || level.code.startsWith("AUD")
                        TrackIdentity.PROBLEM_SOLVING -> level.code.startsWith("MATH") || level.code.startsWith("ALG")
                        TrackIdentity.AI_ML -> level.code.startsWith("ML") || level.code.startsWith("DL")
                    }
                }

                val levelItems = trackLevels.map { level ->
                    val projId = "${level.code}-PROJECT"
                    val draft = drafts[projId]
                    val nodeState = allNodeProgress[projId] ?: "available"
                    val status = when {
                        draft?.submitted == true -> "submitted"
                        draft != null -> "in_progress"
                        nodeState == "completed" -> "submitted"
                        nodeState == "locked" -> "locked"
                        else -> "available"
                    }

                    LevelProjectItem(
                        id = projId,
                        levelCode = level.code,
                        title = level.title,
                        trackName = trackEnum.title,
                        trackIcon = trackEnum.icon,
                        levelName = "${level.code} Level ${level.level_number}",
                        language = when (trackEnum) {
                            TrackIdentity.WEB_DEV -> "JavaScript"
                            TrackIdentity.APP_DEV -> "Kotlin"
                            TrackIdentity.CYBERSECURITY -> "Python"
                            TrackIdentity.PROBLEM_SOLVING -> "C++"
                            TrackIdentity.AI_ML -> "Python"
                        },
                        starterCode = generateStarterCode(level.code, level.title, trackEnum),
                        isCapstone = false,
                        status = status
                    )
                }

                val capstoneProjId = "${trackEnum.id}_capstone"
                val capstoneDraft = drafts[capstoneProjId]
                val capstoneItem = LevelProjectItem(
                    id = capstoneProjId,
                    levelCode = "CAPSTONE",
                    title = "Integrated ${trackEnum.title} Capstone",
                    trackName = trackEnum.title,
                    trackIcon = trackEnum.icon,
                    levelName = "🏆 Capstone Project",
                    language = if (trackEnum == TrackIdentity.WEB_DEV) "TypeScript" else "Python",
                    starterCode = generateCapstoneCode(trackEnum),
                    isCapstone = true,
                    status = if (capstoneDraft?.submitted == true) "submitted" else if (capstoneDraft != null) "in_progress" else "available"
                )

                TrackCategoryGroup(
                    identity = trackEnum,
                    name = trackEnum.title,
                    icon = trackEnum.icon,
                    levels = levelItems,
                    capstone = capstoneItem
                )
            }

            trackGroups = groups
            isLoading = false
        }
    }

    if (isLoading) {
        LoadingPage("Loading real curriculum tracks and levels…")
        return
    }

    // Selected item state
    var selectedItem by remember {
        mutableStateOf(
            trackGroups.firstOrNull()?.levels?.firstOrNull() ?: trackGroups.first().capstone
        )
    }

    var codeContent by remember(selectedItem.id) { mutableStateOf(selectedItem.starterCode) }
    var selectedLanguage by remember(selectedItem.id) { mutableStateOf(selectedItem.language) }
    var expandedTracks by remember { mutableStateOf(setOf(trackGroups.first().identity.id)) }
    var executionOutput by remember { mutableStateOf<String?>(null) }
    var isRunning by remember { mutableStateOf(false) }
    var showLangDropdown by remember { mutableStateOf(false) }
    var showSubmissionDialog by remember { mutableStateOf(false) }
    var viewMode by remember { mutableStateOf("SPLIT") }
    val mathAnalysis = remember(codeContent) { analyzeCodeMath(codeContent) }

    // Submission Confirmation / Feedback Dialog
    if (showSubmissionDialog) {
        AlertDialog(
            onDismissRequest = { showSubmissionDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("✅ ", fontSize = 20.sp)
                    Text("Draft saved locally", color = Theme.colors.textPrimary)
                }
            },
            text = {
                Column(Modifier.padding(vertical = 8.dp)) {
                    Text("Your work is saved on this device. This release does not claim execution, automated tests, or a grade.", style = AppTypography.body2, color = Theme.colors.textSecondary)
                    Spacer(Modifier.height(12.dp))
                    Box(
                        Modifier.fillMaxWidth().background(Theme.colors.surfaceTertiary, RoundedCornerShape(8.dp)).padding(12.dp)
                    ) {
                        Column {
                            Text("LOCAL DRAFT STATUS", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, color = Theme.colors.accentCyan))
                            Spacer(Modifier.height(6.dp))
                            Text("• Source saved to this device", style = AppTypography.caption, color = Theme.colors.textPrimary)
                            Text("• Execution is disabled until a verified local sandbox is packaged", style = AppTypography.caption, color = Theme.colors.textPrimary)
                            Text("• No grade or test result has been generated", style = AppTypography.caption, color = Theme.colors.warning)
                        }
                    }
                }
            },
            confirmButton = {
                PrimaryButton("Continue editing", onClick = { showSubmissionDialog = false })
            },
            backgroundColor = Theme.colors.surfaceSecondary
        )
    }

    Column(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
        // TOP BREADCRUMB & WORKSPACE HEADER
        Row(
            Modifier.fillMaxWidth().background(Theme.colors.surfacePrimary).padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(selectedItem.trackIcon, fontSize = 24.sp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(selectedItem.trackName, style = AppTypography.caption.copy(color = Theme.colors.textSecondary))
                        Text(" › ", style = AppTypography.caption.copy(color = Theme.colors.borderStrong))
                        Text(selectedItem.levelName, style = AppTypography.caption.copy(color = Theme.colors.accentCyan, fontWeight = FontWeight.Bold))
                    }
                    Text(selectedItem.title, style = AppTypography.h3.copy(color = Color.White))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                // Language Dropdown
                Box {
                    SecondaryButton(
                        text = "$selectedLanguage ▼",
                        onClick = { showLangDropdown = !showLangDropdown },
                        enabled = selectedItem.status != "submitted"
                    )
                    DropdownMenu(
                        expanded = showLangDropdown,
                        onDismissRequest = { showLangDropdown = false },
                        modifier = Modifier.background(Theme.colors.surfaceSecondary)
                    ) {
                        languages.forEach { lang ->
                            DropdownMenuItem(onClick = {
                                selectedLanguage = lang
                                selectedItem = selectedItem.copy(language = lang)
                                showLangDropdown = false
                            }) {
                                Text(lang, color = Color.White)
                            }
                        }
                    }
                }

                // Format Code
                SecondaryButton("Format", onClick = {
                    codeContent = codeContent.lines().joinToString("\n") { line ->
                        if (line.trim().startsWith("function") || line.trim().startsWith("def") || line.trim().endsWith("{")) line.trim()
                        else "    " + line.trim()
                    }
                }, enabled = selectedItem.status != "submitted")

                // Run Code
                PrimaryButton(if (isRunning) "Running…" else "Run ▶", onClick = {
                    coroutineScope.launch {
                        isRunning = true
                        val result = executeLocalCode(executionLanguage(selectedLanguage), codeContent)
                        executionOutput = buildString {
                            append("$ ${executionLanguage(selectedLanguage)} · exit ${result.exitCode}\n")
                            if (result.stdout.isNotBlank()) append(result.stdout)
                            if (result.stderr.isNotBlank()) {
                                if (result.stdout.isNotBlank()) append('\n')
                                append(result.stderr)
                            }
                            if (result.timedOut) append("\nExecution stopped after the safety timeout.")
                        }.trim()
                        isRunning = false
                    }
                }, isLoading = isRunning, enabled = !isRunning && selectedItem.status != "submitted", color = Theme.colors.accentCyan)

                // Status Action Buttons
                when (selectedItem.status) {
                    "available", "in_progress" -> {
                        PrimaryButton("Submit Project ✓", onClick = {
                            showSubmissionDialog = true
                        }, color = Color(0xFF7C5CFF))
                    }
                    "submitted" -> {
                        SecondaryButton("Unlock Edit", onClick = {
                            selectedItem.status = "in_progress"
                        })
                    }
                }

                // View Mode Toggles
                Row(
                    Modifier.clip(RoundedCornerShape(8.dp)).background(Theme.colors.surfaceTertiary).padding(2.dp)
                ) {
                    ViewModeButton("Split", viewMode == "SPLIT") { viewMode = "SPLIT" }
                    ViewModeButton("Code", viewMode == "EDITOR_ONLY") { viewMode = "EDITOR_ONLY" }
                    ViewModeButton("Math", viewMode == "VIZ_ONLY") { viewMode = "VIZ_ONLY" }
                }
            }
        }

        // MAIN WORKSPACE BODY: HIERARCHICAL SIDEBAR + EDITOR + MATH VIZ
        Row(Modifier.weight(1f).fillMaxWidth()) {
            // SIDEBAR: HIERARCHICAL ACCORDION TREE (Tracks -> Levels -> Capstones)
            Column(
                Modifier.width(280.dp).fillMaxHeight().background(Theme.colors.surfacePrimary)
                    .border(BorderStroke(1.dp, Theme.colors.borderDefault))
                    .padding(14.dp)
            ) {
                Text("CURRICULUM PROJECTS", style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, color = Theme.colors.accentCyan))
                Spacer(Modifier.height(12.dp))

                Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    trackGroups.forEach { group ->
                        val isExpanded = expandedTracks.contains(group.identity.id)

                        // 1. Collapsible Track Header
                        Box(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                                .background(if (isExpanded) Theme.colors.surfaceSecondary else Color.Transparent)
                                .border(BorderStroke(1.dp, Theme.colors.borderDefault), RoundedCornerShape(10.dp))
                                .clickable {
                                    expandedTracks = if (isExpanded) expandedTracks - group.identity.id else expandedTracks + group.identity.id
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(group.icon, fontSize = 16.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(group.name, style = AppTypography.button.copy(fontSize = 13.sp, color = Color.White))
                                }
                                Text(if (isExpanded) "▼" else "›", color = Theme.colors.accentCyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // 2. Levels List under Track (Hierarchical Tree)
                        AnimatedVisibility(visible = isExpanded) {
                            Column(Modifier.fillMaxWidth().padding(start = 12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                group.levels.forEach { levelItem ->
                                    val isSelected = levelItem.id == selectedItem.id
                                    Box(
                                        Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) Theme.colors.brandPrimary.copy(alpha = 0.25f) else Color.Transparent)
                                            .border(
                                                BorderStroke(if (isSelected) 1.5.dp else 0.dp, if (isSelected) Theme.colors.brandPrimary else Color.Transparent),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable {
                                                selectedItem = levelItem
                                                codeContent = levelItem.starterCode
                                                selectedLanguage = levelItem.language
                                                executionOutput = null
                                            }
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Text("📝 ", fontSize = 12.sp)
                                                Text(levelItem.levelName, style = AppTypography.caption.copy(color = if (isSelected) Theme.colors.accentCyan else Theme.colors.textSecondary, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal), maxLines = 1)
                                            }
                                            SidebarStatusBadge(levelItem.status)
                                        }
                                    }
                                }

                                // 🏆 Capstone Item
                                val isCapSelected = group.capstone.id == selectedItem.id
                                Box(
                                    Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp))
                                        .background(if (isCapSelected) Theme.colors.accentGold.copy(alpha = 0.25f) else Theme.colors.surfaceTertiary.copy(alpha = 0.5f))
                                        .border(
                                            BorderStroke(1.dp, if (isCapSelected) Theme.colors.accentGold else Theme.colors.borderDefault),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable {
                                            selectedItem = group.capstone
                                            codeContent = group.capstone.starterCode
                                            selectedLanguage = group.capstone.language
                                            executionOutput = null
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🏆 Capstone", style = AppTypography.caption.copy(color = Theme.colors.accentGold, fontWeight = FontWeight.Bold))
                                        SidebarStatusBadge(group.capstone.status)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // CENTER: CODE EDITOR AREA
            if (viewMode == "SPLIT" || viewMode == "EDITOR_ONLY") {
                Column(
                    Modifier.weight(if (viewMode == "SPLIT") 1.4f else 2f).fillMaxHeight()
                        .background(Color(0xFF0F0F1E))
                        .padding(16.dp)
                ) {
                    // Editor Status Bar
                    Row(
                        Modifier.fillMaxWidth().background(Color(0xFF1A1A2E)).padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("📜 ${selectedItem.title} ($selectedLanguage)", style = AppTypography.caption.copy(color = Color.White))
                        Text(
                            "Lines: ${codeContent.lines().size} | Chars: ${codeContent.length}",
                            style = AppTypography.caption.copy(color = Theme.colors.textSecondary)
                        )
                    }

                    // Main Code Text Field with Line Numbers Margin & Syntax Color Tokens
                    Row(Modifier.weight(1f).fillMaxWidth().border(1.dp, Theme.colors.borderDefault)) {
                        // Line Numbers Column
                        Column(
                            Modifier.width(44.dp).fillMaxHeight().background(Color(0xFF1A1A2E)).padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val lineCount = codeContent.lines().size
                            (1..maxOf(lineCount, 15)).forEach { lineNum ->
                                Text(
                                    text = "$lineNum",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFF808080),
                                    modifier = Modifier.height(20.dp)
                                )
                            }
                        }

                        // Code Content Text Editor
                        OutlinedTextField(
                            value = codeContent,
                            onValueChange = {
                                if (selectedItem.status != "submitted") {
                                    codeContent = it
                                    selectedItem.starterCode = it
                                }
                            },
                            readOnly = selectedItem.status == "submitted",
                            modifier = Modifier.weight(1f).fillMaxHeight(),
                            textStyle = AppTypography.body2.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color(0xFFE0E0E0)
                            ),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                backgroundColor = Color(0xFF0F0F1E),
                                focusedBorderColor = Color.Transparent,
                                unfocusedBorderColor = Color.Transparent
                            )
                        )

                        // Mini Map Preview Column
                        Column(
                            Modifier.width(60.dp).fillMaxHeight().background(Color(0xFF16213E).copy(alpha = 0.5f)).padding(4.dp)
                        ) {
                            codeContent.lines().take(30).forEach { line ->
                                Box(
                                    Modifier.fillMaxWidth(minOf(1f, line.trim().length / 30f + 0.1f))
                                        .height(3.dp)
                                        .padding(vertical = 1.dp)
                                        .background(
                                            when {
                                                "def" in line || "function" in line || "class" in line -> Color(0xFF7C5CFF)
                                                "return" in line || "if" in line || "for" in line -> Color(0xFF00D9FF)
                                                "//" in line || "#" in line -> Color(0xFF808080)
                                                else -> Color(0xFFE0E0E0).copy(alpha = 0.4f)
                                            }
                                        )
                                )
                            }
                        }
                    }

                    // Execution Console Output Panel
                    if (executionOutput != null) {
                        Spacer(Modifier.height(8.dp))
                        Box(
                            Modifier.fillMaxWidth().height(90.dp).background(Color(0xFF16213E))
                                .border(1.dp, Theme.colors.accentCyan, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = executionOutput!!,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Theme.colors.accentLime
                            )
                        }
                    }
                }
            }

            // RIGHT: REAL-TIME CODE TO MATH VISUALIZATION PANEL (40% SPLIT VIEW)
            if (viewMode == "SPLIT" || viewMode == "VIZ_ONLY") {
                Column(
                    Modifier.weight(1f).fillMaxHeight().background(Theme.colors.surfaceSecondary)
                        .border(BorderStroke(1.dp, Theme.colors.borderDefault))
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("∫ ", style = AppTypography.h2.copy(color = Theme.colors.accentGold))
                        Text("Real-Time Math Visualization", style = AppTypography.h3.copy(color = Color.White))
                    }
                    Text("Mathematical Formalisms & Complexity Analysis", style = AppTypography.caption, color = Theme.colors.textSecondary)

                    Spacer(Modifier.height(20.dp))

                    // 1. Math Function Signature Box
                    MathVizCard("FUNCTION FORMALISM") {
                        Text(
                            text = when {
                                mathAnalysis.functionCount > 0 -> "f(input) → output  ·  ${mathAnalysis.functionCount} function(s)"
                                else -> "f(x_1, x_2, …, x_n) → evaluated expression"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Theme.colors.accentGold
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Domain Mapping: ℝⁿ × ℝᵐ → ℝ", style = AppTypography.caption, color = Theme.colors.textSecondary)
                    }

                    Spacer(Modifier.height(14.dp))

                    // 2. Loop Summation Notation Box
                    MathVizCard("SUMMATION & RECURRENCE") {
                        Text(
                            text = when {
                                mathAnalysis.isRecursive -> "T(n) = T(n − 1) + O(1)"
                                mathAnalysis.loopCount > 0 -> "∑ loop operations  ·  ${mathAnalysis.loopCount} loop(s) detected"
                                else -> "T(n) = O(1)"
                            },
                            fontFamily = FontFamily.Monospace,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Theme.colors.accentCyan
                        )
                        Spacer(Modifier.height(6.dp))
                        Text("Recurrence Relation & Partition Bounds", style = AppTypography.caption, color = Theme.colors.textSecondary)
                    }

                    Spacer(Modifier.height(14.dp))

                    // 3. Algorithm Complexity Analysis Box
                    MathVizCard("ALGORITHM COMPLEXITY ANALYSIS") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text("Time Complexity:", style = AppTypography.caption, color = Theme.colors.textSecondary)
                                Text(mathAnalysis.timeComplexity, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Theme.colors.accentLime)
                            }
                            Column {
                                Text("Space Complexity:", style = AppTypography.caption, color = Theme.colors.textSecondary)
                                Text(mathAnalysis.spaceComplexity, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Theme.colors.accentCyan)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 4. Interactive Mathematical Function Graph Canvas
                    MathVizCard("MATHEMATICAL FUNCTION GRAPH") {
                        Box(
                            Modifier.fillMaxWidth().height(120.dp).background(Color(0xFF0F0F1E))
                                .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Measured growth model · ${mathAnalysis.timeComplexity}", style = AppTypography.caption, color = Theme.colors.accentCyan)
                                Spacer(Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
                                    mathAnalysis.sampleHeights.forEach { h ->
                                        Box(
                                            Modifier.width(12.dp).height(h.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(Theme.colors.brandPrimary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MathVizCard(title: String, content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfacePrimary)
            .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp)).padding(16.dp)
    ) {
        Text(title, style = AppTypography.caption.copy(fontWeight = FontWeight.Bold, color = Theme.colors.accentCyan))
        Spacer(Modifier.height(6.dp))
        content()
    }
}

@Composable
private fun SidebarStatusBadge(status: String) {
    val (label, bg, text) = when (status) {
        "submitted" -> Triple("Submitted ✓", Theme.colors.accentLime.copy(alpha = 0.2f), Theme.colors.accentLime)
        "in_progress" -> Triple("In Progress", Theme.colors.surfaceTertiary, Theme.colors.accentCyan)
        "locked" -> Triple("🔒", Color.Transparent, Theme.colors.textMuted)
        else -> Triple("Ready", Color.Transparent, Theme.colors.textMuted)
    }
    Box(
        Modifier.clip(RoundedCornerShape(4.dp)).background(bg).padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, style = AppTypography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = text)
    }
}

@Composable
private fun ViewModeButton(text: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(6.dp))
            .background(if (active) Theme.colors.brandPrimary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, style = AppTypography.caption.copy(fontSize = 12.sp, color = if (active) Color.White else Theme.colors.textSecondary))
    }
}

private fun executionLanguage(displayLanguage: String): String = when (displayLanguage) {
    "JavaScript" -> "javascript"
    "Python" -> "python"
    else -> displayLanguage.lowercase().replace(" / ", "-").replace(" ", "-")
}

private data class MathAnalysis(
    val loopCount: Int,
    val functionCount: Int,
    val isRecursive: Boolean,
    val timeComplexity: String,
    val spaceComplexity: String,
    val sampleHeights: List<Int>
)

private fun analyzeCodeMath(code: String): MathAnalysis {
    val lines = code.lines()
    val loopCount = lines.count { Regex("\\b(for|while|repeat)\\b").containsMatchIn(it) }
    val functionCount = lines.count { Regex("\\b(fun|function|def)\\b").containsMatchIn(it) }
    val isRecursive = Regex("\\b(recurs|quicksort|mergesort)\\b", RegexOption.IGNORE_CASE).containsMatchIn(code)
    val exponent = when {
        isRecursive -> 1
        loopCount >= 3 -> 3
        loopCount == 2 -> 2
        loopCount == 1 -> 1
        else -> 0
    }
    val time = if (isRecursive) "O(n log n)" else when (exponent) { 3 -> "O(n³)"; 2 -> "O(n²)"; 1 -> "O(n)"; else -> "O(1)" }
    val samples = (1..6).map { n ->
        val raw = if (exponent == 0) 1 else (1..exponent).fold(n) { value, _ -> value * n }
        (10 + raw * (100 / (6 * 6 * 6))).coerceIn(10, 108)
    }
    return MathAnalysis(loopCount, functionCount, isRecursive, time, if (loopCount > 1) "O(n)" else "O(1)", samples)
}

private fun generateStarterCode(code: String, title: String, track: TrackIdentity): String = when (track) {
    TrackIdentity.WEB_DEV -> """// $title ($code)
function executeLevelChallenge(inputData) {
    console.log("Processing $code...");
    let result = inputData.map(x => x * 2);
    return result;
}"""

    TrackIdentity.APP_DEV -> """// $title ($code)
fun buildWidgetTree(): State {
    val items = listOf("View 1", "View 2", "View 3")
    return State(items = items)
}"""

    TrackIdentity.CYBERSECURITY -> """# $title ($code)
def audit_security_policy(assets):
    vulnerabilities = []
    for item in assets:
        if not item.get("encrypted"):
            vulnerabilities.append(item)
    return vulnerabilities"""

    TrackIdentity.PROBLEM_SOLVING -> """// $title ($code)
#include <iostream>
#include <vector>

std::vector<int> solveInvariant(const std::vector<int>& arr) {
    std::vector<int> res;
    for (int x : arr) {
        if (x % 2 == 0) res.push_back(x);
    }
    return res;
}"""

    TrackIdentity.AI_ML -> """# $title ($code)
import numpy as np

def compute_loss(y_true, y_pred):
    return np.mean((y_true - y_pred) ** 2)"""
}

private fun generateCapstoneCode(track: TrackIdentity): String = when (track) {
    TrackIdentity.WEB_DEV -> """// Full-Stack Web Development Capstone
async function processOrder(orderId, items) {
    const total = items.reduce((sum, item) => sum + item.price, 0);
    return { orderId, total, status: "PROCESSED" };
}"""

    TrackIdentity.APP_DEV -> """// Cross-Platform App Capstone
class AppStateNotifier : Observable() {
    fun updateState(newState: AppState) {
        notifyListeners(newState)
    }
}"""

    TrackIdentity.CYBERSECURITY -> """# Security Audit & Defense Capstone
def run_penetration_audit(network_grid):
    return {"status": "SECURE", "score": 100}"""

    TrackIdentity.PROBLEM_SOLVING -> """// Algorithmic Math Capstone
#include <vector>
int optimizePath(const std::vector<std::vector<int>>& grid) {
    return 42;
}"""

    TrackIdentity.AI_ML -> """# AI System & Model Training Capstone
def train_model(X, y, epochs=100):
    weights = np.random.randn(X.shape[1])
    return weights"""
}
