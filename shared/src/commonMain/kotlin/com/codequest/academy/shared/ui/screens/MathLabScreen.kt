package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.labs.*
import com.codequest.academy.shared.ui.components.*
import com.codequest.academy.shared.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*
import kotlin.random.Random

private val MathTools = listOf("Practice", "Graph", "Matrix / vector", "Statistics", "Probability")

@Composable fun MathLabScreen(repository: ProgressRepository) {
 var tool by remember { mutableStateOf(repository.getSetting("math_tool", "Practice").let { if (it in MathTools) it else "Practice" }) }
 var pendingGraphExpr by remember { mutableStateOf<String?>(null) }

 Column(
  Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(horizontal = 24.dp, vertical = 18.dp),
  verticalArrangement = Arrangement.spacedBy(14.dp)
 ) {
  Row(
   Modifier.fillMaxWidth(),
   verticalAlignment = Alignment.CenterVertically,
   horizontalArrangement = Arrangement.SpaceBetween
  ) {
   Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text("Math Lab", style = DisplayStyle, color = Theme.colors.textPrimary)
    Text(
     "Interactive graphing calculator, linear algebra studio, statistical analysis, probability distributions & guided mastery",
     style = AppTypography.caption,
     color = Theme.colors.textSecondary
    )
   }
  }
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   MathTools.forEach { name ->
    val select = { tool = name; repository.setSetting("math_tool", name) }
    if (tool == name) PrimaryButton(name, select) else SecondaryButton(name, select)
   }
  }
  when (tool) {
   "Graph" -> GraphingCalculator(
    repository = repository,
    injectedExpression = pendingGraphExpr,
    onInjectedConsumed = { pendingGraphExpr = null },
    modifier = Modifier.weight(1f).fillMaxWidth()
   )
   "Practice" -> Column(
    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
   ) {
    MathPractice(
     repository = repository,
     onOpenInGraph = { expr ->
      pendingGraphExpr = expr
      tool = "Graph"
      repository.setSetting("math_tool", "Graph")
     }
    )
   }
   "Matrix / vector" -> Column(
    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
   ) {
    MatrixVectorWorkbench(repository)
   }
   "Statistics" -> Column(
    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
   ) {
    StatisticsWorkbench(repository)
   }
   else -> Column(
    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
   ) {
    ProbabilityWorkbench(repository)
   }
  }
 }
}

// ───────────────────────────── Shared UI Cards & Chips ─────────────────────────────

@Composable private fun WorkbenchCard(
 title: String,
 subtitle: String? = null,
 modifier: Modifier = Modifier,
 headerEnd: (@Composable RowScope.() -> Unit)? = null,
 content: @Composable ColumnScope.() -> Unit
) {
 Column(
  modifier
   .clip(RoundedCornerShape(16.dp))
   .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(16.dp))
   .background(Theme.colors.surfacePrimary)
   .padding(18.dp),
  verticalArrangement = Arrangement.spacedBy(12.dp)
 ) {
  Row(
   Modifier.fillMaxWidth(),
   verticalAlignment = Alignment.CenterVertically,
   horizontalArrangement = Arrangement.SpaceBetween
  ) {
   Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
    Text(title, style = AppTypography.h3, color = Theme.colors.textPrimary)
    if (subtitle != null) Text(subtitle, style = AppTypography.caption, color = Theme.colors.textSecondary)
   }
   if (headerEnd != null) Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, content = headerEnd)
  }
  content()
 }
}

@Composable private fun MetricTile(label: String, value: String, detail: String? = null, modifier: Modifier = Modifier) {
 Column(
  modifier
   .clip(RoundedCornerShape(12.dp))
   .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp))
   .background(Theme.colors.surfaceSecondary)
   .padding(horizontal = 14.dp, vertical = 10.dp),
  verticalArrangement = Arrangement.spacedBy(2.dp)
 ) {
  Text(label, style = AppTypography.caption, color = Theme.colors.textSecondary)
  SelectionContainer {
   Text(value, style = CodeStyle.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold), color = Theme.colors.textPrimary)
  }
  if (detail != null) Text(detail, style = AppTypography.caption.copy(fontSize = 11.sp), color = Theme.colors.textMuted)
 }
}

@Composable private fun SmallChip(
 text: String,
 active: Boolean = false,
 onClick: () -> Unit
) {
 val bg = if (active) Theme.colors.brandPrimary else Theme.colors.surfaceSecondary
 val fg = if (active) (if (Theme.colors.isDark) Color.Black else Color.White) else Theme.colors.textPrimary
 val border = if (active) Theme.colors.brandPrimary else Theme.colors.borderDefault
 Box(
  Modifier
   .clip(RoundedCornerShape(10.dp))
   .border(1.dp, border, RoundedCornerShape(10.dp))
   .background(bg)
   .clickable(onClick = onClick)
   .padding(horizontal = 12.dp, vertical = 7.dp)
 ) {
  Text(text, style = AppTypography.caption, color = fg, fontWeight = FontWeight.SemiBold)
 }
}

// ───────────────────────────── 1. Practice Workbench ─────────────────────────────

@Composable private fun MathPractice(
 repository: ProgressRepository,
 onOpenInGraph: (String) -> Unit
) {
 var category by remember { mutableStateOf(MathTopicCategory.All) }
 var topic by remember {
  mutableStateOf(runCatching { MathTopic.valueOf(repository.getSetting("math_topic", "Algebra")) }.getOrDefault(MathTopic.Algebra))
 }
 var seed by remember { mutableStateOf(repository.getSetting("math_seed", "1").toIntOrNull() ?: 1) }
 var difficulty by remember { mutableStateOf(repository.getSetting("math_difficulty", "1").toIntOrNull()?.coerceIn(1, 3) ?: 1) }
 var mixed by remember { mutableStateOf(false) }
 var reasoning by remember { mutableStateOf(false) }
 val filteredTopics = remember(category) {
  if (category == MathTopicCategory.All) MathTopic.values().toList()
  else MathTopic.values().filter { it.category == category }
 }
 val problem = remember(topic, difficulty, seed, reasoning) {
  MathEngine.generate(topic, difficulty, seed).let {
   if (reasoning) it.copy(
    id = it.id + ":reasoning",
    statement = "Explain and justify the method for this problem:\n" + it.statement,
    type = MathAnswerType.Explanation
   ) else it
  }
 }
 var answer by remember(problem.id) { mutableStateOf(repository.getSetting("math_answer:${problem.id}", "")) }
 var hints by remember(problem.id) { mutableStateOf(0) }
 var verdict by remember(problem.id) { mutableStateOf<MathVerdict?>(null) }
 var start by remember(problem.id) { mutableStateOf(System.currentTimeMillis()) }
 var reviewSaved by remember(problem.id) { mutableStateOf("") }
 var summary by remember(topic) { mutableStateOf(repository.labs.summary("math", topic.name)) }
 var scratchpad by remember { mutableStateOf("2 * 3^2 + 1") }

 fun submit() {
  if (verdict != null || answer.trim().length < (if (reasoning) 10 else 1)) return
  val result = MathEngine.check(problem, answer)
  verdict = result
  if (!reasoning) {
   repository.labs.record("math", topic.name, problem.id, answer, result.message, result.correct, hints, System.currentTimeMillis() - start)
   summary = repository.labs.summary("math", topic.name)
  }
 }

 // Category & Controls Row
 Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
   MathTopicCategory.values().forEach { cat ->
    SmallChip(cat.title, active = category == cat) {
     category = cat
     val list = if (cat == MathTopicCategory.All) MathTopic.values().toList() else MathTopic.values().filter { it.category == cat }
     if (topic !in list && list.isNotEmpty()) {
      topic = list.first()
      repository.setSetting("math_topic", topic.name)
     }
    }
   }
  }
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
   filteredTopics.forEach { t ->
    SmallChip(t.title, active = topic == t) {
     topic = t
     repository.setSetting("math_topic", t.name)
    }
   }
  }
  Row(
   horizontalArrangement = Arrangement.spacedBy(8.dp),
   verticalAlignment = Alignment.CenterVertically,
   modifier = Modifier.horizontalScroll(rememberScrollState())
  ) {
   for (d in 1..3) {
    val pick = { difficulty = d; repository.setSetting("math_difficulty", d.toString()) }
    if (difficulty == d) PrimaryButton("Level $d", pick) else SecondaryButton("Level $d", pick)
   }
   SecondaryButton(if (mixed) "Mixed review ✓" else "Mixed review", { mixed = !mixed })
   SecondaryButton(if (reasoning) "Reasoning self-review ✓" else "Reasoning self-review", { reasoning = !reasoning })
  }
 }

 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
  // Left Column: Problem + Answer + Solution
  Column(Modifier.weight(1.35f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
   WorkbenchCard(
    title = "${topic.title} · Problem #$seed",
    subtitle = "${topic.category.title} · Level $difficulty ${if (difficulty == 1) "(Foundations)" else if (difficulty == 2) "(Intermediate)" else "(Advanced)"}",
    headerEnd = {
     problem.graph?.let { expr ->
      SmallChip("Open in Graph →") { onOpenInGraph("y = $expr") }
     }
    }
   ) {
    Text(problem.statement, style = AppTypography.h2, color = Theme.colors.textPrimary)
    Surface(
     color = Theme.colors.surfaceSecondary,
     shape = RoundedCornerShape(12.dp),
     modifier = Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp))
    ) {
     SelectionContainer {
      Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
       Text("GOVERNING PRINCIPLE / FORMULA", style = AppTypography.caption, color = Theme.colors.textMuted)
       Text(problem.formula, style = CodeStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = Theme.colors.textPrimary)
      }
     }
    }
    Text(
     when (problem.type) {
      MathAnswerType.Number -> "Accepts integers, decimals, fractions or expressions (e.g. 5/2, 2^3, sqrt(2), x = 3)."
      MathAnswerType.Expression -> "Enter an expression in x (e.g. 6x + 4 or x^2 + 5x + 6). Implicit multiplication is supported."
      MathAnswerType.Matrix -> "Enter comma-separated cells and semicolon-separated rows: 1,2;3,4 (or [[1,2],[3,4]])."
      MathAnswerType.Vector -> "Enter comma-separated components: 1,2 (or [1, 2])."
      MathAnswerType.Explanation -> "Write your step-by-step justification before revealing the rubric."
      else -> "Select the correct option below."
     },
     style = AppTypography.caption,
     color = Theme.colors.textSecondary
    )
    if (problem.type == MathAnswerType.Choice) {
     problem.choices.forEach { choice ->
      val isSelected = answer == choice
      OutlinedButton(
       onClick = { if (verdict == null) answer = choice },
       enabled = verdict == null,
       modifier = Modifier.fillMaxWidth(),
       border = BorderStroke(if (isSelected) 2.dp else 1.dp, if (isSelected) Theme.colors.brandPrimary else Theme.colors.borderStrong)
      ) {
       Text((if (isSelected) "● " else "○ ") + choice, color = Theme.colors.textPrimary)
      }
     }
    } else {
     OutlinedTextField(
      value = answer,
      onValueChange = { answer = it.take(256); repository.setSetting("math_answer:${problem.id}", answer) },
      label = { Text("Your answer") },
      enabled = verdict == null,
      singleLine = !reasoning,
      modifier = Modifier
       .fillMaxWidth()
       .onPreviewKeyEvent {
        if (!reasoning && it.type == KeyEventType.KeyDown && (it.key == Key.Enter || it.key == Key.NumPadEnter)) {
         submit(); true
        } else false
       },
      textStyle = CodeStyle.copy(fontSize = 16.sp)
     )
     if (verdict == null && !reasoning) {
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
       val snippets = when (problem.type) {
        MathAnswerType.Expression -> listOf("x^2", "x", "+", "-", "*")
        MathAnswerType.Matrix -> listOf(";", ",", "[", "]")
        MathAnswerType.Vector -> listOf(",", "[", "]")
        else -> listOf("/", "^", "sqrt(", "pi")
       }
       Text("Quick insert:", style = AppTypography.caption, color = Theme.colors.textMuted, modifier = Modifier.align(Alignment.CenterVertically))
       snippets.forEach { token ->
        SmallChip(token) {
         answer = (answer + token).take(256)
         repository.setSetting("math_answer:${problem.id}", answer)
        }
       }
      }
     }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
     SecondaryButton("Hint (${hints}/${problem.steps.size})", { hints = (hints + 1).coerceAtMost(problem.steps.size) }, enabled = verdict == null && hints < problem.steps.size)
     PrimaryButton(
      if (reasoning) "Submit reasoning" else "Check answer",
      { submit() },
      enabled = answer.trim().length >= (if (reasoning) 10 else 1) && verdict == null
     )
     SecondaryButton("Retry", {
      verdict = null; answer = ""; repository.setSetting("math_answer:${problem.id}", "")
      reviewSaved = ""; start = System.currentTimeMillis()
     })
     SecondaryButton("Next problem →", {
      seed++
      repository.setSetting("math_seed", seed.toString())
      if (mixed) {
       val pool = filteredTopics.ifEmpty { MathTopic.values().toList() }
       topic = pool[seed % pool.size]
       repository.setSetting("math_topic", topic.name)
      }
     })
    }

    if (verdict == null && hints > 0) {
     Column(
      Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Theme.colors.surfaceSecondary).padding(12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp)
     ) {
      problem.steps.take(hints).forEachIndexed { idx, step ->
       Text("Hint ${idx + 1}: $step", style = AppTypography.body2, color = Theme.colors.textPrimary)
      }
     }
    }
   }

   verdict?.let { v ->
    WorkbenchCard(
     title = if (reasoning) "Reasoning Review" else if (v.correct) "✓ Correct!" else "× Review Your Answer",
     subtitle = v.message
    ) {
     Text("Step-by-step solution:", style = AppTypography.caption, color = Theme.colors.textSecondary)
     problem.steps.forEachIndexed { i, step ->
      Text("${i + 1}. $step", style = AppTypography.body1, color = Theme.colors.textPrimary)
     }
     Text("Expected answer: ${problem.answer}", style = CodeStyle, color = Theme.colors.brandPrimary)
     if (reasoning) {
      Text("Rubric: name the governing principle, show each transformation, and justify the result and its assumptions.", color = Theme.colors.textPrimary)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       listOf("Needs Review", "Mastered").forEach { choice ->
        SecondaryButton(
         choice,
         {
          repository.labs.record("math-self-review", topic.name, problem.id, answer, "Self-assessed: $choice", choice == "Mastered", hints, System.currentTimeMillis() - start)
          reviewSaved = "Saved locally: $choice (self-assessed)"
         },
         enabled = reviewSaved.isEmpty()
        )
       }
      }
      if (reviewSaved.isNotEmpty()) Text(reviewSaved, color = Theme.colors.textSecondary)
     }
    }
   }

   WorkbenchCard(
    title = "Topic Mastery: ${topic.title}",
    subtitle = "${summary.mastery}/100 mastery score · ${summary.correct} correct of ${summary.attempts} attempts"
   ) {
    LinearProgressIndicator(
     progress = (summary.mastery / 100f).coerceIn(0f, 1f),
     modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
     color = Theme.colors.brandPrimary,
     backgroundColor = Theme.colors.surfaceTertiary
    )
   }
  }

  // Right Column: Live Graph Preview + Scratchpad Calculator
  Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
   problem.graph?.let { expr ->
    WorkbenchCard(
     title = "Visual Function Plot",
     subtitle = "y = $expr (drag to pan, + / − to zoom)",
     headerEnd = { SmallChip("Full Screen →") { onOpenInGraph("y = $expr") } }
    ) {
     GraphPreview(expr)
    }
   }

   WorkbenchCard(
    title = "Quick Scratchpad Evaluator",
    subtitle = "Test arithmetic, fractions, powers, roots, or trig while solving"
   ) {
    OutlinedTextField(
     value = scratchpad,
     onValueChange = { scratchpad = it.take(256) },
     singleLine = true,
     label = { Text("Expression, e.g. (12 + 8)/3, sqrt(49), log_2(32)") },
     modifier = Modifier.fillMaxWidth(),
     textStyle = CodeStyle
    )
    val evalOut = remember(scratchpad) {
     if (scratchpad.isBlank()) "Enter an expression above"
     else runCatching { "= " + formatMath(Evaluator().eval(MathParser.parse(scratchpad)), 10) }
      .getOrElse { "⚠ ${it.message ?: "Invalid expression"}" }
    }
    Surface(
     color = Theme.colors.surfaceSecondary,
     shape = RoundedCornerShape(10.dp),
     modifier = Modifier.fillMaxWidth()
    ) {
     SelectionContainer {
      Text(evalOut, Modifier.padding(12.dp), style = CodeStyle.copy(fontSize = 16.sp), color = Theme.colors.textPrimary)
     }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
     SmallChip("Use as answer") {
      val clean = evalOut.removePrefix("= ").trim()
      if (!clean.startsWith("⚠") && clean != "undefined") {
       answer = clean
       repository.setSetting("math_answer:${problem.id}", answer)
      }
     }
     SmallChip("Clear") { scratchpad = "" }
    }
   }
  }
 }
}

@Composable private fun GraphPreview(expression: String) {
 val viewport = remember(expression) { GraphViewport() }
 val compiled = remember(expression) { GraphCompiler.compile(listOf(expression)) }
 Box(
  Modifier
   .fillMaxWidth()
   .height(260.dp)
   .clip(RoundedCornerShape(12.dp))
   .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp))
 ) {
  GraphCanvas(compiled, listOf(true), listOf(graphColor(1)), viewport, selectedIndex = 0, wheelZoom = false, modifier = Modifier.fillMaxSize())
  ZoomControls(viewport, Modifier.align(Alignment.TopEnd).padding(8.dp))
 }
}

// ───────────────────────────── 2. Desmos-Style Graphing Calculator ─────────────────────────────

private val GraphPalette = listOf(
 Color(0xFFC74440),
 Color(0xFF2D70B3),
 Color(0xFF388C46),
 Color(0xFF6042A6),
 Color(0xFFFA7E19),
 Color(0xFF0E7C7B)
)

@Composable private fun graphColor(id: Int): Color = GraphPalette[((id % GraphPalette.size) + GraphPalette.size) % GraphPalette.size]

private data class GraphRow(val id: Int, val text: String, val visible: Boolean = true)

/** View state: centre (cx, cy) and half of the visible width in graph units. Units are square, like Desmos. */
@Stable class GraphViewport(private val homeRadius: Double = 10.0) {
 var cx by mutableStateOf(0.0)
 var cy by mutableStateOf(0.0)
 var radius by mutableStateOf(homeRadius)
 var width by mutableStateOf(0); internal set
 var height by mutableStateOf(0); internal set
 fun zoomAt(factor: Double, px: Float = width / 2f, py: Float = height / 2f) {
  if (width <= 0) { radius = (radius * factor).coerceIn(1e-7, 1e8); return }
  val upp = 2 * radius / width
  val dataX = cx + (px - width / 2.0) * upp; val dataY = cy - (py - height / 2.0) * upp
  val r = (radius * factor).coerceIn(1e-7, 1e8); val nupp = 2 * r / width
  cx = dataX - (px - width / 2.0) * nupp; cy = dataY + (py - height / 2.0) * nupp; radius = r
 }
 fun pan(dx: Float, dy: Float) { if (width <= 0) return; val upp = 2 * radius / width; cx -= dx * upp; cy += dy * upp }
 fun reset() { cx = 0.0; cy = 0.0; radius = homeRadius }
}

private const val HiddenPrefix = "#hidden#"
private val DefaultGraphRows = listOf("y = x^2 - 2", "y = a sin(x)", "a = 2", "x^2 + y^2 = 16", "y < -x - 6", "")
private val GraphExamples = listOf(
 "y = 2x + 1" to "Line",
 "y = x^3 - 3x" to "Cubic with extrema",
 "y = 1/x" to "Hyperbola (asymptotes)",
 "y = tan(x)" to "Tangent",
 "y = sqrt(x)" to "Square root",
 "y = |x - 2|" to "Absolute value",
 "y = e^x" to "Exponential",
 "y = ln(x)" to "Natural log",
 "x^2 + y^2 = 25" to "Circle",
 "x^2/9 + y^2/4 = 1" to "Ellipse",
 "y^2 = x" to "Sideways parabola",
 "x = 3" to "Vertical line",
 "y > x^2" to "Shaded inequality",
 "x^2 + y^2 <= 9" to "Shaded disk",
 "f(x) = x^2 - 4" to "Define function f(x)",
 "y = f'(x)" to "Derivative of f(x)",
 "y = {x < 0: -x, x^2}" to "Piecewise function",
 "y = sin(x) {0 <= x <= 2pi}" to "Domain restriction",
 "(3cos(t), 3sin(t))" to "Parametric circle",
 "(4sin(3t), 4cos(2t))" to "Lissajous curve",
 "(2, 3)" to "Point"
)

private val KeypadButtons = listOf(
 "x", "y", "t", "^2", "^", "sqrt(", "|", "pi", "e",
 "<=", ">=", "(", ")", "{", "}", "sin(", "cos(", "tan(", "ln(", "log("
)

@Composable private fun GraphingCalculator(
 repository: ProgressRepository,
 injectedExpression: String? = null,
 onInjectedConsumed: () -> Unit = {},
 modifier: Modifier
) {
 val rows = remember {
  mutableStateListOf<GraphRow>().apply {
   val saved = repository.getSetting("math_graph_rows", "")
   val lines = if (saved.isBlank()) DefaultGraphRows else saved.split('\n')
   lines.forEachIndexed { i, l -> add(GraphRow(i, l.removePrefix(HiddenPrefix), !l.startsWith(HiddenPrefix))) }
   if (isEmpty() || last().text.isNotBlank()) add(GraphRow(size, ""))
  }
 }
 var nextId by remember { mutableStateOf(rows.size + 1) }
 val viewport = remember { GraphViewport() }
 var selectedId by remember { mutableStateOf<Int?>(rows.firstOrNull()?.id) }
 var focusId by remember { mutableStateOf<Int?>(null) }
 var showHelp by remember { mutableStateOf(false) }
 var showKeypad by remember { mutableStateOf(true) }
 var showCalculus by remember { mutableStateOf(false) }
 var intA by remember { mutableStateOf("0") }
 var intB by remember { mutableStateOf("2") }
 var examples by remember { mutableStateOf(false) }
 val requesters = remember { mutableMapOf<Int, FocusRequester>() }
 val texts = rows.map { it.text }
 val compiled = remember(texts) { GraphCompiler.compile(texts) }
 val snapshot = rows.toList()

 LaunchedEffect(snapshot) {
  delay(400)
  repository.setSetting("math_graph_rows", snapshot.joinToString("\n") { (if (it.visible) "" else HiddenPrefix) + it.text.replace('\n', ' ') })
 }
 LaunchedEffect(focusId) {
  val id = focusId ?: return@LaunchedEffect
  delay(30)
  runCatching { requesters[id]?.requestFocus() }
  focusId = null
 }

 fun insertAfter(index: Int, text: String = ""): Int {
  val id = nextId++
  rows.add((index + 1).coerceIn(0, rows.size), GraphRow(id, text))
  return id
 }

 fun addSliders(index: Int, names: List<String>) {
  var at = index
  names.filter { n -> rows.none { r -> MathText.normalize(r.text).replace(" ", "").startsWith("$n=") } }
   .forEach { insertAfter(at, "$it = 1"); at++ }
 }

 fun appendExpression(text: String) {
  val emptyIndex = rows.indexOfLast { it.text.isBlank() }
  val id: Int
  if (emptyIndex >= 0) {
   id = rows[emptyIndex].id
   rows[emptyIndex] = rows[emptyIndex].copy(text = text)
  } else {
   id = nextId++
   rows.add(GraphRow(id, text))
  }
  if (rows.last().text.isNotBlank()) rows.add(GraphRow(nextId++, ""))
  selectedId = id
 }

 LaunchedEffect(injectedExpression) {
  val expr = injectedExpression ?: return@LaunchedEffect
  appendExpression(expr)
  onInjectedConsumed()
 }

 fun insertTokenIntoSelected(token: String) {
  val idx = rows.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 } ?: (rows.size - 1).coerceAtLeast(0)
  val r = rows[idx]
  rows[idx] = r.copy(text = (r.text + token).take(500))
  selectedId = r.id
  if (idx == rows.size - 1 && rows[idx].text.isNotBlank()) rows.add(GraphRow(nextId++, ""))
 }

 val colors = rows.map { graphColor(it.id) }
 val selectedIndex = rows.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 }

 Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
  // ── Left Sidebar: Expression List + Keypad + Calculus Inspector ──
  Column(
   Modifier
    .width(420.dp)
    .fillMaxHeight()
    .clip(RoundedCornerShape(14.dp))
    .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp))
    .background(Theme.colors.surfacePrimary)
  ) {
   Row(
    Modifier.fillMaxWidth().background(Theme.colors.surfaceSecondary).padding(horizontal = 10.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(6.dp)
   ) {
    SmallChip("+ Add") { focusId = insertAfter(rows.size - 1) }
    Box {
     SmallChip("Examples ▾") { examples = true }
     DropdownMenu(examples, { examples = false }) {
      GraphExamples.forEach { (expr, label) ->
       DropdownMenuItem(onClick = { appendExpression(expr); examples = false }) {
        Text("$label   ", color = Theme.colors.textSecondary, style = AppTypography.caption)
        Text(expr, style = CodeStyle)
       }
      }
     }
    }
    SmallChip("⌨ Keypad", active = showKeypad) { showKeypad = !showKeypad }
    SmallChip("∫dx", active = showCalculus) { showCalculus = !showCalculus }
    SmallChip("?", active = showHelp) { showHelp = !showHelp }
    Spacer(Modifier.weight(1f))
    SmallChip("Reset") {
     rows.clear()
     DefaultGraphRows.forEach { rows.add(GraphRow(nextId++, it)) }
     selectedId = rows.firstOrNull()?.id
     viewport.reset()
    }
   }

   if (showHelp) GraphHelp()

   if (showCalculus) {
    val selectedLine = selectedIndex?.let { compiled.lines.getOrNull(it) }
    val fnItem = when (val it = selectedLine?.item) {
     is GraphItem.FunctionOfX -> it
     is GraphItem.Definition -> it.plot
     else -> null
    }
    Column(
     Modifier.fillMaxWidth().background(Theme.colors.surfaceSecondary).padding(10.dp),
     verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
     Text("DEFINITE INTEGRAL & DERIVATIVE (SELECTED CURVE)", style = AppTypography.caption, color = Theme.colors.textSecondary)
     if (fnItem == null) {
      Text("Select a function of x (e.g. y = x^2) to integrate or differentiate.", style = AppTypography.caption, color = Theme.colors.textMuted)
     } else {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
       OutlinedTextField(intA, { intA = it.take(32) }, singleLine = true, label = { Text("From a") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
       OutlinedTextField(intB, { intB = it.take(32) }, singleLine = true, label = { Text("To b") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
      }
      val calcText = remember(fnItem, compiled.context, intA, intB) {
       runCatching {
        val aVal = Evaluator(compiled.context).eval(MathParser.parse(intA))
        val bVal = Evaluator(compiled.context).eval(MathParser.parse(intB))
        val ev = Evaluator(compiled.context)
        val f: (Double) -> Double = { x -> ev.at(x).eval(fnItem.expr) }
        val area = GraphSampler.definiteIntegral(f, aVal, bVal)
        val h = 1e-5 * max(1.0, abs(aVal))
        val derivAtA = (f(aVal + h) - f(aVal - h)) / (2 * h)
        "∫(${formatMath(aVal, 3)} → ${formatMath(bVal, 3)}) f(x) dx = ${formatMath(area, 6)}   ·   f'(${formatMath(aVal, 3)}) = ${formatMath(derivAtA, 6)}"
       }.getOrElse { "Enter valid bounds a and b" }
      }
      SelectionContainer { Text(calcText, style = CodeStyle.copy(fontSize = 13.sp), color = Theme.colors.brandPrimary) }
     }
    }
    Divider(color = Theme.colors.borderDefault)
   }

   Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
    rows.forEachIndexed { index, row ->
     val line = compiled.lines.getOrNull(index) ?: GraphLine(GraphItem.Blank)
     val fr = requesters.getOrPut(row.id) { FocusRequester() }
     key(row.id) {
      ExpressionRow(
       index = index,
       row = row,
       line = line,
       color = colors[index],
       selected = row.id == selectedId,
       focusRequester = fr,
       onText = { t ->
        val i = rows.indexOfFirst { it.id == row.id }
        if (i >= 0) {
         rows[i] = rows[i].copy(text = t)
         if (i == rows.size - 1 && t.isNotBlank()) rows.add(GraphRow(nextId++, ""))
        }
       },
       onToggle = {
        val i = rows.indexOfFirst { it.id == row.id }
        if (i >= 0) rows[i] = rows[i].copy(visible = !rows[i].visible)
       },
       onDelete = {
        val i = rows.indexOfFirst { it.id == row.id }
        if (i >= 0) {
         rows.removeAt(i)
         if (rows.isEmpty()) rows.add(GraphRow(nextId++, ""))
         focusId = rows[(i - 1).coerceAtLeast(0)].id
        }
       },
       onEnter = {
        val i = rows.indexOfFirst { it.id == row.id }
        focusId = if (i == rows.size - 1 || rows[i + 1].text.isNotBlank()) insertAfter(i) else rows[i + 1].id
       },
       onMove = { delta ->
        val i = rows.indexOfFirst { it.id == row.id } + delta
        if (i in rows.indices) focusId = rows[i].id
       },
       onFocus = { selectedId = row.id },
       onAddSliders = { names -> addSliders(rows.indexOfFirst { it.id == row.id }, names) }
      )
     }
    }
    Spacer(Modifier.height(48.dp).fillMaxWidth().clickable { focusId = rows.last().id })
   }

   if (showKeypad) {
    Divider(color = Theme.colors.borderDefault)
    Column(
     Modifier.fillMaxWidth().background(Theme.colors.surfaceSecondary).padding(8.dp),
     verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
     KeypadButtons.chunked(7).forEach { chunk ->
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
       chunk.forEach { token ->
        Box(
         Modifier
          .weight(1f)
          .clip(RoundedCornerShape(8.dp))
          .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(8.dp))
          .background(Theme.colors.surfacePrimary)
          .clickable { insertTokenIntoSelected(token) }
          .padding(vertical = 7.dp),
         contentAlignment = Alignment.Center
        ) {
         Text(token, style = CodeStyle.copy(fontSize = 12.sp), color = Theme.colors.textPrimary)
        }
       }
      }
     }
    }
   }
  }

  // ── Right Area: Interactive Graph Canvas ──
  Box(
   Modifier
    .weight(1f)
    .fillMaxHeight()
    .clip(RoundedCornerShape(14.dp))
    .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp))
  ) {
   GraphCanvas(compiled, rows.map { it.visible }, colors, viewport, selectedIndex, wheelZoom = true, modifier = Modifier.fillMaxSize())
   ZoomControls(viewport, Modifier.align(Alignment.TopEnd).padding(10.dp))
  }
 }
}

@Composable private fun GraphHelp() {
 val tips = listOf(
  "y = 2x + 1,  y = x^2,  sin(x),  |x|" to "functions of x (\"y =\" is optional)",
  "x^2 + y^2 = 9,  xy = 1" to "implicit equations in x and y",
  "y > x^2,  x^2 + y^2 <= 4" to "shaded inequalities",
  "x = y^2,  x = 3" to "functions of y & vertical lines",
  "(3cos(t), 3sin(t))" to "parametric curves in t",
  "a = 2,  y = a x" to "interactive sliders",
  "f(x) = x^3,  y = f(x - 1),  y = f'(x)" to "custom functions & derivatives",
  "{x < 0: -x, x^2},  sin(x) {x > 0}" to "piecewise & domain restrictions"
 )
 Column(
  Modifier.fillMaxWidth().background(Theme.colors.surfaceSecondary).padding(10.dp),
  verticalArrangement = Arrangement.spacedBy(4.dp)
 ) {
  tips.forEach { (code, note) ->
   Text(code, style = CodeStyle.copy(fontSize = 12.sp), color = Theme.colors.textPrimary)
   Text(note, style = AppTypography.caption, color = Theme.colors.textSecondary)
  }
  Text("Drag to pan, scroll wheel to zoom, hover any curve to trace coordinates. Select a curve to highlight its roots, extrema, and intersections.", style = AppTypography.caption, color = Theme.colors.textSecondary)
 }
 Divider(color = Theme.colors.borderDefault)
}

@Composable private fun ExpressionRow(
 index: Int, row: GraphRow, line: GraphLine, color: Color, selected: Boolean, focusRequester: FocusRequester,
 onText: (String) -> Unit, onToggle: () -> Unit, onDelete: () -> Unit, onEnter: () -> Unit, onMove: (Int) -> Unit,
 onFocus: () -> Unit, onAddSliders: (List<String>) -> Unit
) {
 val item = line.item
 val drawable = line.drawable && line.missing.isEmpty()
 Column(Modifier.fillMaxWidth().background(if (selected) Theme.colors.surfaceSecondary else Theme.colors.surfacePrimary)) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 2.dp)) {
   Text("${index + 1}", style = AppTypography.caption, color = Theme.colors.textMuted, modifier = Modifier.width(22.dp))
   Box(
    Modifier
     .size(22.dp)
     .clip(CircleShape)
     .background(if (drawable && row.visible) color else Color.Transparent)
     .border(2.dp, if (drawable) color else Theme.colors.borderDefault, CircleShape)
     .clickable(enabled = drawable, onClick = onToggle)
     .semantics { contentDescription = if (row.visible) "Hide expression ${index + 1}" else "Show expression ${index + 1}" }
   )
   Spacer(Modifier.width(10.dp))
   BasicTextField(
    value = row.text,
    onValueChange = { onText(it.replace("\n", "").take(500)) },
    singleLine = true,
    textStyle = CodeStyle.copy(color = Theme.colors.textPrimary, fontSize = 16.sp),
    cursorBrush = SolidColor(Theme.colors.textPrimary),
    modifier = Modifier
     .weight(1f)
     .focusRequester(focusRequester)
     .onFocusChanged { if (it.isFocused) onFocus() }
     .onPreviewKeyEvent { e ->
      if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
      when (e.key) {
       Key.Enter, Key.NumPadEnter -> { onEnter(); true }
       Key.Backspace -> if (row.text.isEmpty()) { onDelete(); true } else false
       Key.DirectionUp -> { onMove(-1); true }
       Key.DirectionDown -> { onMove(1); true }
       else -> false
      }
     },
    decorationBox = { inner ->
     Box(Modifier.padding(vertical = 10.dp)) {
      if (row.text.isEmpty()) Text(if (index == 0) "Type an equation, e.g. y = x^2" else "", style = CodeStyle.copy(fontSize = 16.sp), color = Theme.colors.textMuted)
      inner()
     }
    }
   )
   Text(
    "×",
    color = Theme.colors.textMuted,
    style = AppTypography.h3,
    modifier = Modifier
     .clip(CircleShape)
     .clickable(onClick = onDelete)
     .padding(horizontal = 8.dp, vertical = 2.dp)
     .semantics { contentDescription = "Delete expression ${index + 1}" }
   )
  }
  val info: Pair<String, Color>? = when (item) {
   is GraphItem.Invalid -> "⚠ ${item.message}" to Theme.colors.error
   is GraphItem.Value -> "= ${formatMath(item.value, 10)}" to Theme.colors.textSecondary
   is GraphItem.Constant -> "${item.name} = ${formatMath(item.value, 10)}" to Theme.colors.textSecondary
   is GraphItem.Point -> "point (${formatMath(item.x, 4)}, ${formatMath(item.y, 4)})" to Theme.colors.textSecondary
   is GraphItem.Parametric -> "parametric curve (−2π ≤ t ≤ 2π)" to Theme.colors.textMuted
   is GraphItem.Definition -> if (item.plot == null && line.missing.isEmpty()) "function ${item.name}(${item.params.joinToString()})" to Theme.colors.textSecondary else null
   is GraphItem.Implicit -> if (item.relation.isInequality) "region ${item.relation.symbol}" to Theme.colors.textMuted else null
   else -> null
  }
  info?.let { (text, c) -> Text(text, style = AppTypography.caption, color = c, modifier = Modifier.padding(start = 60.dp, end = 10.dp, bottom = 6.dp)) }
  if (item is GraphItem.Slider) SliderRow(item, onText)
  if (line.missing.isNotEmpty()) {
   Row(
    Modifier.padding(start = 60.dp, bottom = 8.dp).horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    verticalAlignment = Alignment.CenterVertically
   ) {
    Text("add slider:", style = AppTypography.caption, color = Theme.colors.textSecondary)
    line.missing.forEach { name -> SmallChip(name) { onAddSliders(listOf(name)) } }
    if (line.missing.size > 1) SmallChip("all") { onAddSliders(line.missing) }
   }
  }
  Divider(color = Theme.colors.borderDefault)
 }
}

@Composable private fun SliderRow(item: GraphItem.Slider, onText: (String) -> Unit) {
 val lo = min(-10.0, floor(item.value)); val hi = max(10.0, ceil(item.value))
 val step = GraphSampler.niceStep((hi - lo) / 200)
 Row(Modifier.padding(start = 56.dp, end = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
  Text(formatMath(lo), style = AppTypography.caption, color = Theme.colors.textMuted)
  Slider(
   value = item.value.toFloat().coerceIn(lo.toFloat(), hi.toFloat()),
   onValueChange = { v ->
    val snapped = (v / step).roundToLong() * step
    onText("${item.name} = ${formatMath(snapped, 6)}")
   },
   valueRange = lo.toFloat()..hi.toFloat(),
   modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
   colors = SliderDefaults.colors(thumbColor = Theme.colors.brandPrimary, activeTrackColor = Theme.colors.brandPrimary)
  )
  Text(formatMath(hi), style = AppTypography.caption, color = Theme.colors.textMuted)
 }
}

@Composable private fun ZoomControls(viewport: GraphViewport, modifier: Modifier) {
 Column(modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(10.dp)).background(Theme.colors.surfacePrimary)) {
  listOf("+" to "Zoom in", "−" to "Zoom out", "⌂" to "Default view").forEach { (label, desc) ->
   Box(
    Modifier
     .size(38.dp)
     .clickable { when (label) { "+" -> viewport.zoomAt(0.6); "−" -> viewport.zoomAt(1 / 0.6); else -> viewport.reset() } }
     .semantics { contentDescription = desc },
    contentAlignment = Alignment.Center
   ) {
    Text(label, style = AppTypography.h3, color = Theme.colors.textPrimary)
   }
  }
 }
}

// ── Graph Scene Rendering ──

private class GraphTransform(val xMin: Double, val yMax: Double, val upp: Double, val w: Float, val h: Float) {
 val xMax get() = xMin + w * upp
 val yMin get() = yMax - h * upp
 fun px(x: Double) = ((x - xMin) / upp).coerceIn(-1e5, 1e5).toFloat()
 fun py(y: Double) = ((yMax - y) / upp).coerceIn(-1e5, 1e5).toFloat()
 fun dataX(px: Float) = xMin + px * upp
 fun dataY(py: Float) = yMax - py * upp
}

private class SceneStroke(val color: Color, val path: Path, val dashed: Boolean)
private class SceneShade(val color: Color, val rects: List<Rect>)
private class SceneDot(val color: Color, val at: Offset, val label: String)
private class SceneTrace(val color: Color, val f: (Double) -> Double)
private class ScenePoi(val at: Offset, val poi: PointOfInterest)
private class Scene(val t: GraphTransform, val shades: List<SceneShade>, val strokes: List<SceneStroke>, val dots: List<SceneDot>, val traces: List<SceneTrace>, val pois: List<ScenePoi>)

private fun polylinePath(lines: List<Polyline>, toX: (Double, Double) -> Float, toY: (Double, Double) -> Float): Path {
 val path = Path()
 for (l in lines) {
  if (l.size == 0) continue
  path.moveTo(toX(l.xs[0], l.ys[0]), toY(l.xs[0], l.ys[0]))
  if (l.size == 1) path.lineTo(toX(l.xs[0], l.ys[0]) + 0.5f, toY(l.xs[0], l.ys[0]))
  for (k in 1 until l.size) path.lineTo(toX(l.xs[k], l.ys[k]), toY(l.xs[k], l.ys[k]))
 }
 return path
}

private fun buildScene(compiled: CompiledGraph, visible: List<Boolean>, colors: List<Color>, t: GraphTransform, selected: Int?): Scene {
 val shades = mutableListOf<SceneShade>(); val strokes = mutableListOf<SceneStroke>(); val dots = mutableListOf<SceneDot>()
 val traces = mutableListOf<SceneTrace>(); val pois = mutableListOf<ScenePoi>()
 val ctx = compiled.context
 val steps = t.w.toInt().coerceIn(200, 2400)
 val explicitFns = mutableMapOf<Int, (Double) -> Double>()
 fun fnOfX(expr: Expr): (Double) -> Double {
  val ev = Evaluator(ctx)
  return { x -> try { ev.at(x).eval(expr) } catch (_: Exception) { Double.NaN } }
 }
 compiled.lines.forEachIndexed { i, line ->
  if (!(visible.getOrElse(i) { true }) || !line.drawable || line.missing.isNotEmpty()) return@forEachIndexed
  val color = colors.getOrElse(i) { Color.Gray }
  val item = when (val it = line.item) { is GraphItem.Definition -> it.plot ?: return@forEachIndexed; else -> it }
  when (item) {
   is GraphItem.FunctionOfX -> {
    val f = fnOfX(item.expr)
    if (item.relation == Relation.Eq) explicitFns[i] = f
    if (item.relation.isInequality) {
     val above = item.relation == Relation.Gt || item.relation == Relation.Ge
     val rects = ArrayList<Rect>(); var px = 0f
     while (px < t.w) {
      val y = f(t.dataX(px + 1f))
      if (y.isFinite()) {
       val py = t.py(y).coerceIn(0f, t.h)
       rects += if (above) Rect(px, 0f, px + 2f, py) else Rect(px, py, px + 2f, t.h)
      }
      px += 2f
     }
     shades += SceneShade(color, rects)
    }
    val lines = GraphSampler.sample(f, t.xMin, t.xMax, steps, t.yMin, t.yMax)
    strokes += SceneStroke(color, polylinePath(lines, { x, _ -> t.px(x) }, { _, y -> t.py(y) }), item.relation.strict)
    traces += SceneTrace(color, f)
   }
   is GraphItem.FunctionOfY -> {
    val ev = Evaluator(ctx)
    val g: (Double) -> Double = { y -> try { ev.y = y; ev.hasY = true; ev.eval(item.expr) } catch (_: Exception) { Double.NaN } }
    if (item.relation.isInequality) {
     val right = item.relation == Relation.Gt || item.relation == Relation.Ge
     val rects = ArrayList<Rect>(); var py = 0f
     while (py < t.h) {
      val x = g(t.dataY(py + 1f))
      if (x.isFinite()) {
       val px = t.px(x).coerceIn(0f, t.w)
       rects += if (right) Rect(px, py, t.w, py + 2f) else Rect(0f, py, px, py + 2f)
      }
      py += 2f
     }
     shades += SceneShade(color, rects)
    }
    val lines = GraphSampler.sample(g, t.yMin, t.yMax, t.h.toInt().coerceIn(200, 2400), t.xMin, t.xMax)
    strokes += SceneStroke(color, polylinePath(lines, { _, x -> t.px(x) }, { y, _ -> t.py(y) }), item.relation.strict)
   }
   is GraphItem.Parametric -> {
    val evX = Evaluator(ctx); val evY = Evaluator(ctx)
    val lines = GraphSampler.sampleParametric(
     fx = { p -> evX.atT(p).eval(item.xExpr) },
     fy = { p -> evY.atT(p).eval(item.yExpr) },
     tMin = item.tMin,
     tMax = item.tMax
    )
    strokes += SceneStroke(color, polylinePath(lines, { x, _ -> t.px(x) }, { _, y -> t.py(y) }), false)
   }
   is GraphItem.Implicit -> {
    val ev = Evaluator(ctx)
    val f: (Double, Double) -> Double = { x, y -> try { ev.at(x, y).eval(item.expr) } catch (_: Exception) { Double.NaN } }
    val cell = 4f
    val cols = (t.w / cell).toInt().coerceAtLeast(10); val rowsN = (t.h / cell).toInt().coerceAtLeast(10)
    val field = GraphSampler.field(f, t.xMin, t.xMax, t.yMin, t.yMax, cols, rowsN)
    val cw = t.w / cols; val ch = t.h / rowsN
    if (item.relation.isInequality) {
     val rects = ArrayList<Rect>()
     for (j in 0 until rowsN) {
      var runStart = -1
      for (k in 0..cols) {
       val inside = k < cols && run {
        val v = (field[k, j] + field[k + 1, j] + field[k, j + 1] + field[k + 1, j + 1]) / 4
        v.isFinite() && item.relation.holds(v)
       }
       if (inside && runStart < 0) runStart = k
       if (!inside && runStart >= 0) {
        val top = t.h - (j + 1) * ch
        rects += Rect(runStart * cw, top, k * cw, top + ch)
        runStart = -1
       }
      }
     }
     shades += SceneShade(color, rects)
    }
    val segs = GraphSampler.contour(field, f)
    val path = Path()
    segs.forEach { s -> path.moveTo(t.px(s.x1), t.py(s.y1)); path.lineTo(t.px(s.x2), t.py(s.y2)) }
    strokes += SceneStroke(color, path, item.relation.strict)
   }
   is GraphItem.Point -> dots += SceneDot(color, Offset(t.px(item.x), t.py(item.y)), "(${formatMath(item.x, 4)}, ${formatMath(item.y, 4)})")
   else -> {}
  }
 }
 val sf = selected?.let { explicitFns[it] }
 if (sf != null) {
  val others = explicitFns.filterKeys { it != selected }.values.toList()
  GraphSampler.pointsOfInterest(sf, others, t.xMin, t.xMax, t.yMin, t.yMax).forEach { p ->
   if (p.y in t.yMin..t.yMax) pois += ScenePoi(Offset(t.px(p.x), t.py(p.y)), p)
  }
 }
 return Scene(t, shades, strokes, dots, traces, pois)
}

@Composable private fun GraphCanvas(
 compiled: CompiledGraph,
 visible: List<Boolean>,
 colors: List<Color>,
 viewport: GraphViewport,
 selectedIndex: Int?,
 wheelZoom: Boolean,
 modifier: Modifier
) {
 val measurer = rememberTextMeasurer()
 var hover by remember { mutableStateOf<Offset?>(null) }
 val bg = Theme.colors.surfacePrimary
 val ink = Theme.colors.textPrimary
 val labelStyle = TextStyle(fontSize = 11.sp, color = Theme.colors.textSecondary)
 val w = viewport.width; val h = viewport.height
 val scene = if (w > 0 && h > 0) {
  val upp = 2 * viewport.radius / w
  val t = GraphTransform(viewport.cx - w / 2.0 * upp, viewport.cy + h / 2.0 * upp, upp, w.toFloat(), h.toFloat())
  remember(compiled, visible, colors, viewport.cx, viewport.cy, viewport.radius, w, h, selectedIndex) {
   buildScene(compiled, visible, colors, t, selectedIndex)
  }
 } else null
 Box(
  modifier
   .background(bg)
   .onSizeChanged { s: IntSize -> viewport.width = s.width; viewport.height = s.height }
   .semantics { contentDescription = "Interactive graph. Drag to pan, scroll to zoom." }
   .pointerInput(wheelZoom) {
    awaitPointerEventScope {
     var last: Offset? = null
     while (true) {
      val event = awaitPointerEvent()
      val change = event.changes.firstOrNull() ?: continue
      when (event.type) {
       PointerEventType.Scroll -> if (wheelZoom) {
        val dy = change.scrollDelta.y
        if (dy != 0f) { viewport.zoomAt(exp(dy * 0.15).toDouble(), change.position.x, change.position.y); change.consume() }
       }
       PointerEventType.Press -> last = change.position
       PointerEventType.Move -> {
        val l = last
        if (change.pressed && l != null) { viewport.pan(change.position.x - l.x, change.position.y - l.y); last = change.position; change.consume() }
        hover = change.position
       }
       PointerEventType.Release -> last = null
       PointerEventType.Enter -> hover = change.position
       PointerEventType.Exit -> { hover = null; last = null }
      }
     }
    }
   }
 ) {
  if (scene != null) {
   Canvas(Modifier.fillMaxSize()) {
    drawGrid(scene.t, ink, bg, measurer, labelStyle)
    clipRectSafe {
     scene.shades.forEach { s -> s.rects.forEach { r -> drawRect(s.color.copy(alpha = 0.16f), r.topLeft, r.size) } }
     val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))
     scene.strokes.forEach { s ->
      drawPath(
       s.path,
       s.color,
       style = Stroke(width = 2.6.dp.toPx(), pathEffect = if (s.dashed) dash else null, join = StrokeJoin.Round, cap = StrokeCap.Round)
      )
     }
     scene.pois.forEach { p -> drawCircle(Color.Gray, 4.5.dp.toPx(), p.at); drawCircle(bg, 2.dp.toPx(), p.at) }
     scene.dots.forEach { d -> drawCircle(d.color, 5.5.dp.toPx(), d.at); drawLabel(measurer, d.label, d.at + Offset(10f, -26f), ink, bg) }
    }
   }
   Canvas(Modifier.fillMaxSize()) {
    val hv = hover ?: return@Canvas
    val t = scene.t
    val snapPoi = scene.pois.minByOrNull { (it.at - hv).getDistance() }?.takeIf { (it.at - hv).getDistance() < 14.dp.toPx() }
    if (snapPoi != null) {
     val p = snapPoi.poi
     val kind = when (p.kind) {
      PoiKind.Root -> "root"
      PoiKind.Maximum -> "maximum"
      PoiKind.Minimum -> "minimum"
      PoiKind.YIntercept -> "y-intercept"
      PoiKind.Intersection -> "intersection"
     }
     drawCircle(Color.Gray, 6.dp.toPx(), snapPoi.at)
     drawLabel(measurer, "$kind (${formatMath(p.x, 4)}, ${formatMath(p.y, 4)})", snapPoi.at + Offset(12f, -30f), ink, bg)
     return@Canvas
    }
    val x = t.dataX(hv.x)
    val best = scene.traces.mapNotNull { tr -> val y = tr.f(x); if (y.isFinite()) Triple(tr, y, abs(t.py(y) - hv.y)) else null }.minByOrNull { it.third }
    if (best != null && best.third < 36.dp.toPx()) {
     val at = Offset(hv.x, t.py(best.second))
     drawCircle(best.first.color, 5.5.dp.toPx(), at); drawCircle(bg, 2.5.dp.toPx(), at)
     drawLabel(measurer, "(${formatMath(x, 4)}, ${formatMath(best.second, 4)})", at + Offset(12f, -30f), ink, bg)
    } else {
     drawLabel(measurer, "x = ${formatMath(x, 3)}   y = ${formatMath(t.dataY(hv.y), 3)}", Offset(8f, size.height - 30f), ink, bg)
    }
   }
  }
 }
}

private inline fun DrawScope.clipRectSafe(block: DrawScope.() -> Unit) {
 drawContext.canvas.save()
 drawContext.canvas.clipRect(0f, 0f, size.width, size.height)
 block()
 drawContext.canvas.restore()
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, at: Offset, ink: Color, bg: Color) {
 val layout = measurer.measure(text, TextStyle(fontSize = 12.sp, color = ink, fontWeight = FontWeight.Medium))
 val pad = 5f
 val x = at.x.coerceIn(2f, (size.width - layout.size.width - 2 * pad - 2f).coerceAtLeast(2f))
 val y = at.y.coerceIn(2f, (size.height - layout.size.height - 2 * pad - 2f).coerceAtLeast(2f))
 drawRoundRect(bg.copy(alpha = 0.92f), Offset(x, y), Size(layout.size.width + 2 * pad, layout.size.height + 2 * pad), CornerRadius(6f))
 drawRoundRect(ink.copy(alpha = 0.25f), Offset(x, y), Size(layout.size.width + 2 * pad, layout.size.height + 2 * pad), CornerRadius(6f), style = Stroke(1f))
 drawText(layout, topLeft = Offset(x + pad, y + pad))
}

private fun DrawScope.drawGrid(t: GraphTransform, ink: Color, bg: Color, measurer: TextMeasurer, labelStyle: TextStyle) {
 val major = GraphSampler.niceStep(t.upp * 110)
 val lead = major / 10.0.pow(floor(log10(major)))
 val minor = major / (if (abs(lead - 2.0) < 1e-9) 4 else 5)
 val minorColor = ink.copy(alpha = 0.06f); val majorColor = ink.copy(alpha = 0.16f); val axisColor = ink.copy(alpha = 0.75f)
 fun lines(step: Double, color: Color, width: Float) {
  var x = ceil(t.xMin / step) * step; var n = 0
  while (x <= t.xMax && n < 2000) { val px = t.px(x); drawLine(color, Offset(px, 0f), Offset(px, size.height), width); x += step; n++ }
  var y = ceil(t.yMin / step) * step; n = 0
  while (y <= t.yMax && n < 2000) { val py = t.py(y); drawLine(color, Offset(0f, py), Offset(size.width, py), width); y += step; n++ }
 }
 lines(minor, minorColor, 1f)
 lines(major, majorColor, 1f)
 val ax = t.px(0.0); val ay = t.py(0.0)
 if (ay in 0f..size.height) drawLine(axisColor, Offset(0f, ay), Offset(size.width, ay), 1.6f)
 if (ax in 0f..size.width) drawLine(axisColor, Offset(ax, 0f), Offset(ax, size.height), 1.6f)
 val decimals = (-floor(log10(major))).toInt().coerceIn(0, 12) + 1
 fun label(v: Double) = formatMath(if (abs(v) < major * 1e-6) 0.0 else v, decimals)
 val labelY = ay.coerceIn(2f, size.height - 18f)
 var x = ceil(t.xMin / major) * major; var n = 0
 while (x <= t.xMax && n < 400) {
  if (abs(x) > major * 1e-6) {
   val layout = measurer.measure(label(x), labelStyle)
   val px = t.px(x) - layout.size.width / 2f
   val top = if (labelY + 4f + layout.size.height > size.height) labelY - layout.size.height - 4f else labelY + 4f
   drawRect(bg.copy(alpha = 0.8f), Offset(px - 2f, top), Size(layout.size.width + 4f, layout.size.height.toFloat()))
   drawText(layout, topLeft = Offset(px, top))
  }
  x += major; n++
 }
 var y = ceil(t.yMin / major) * major; n = 0
 while (y <= t.yMax && n < 400) {
  if (abs(y) > major * 1e-6) {
   val layout = measurer.measure(label(y), labelStyle)
   var left = ax - layout.size.width - 6f
   if (left < 2f) left = (ax + 6f).coerceAtLeast(2f)
   if (left + layout.size.width > size.width - 2f) left = size.width - layout.size.width - 4f
   val top = t.py(y) - layout.size.height / 2f
   drawRect(bg.copy(alpha = 0.8f), Offset(left - 2f, top), Size(layout.size.width + 4f, layout.size.height.toFloat()))
   drawText(layout, topLeft = Offset(left, top))
  }
  y += major; n++
 }
 if (ax in 0f..size.width && ay in 0f..size.height) drawText(measurer, "0", Offset(ax - 12f, ay + 3f), labelStyle)
}

// ───────────────────────────── 3. Matrix & Vector Workbench ─────────────────────────────

private enum class LinalgOp(val title: String, val isVectorOp: Boolean, val needsSecondInput: Boolean) {
 Multiply("Multiply (A × B)", false, true),
 Add("Add (A + B)", false, true),
 Subtract("Subtract (A − B)", false, true),
 DetTrace("Determinant & Trace", false, false),
 Inverse("Inverse (A⁻¹)", false, false),
 Transpose("Transpose (Aᵀ)", false, false),
 RrefRank("RREF & Rank", false, false),
 Eigen("Eigenvalues (2×2)", false, false),
 Solve("Solve System (Ax = b)", false, true),
 Dot("Dot Product (a · b)", true, true),
 Norm("Norm & Unit Vector", true, false),
 Angle("Angle & Cosine Sim", true, true),
 Projection("Projection (proj_b a)", true, true),
 Cross("Cross Product (a × b)", true, true),
 VecAdd("Vector Add & Distance", true, true)
}

@Composable private fun MatrixVectorWorkbench(repository: ProgressRepository) {
 var op by remember {
  mutableStateOf(
   runCatching { LinalgOp.valueOf(repository.getSetting("math_linalg_op", LinalgOp.Multiply.name)) }
    .getOrDefault(LinalgOp.Multiply)
  )
 }
 // Separate saved inputs for matrices vs vectors so switching between Matrix & Vector operations NEVER breaks!
 var matA by remember { mutableStateOf(repository.getSetting("math_mat_a", "1, 2; 3, 4")) }
 var matB by remember { mutableStateOf(repository.getSetting("math_mat_b", "2, 0; 1, 3")) }
 var rhsB by remember { mutableStateOf(repository.getSetting("math_rhs_b", "5, 11")) }
 var vecA by remember { mutableStateOf(repository.getSetting("math_vec_a", "3, 4")) }
 var vecB by remember { mutableStateOf(repository.getSetting("math_vec_b", "1, 2")) }

 fun setMatA(v: String) { matA = v; repository.setSetting("math_mat_a", v) }
 fun setMatB(v: String) { matB = v; repository.setSetting("math_mat_b", v) }
 fun setRhsB(v: String) { rhsB = v; repository.setSetting("math_rhs_b", v) }
 fun setVecA(v: String) { vecA = v; repository.setSetting("math_vec_a", v) }
 fun setVecB(v: String) { vecB = v; repository.setSetting("math_vec_b", v) }

 fun selectOp(newOp: LinalgOp) {
  op = newOp
  repository.setSetting("math_linalg_op", newOp.name)
  if (newOp == LinalgOp.Cross) {
   // If vectors are currently 2D, cross product still works (promotes z=0), or user can use 3D preset
  }
 }

 // Operation Selection Card
 WorkbenchCard(
  title = "Matrix & Vector Studio",
  subtitle = "Choose a matrix or vector operation — visual bracket grids and text inputs update live"
 ) {
  Text("MATRIX OPERATIONS", style = AppTypography.caption, color = Theme.colors.textMuted)
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
   LinalgOp.values().filter { !it.isVectorOp }.forEach { item ->
    SmallChip(item.title, active = op == item) { selectOp(item) }
   }
  }
  Text("VECTOR OPERATIONS", style = AppTypography.caption, color = Theme.colors.textMuted)
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
   LinalgOp.values().filter { it.isVectorOp }.forEach { item ->
    SmallChip(item.title, active = op == item) { selectOp(item) }
   }
  }
 }

 // Input Editors Row
 Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
  if (!op.isVectorOp) {
   MatrixEditorCard(
    title = "Matrix A",
    text = matA,
    onTextChange = ::setMatA,
    isVector = false,
    modifier = Modifier.weight(1f),
    onSwap = if (op.needsSecondInput && op != LinalgOp.Solve) ({ val t = matA; setMatA(matB); setMatB(t) }) else null
   )
   if (op == LinalgOp.Solve) {
    MatrixEditorCard(
     title = "Right-Hand Vector b (for Ax = b)",
     text = rhsB,
     onTextChange = ::setRhsB,
     isVector = true,
     modifier = Modifier.weight(1f)
    )
   } else if (op.needsSecondInput) {
    MatrixEditorCard(
     title = "Matrix B",
     text = matB,
     onTextChange = ::setMatB,
     isVector = false,
     modifier = Modifier.weight(1f)
    )
   }
  } else {
   MatrixEditorCard(
    title = "Vector a",
    text = vecA,
    onTextChange = ::setVecA,
    isVector = true,
    modifier = Modifier.weight(1f),
    onSwap = if (op.needsSecondInput) ({ val t = vecA; setVecA(vecB); setVecB(t) }) else null
   )
   if (op.needsSecondInput) {
    MatrixEditorCard(
     title = "Vector b",
     text = vecB,
     onTextChange = ::setVecB,
     isVector = true,
     modifier = Modifier.weight(1f)
    )
   }
  }
 }

 // Live Computation Result
 val evaluation = remember(op, matA, matB, rhsB, vecA, vecB) {
  runCatching { computeLinalgResult(op, matA, matB, rhsB, vecA, vecB) }
 }

 evaluation.fold(
  onSuccess = { res ->
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    WorkbenchCard(
     title = res.headline,
     subtitle = res.formula,
     modifier = Modifier.weight(1.3f),
     headerEnd = {
      res.resultMatrix?.let { m ->
       val formatted = m.joinToString("; ") { r -> r.joinToString(", ") { formatMath(it, 6) } }
       SmallChip("Use Result as Input A") {
        if (op.isVectorOp) setVecA(formatted) else setMatA(formatted)
       }
      }
     }
    ) {
     if (res.resultMatrix != null) {
      VisualMatrixDisplay(res.resultMatrix)
     }
     if (res.metrics.isNotEmpty()) {
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
       res.metrics.forEach { (k, v) -> MetricTile(k, v) }
      }
     }
     if (res.steps.isNotEmpty()) {
      Column(
       Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Theme.colors.surfaceSecondary).padding(12.dp),
       verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
       Text("COMPUTATION DETAILS", style = AppTypography.caption, color = Theme.colors.textMuted)
       res.steps.forEach { line ->
        SelectionContainer { Text(line, style = CodeStyle.copy(fontSize = 13.sp), color = Theme.colors.textPrimary) }
       }
      }
     }
    }

    // Side Card: Properties of A or 2D Vector Plot
    if (op.isVectorOp) {
     val va = runCatching { extractVectorOrFlatten(vecA).first }.getOrNull()
     val vb = if (op.needsSecondInput) runCatching { extractVectorOrFlatten(vecB).first }.getOrNull() else null
     if (va != null && va.size >= 2) {
      WorkbenchCard(
       title = "2D Vector Plane (x, y)",
       subtitle = "Visualizes the first two components of vectors a and b",
       modifier = Modifier.weight(1f)
      ) {
       VectorPlaneCanvas(va, vb, res.resultMatrix?.flatten())
      }
     }
    } else {
     val parsedA = runCatching { MathEngine.matrix(matA) }.getOrNull()
     if (parsedA != null) {
      WorkbenchCard(
       title = "Inspector: Matrix A (${parsedA.size}×${parsedA[0].size})",
       subtitle = "Automatic structural invariants of A",
       modifier = Modifier.weight(1f)
      ) {
       val isSq = parsedA.size == parsedA[0].size
       val det = if (isSq) formatMath(MathEngine.determinant(parsedA), 6) else "n/a (non-square)"
       val tr = if (isSq) formatMath(MathEngine.trace(parsedA), 6) else "n/a (non-square)"
       val rnk = MathEngine.rank(parsedA).toString()
       val frob = formatMath(sqrt(parsedA.flatten().sumOf { it * it }), 6)
       Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricTile("Determinant", det, modifier = Modifier.weight(1f))
        MetricTile("Trace", tr, modifier = Modifier.weight(1f))
       }
       Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricTile("Rank", "$rnk / ${min(parsedA.size, parsedA[0].size)}", modifier = Modifier.weight(1f))
        MetricTile("Frobenius ‖A‖_F", frob, modifier = Modifier.weight(1f))
       }
      }
     }
    }
   }
  },
  onFailure = { err ->
   WorkbenchCard(title = "Check Input Format", subtitle = err.message ?: "Invalid matrix or vector input") {
    Text(
     "Tip: Separate cells in a row with commas (or spaces) and separate rows with semicolons (;). For example: 1, 2; 3, 4 for a 2×2 matrix, or 3, 4 for a vector.",
     style = AppTypography.body2,
     color = Theme.colors.textSecondary
    )
   }
  }
 )
}

private data class LinalgComputation(
 val headline: String,
 val formula: String,
 val resultMatrix: List<List<Double>>? = null,
 val metrics: List<Pair<String, String>> = emptyList(),
 val steps: List<String> = emptyList()
)

/** Extracts a vector; if the user pasted a multi-row matrix into a vector box, flattens it with a friendly note instead of crashing! */
private fun extractVectorOrFlatten(text: String): Pair<List<Double>, Boolean> {
 val m = MathEngine.matrix(text)
 val isTrueVec = m.size == 1 || m.all { it.size == 1 }
 return m.flatten() to !isTrueVec
}

private fun computeLinalgResult(
 op: LinalgOp,
 matAText: String,
 matBText: String,
 rhsBText: String,
 vecAText: String,
 vecBText: String
): LinalgComputation {
 return when (op) {
  LinalgOp.Multiply -> {
   val a = MathEngine.matrix(matAText); val b = MathEngine.matrix(matBText)
   val prod = MathEngine.multiply(a, b)
   LinalgComputation(
    headline = "Matrix Product A × B (${prod.size}×${prod[0].size})",
    formula = "(AB)ᵢⱼ = ∑ₖ Aᵢₖ Bₖⱼ",
    resultMatrix = prod,
    steps = prod.indices.take(4).map { r ->
     "Row ${r + 1}: " + prod[0].indices.joinToString(",  ") { c ->
      a[r].indices.joinToString(" + ") { k -> "${formatMath(a[r][k])}·${formatMath(b[k][c])}" } + " = ${formatMath(prod[r][c])}"
     }
    }
   )
  }
  LinalgOp.Add -> {
   val a = MathEngine.matrix(matAText); val b = MathEngine.matrix(matBText)
   val sum = MathEngine.addMatrices(a, b)
   LinalgComputation("Matrix Sum A + B (${sum.size}×${sum[0].size})", "(A + B)ᵢⱼ = Aᵢⱼ + Bᵢⱼ", resultMatrix = sum)
  }
  LinalgOp.Subtract -> {
   val a = MathEngine.matrix(matAText); val b = MathEngine.matrix(matBText)
   val diff = MathEngine.subtractMatrices(a, b)
   LinalgComputation("Matrix Difference A − B (${diff.size}×${diff[0].size})", "(A − B)ᵢⱼ = Aᵢⱼ − Bᵢⱼ", resultMatrix = diff)
  }
  LinalgOp.DetTrace -> {
   val a = MathEngine.matrix(matAText)
   val det = MathEngine.determinant(a)
   val tr = MathEngine.trace(a)
   val rnk = MathEngine.rank(a)
   LinalgComputation(
    headline = "Determinant & Trace of A (${a.size}×${a[0].size})",
    formula = "det(A) measures volume scaling; trace(A) = ∑ Aᵢᵢ",
    resultMatrix = a,
    metrics = listOf(
     "det(A)" to formatMath(det, 8),
     "trace(A)" to formatMath(tr, 8),
     "rank(A)" to "$rnk",
     "Invertible?" to if (abs(det) > 1e-11) "Yes (det ≠ 0)" else "No (Singular)"
    ),
    steps = if (a.size == 2) listOf(
     "det(A) = (${formatMath(a[0][0])})(${formatMath(a[1][1])}) − (${formatMath(a[0][1])})(${formatMath(a[1][0])}) = ${formatMath(det, 8)}",
     "trace(A) = ${formatMath(a[0][0])} + ${formatMath(a[1][1])} = ${formatMath(tr, 8)}"
    ) else listOf("Computed via Gaussian elimination with partial pivoting.")
   )
  }
  LinalgOp.Inverse -> {
   val a = MathEngine.matrix(matAText)
   val det = MathEngine.determinant(a)
   val inv = MathEngine.inverse(a)
   LinalgComputation(
    headline = "Inverse Matrix A⁻¹ (${inv.size}×${inv[0].size})",
    formula = "A · A⁻¹ = I   (det(A) = ${formatMath(det, 6)})",
    resultMatrix = inv,
    steps = listOf("Verified: det(A) = ${formatMath(det, 6)} ≠ 0. Reduced [A | I] → [I | A⁻¹] via Gauss-Jordan elimination.")
   )
  }
  LinalgOp.Transpose -> {
   val a = MathEngine.matrix(matAText)
   val t = MathEngine.transpose(a)
   LinalgComputation("Transpose Aᵀ (${t.size}×${t[0].size})", "(Aᵀ)ᵢⱼ = Aⱼᵢ", resultMatrix = t)
  }
  LinalgOp.RrefRank -> {
   val a = MathEngine.matrix(matAText)
   val reduced = MathEngine.rref(a)
   val rnk = MathEngine.rank(a)
   val nullity = a[0].size - rnk
   LinalgComputation(
    headline = "Reduced Row Echelon Form RREF(A)",
    formula = "Rank-Nullity Theorem: rank(A) + nullity(A) = $rnk + $nullity = ${a[0].size}",
    resultMatrix = reduced,
    metrics = listOf("rank(A)" to "$rnk", "nullity(A)" to "$nullity", "Dimensions" to "${a.size}×${a[0].size}")
   )
  }
  LinalgOp.Eigen -> {
   val a = MathEngine.matrix(matAText)
   val eig = MathEngine.eigen2x2(a)
   val metrics = mutableListOf(
    "Trace (λ₁+λ₂)" to formatMath(eig.trace, 6),
    "Det (λ₁·λ₂)" to formatMath(eig.determinant, 6),
    "Discriminant Δ" to formatMath(eig.discriminant, 6)
   )
   val steps = mutableListOf("Characteristic polynomial: λ² − (${formatMath(eig.trace)})λ + (${formatMath(eig.determinant)}) = 0")
   if (eig.realEigenvalues != null) {
    val (l1, l2) = eig.realEigenvalues
    metrics += "λ₁" to formatMath(l1, 6)
    metrics += "λ₂" to formatMath(l2, 6)
    if (eig.eigenvectors.size == 2) {
     steps += "Unit eigenvector v₁ (for λ₁ = ${formatMath(l1, 4)}): [${eig.eigenvectors[0].joinToString(", ") { formatMath(it, 4) }}]"
     steps += "Unit eigenvector v₂ (for λ₂ = ${formatMath(l2, 4)}): [${eig.eigenvectors[1].joinToString(", ") { formatMath(it, 4) }}]"
    }
   } else {
    metrics += "Complex λ₁,₂" to (eig.complexEigenvalueText ?: "")
   }
   LinalgComputation("2×2 Eigenvalue & Eigenvector Analysis", "Av = λv   ⇔   det(A − λI) = 0", resultMatrix = eig.eigenvectors.takeIf { it.isNotEmpty() }, metrics = metrics, steps = steps)
  }
  LinalgOp.Solve -> {
   val a = MathEngine.matrix(matAText)
   val bVec = extractVectorOrFlatten(rhsBText).first
   val xSol = MathEngine.solveLinearSystem(a, bVec)
   LinalgComputation(
    headline = "Linear System Solution x (where Ax = b)",
    formula = "x = A⁻¹b",
    resultMatrix = listOf(xSol),
    metrics = xSol.mapIndexed { idx, v -> "x_${idx + 1}" to formatMath(v, 8) },
    steps = listOf("Solved ${a.size}×${a.size} system via Gaussian elimination with partial pivoting.")
   )
  }
  LinalgOp.Dot -> {
   val (a, flatA) = extractVectorOrFlatten(vecAText)
   val (b, flatB) = extractVectorOrFlatten(vecBText)
   val d = MathEngine.dot(a, b)
   val note = if (flatA || flatB) " (flattened matrix entries — Frobenius inner product)" else ""
   LinalgComputation(
    headline = "Dot Product a · b = ${formatMath(d, 8)}",
    formula = "a · b = ∑ᵢ aᵢ bᵢ$note",
    metrics = listOf(
     "a · b" to formatMath(d, 8),
     "‖a‖₂" to formatMath(MathEngine.norm(a), 6),
     "‖b‖₂" to formatMath(MathEngine.norm(b), 6),
     "Orthogonal?" to if (abs(d) < 1e-9) "Yes (a ⊥ b)" else "No"
    ),
    steps = listOf(
     a.indices.joinToString(" + ") { "(${formatMath(a[it])})(${formatMath(b[it])})" } + " = ${formatMath(d, 8)}"
    )
   )
  }
  LinalgOp.Norm -> {
   val (a, flatA) = extractVectorOrFlatten(vecAText)
   val l2 = MathEngine.norm(a)
   val l1 = MathEngine.norm1(a)
   val linf = MathEngine.normInf(a)
   val unit = if (l2 > 1e-12) listOf(MathEngine.unitVector(a)) else null
   LinalgComputation(
    headline = "Vector Norms & Unit Vector â",
    formula = "‖a‖₂ = √(∑ aᵢ²),   â = a / ‖a‖₂${if (flatA) " (Frobenius norm of matrix)" else ""}",
    resultMatrix = unit,
    metrics = listOf(
     "Euclidean ‖a‖₂" to formatMath(l2, 8),
     "Manhattan ‖a‖₁" to formatMath(l1, 8),
     "Max ‖a‖_∞" to formatMath(linf, 8),
     "‖a‖₂²" to formatMath(l2 * l2, 8)
    ),
    steps = listOf("‖a‖₂ = √(" + a.joinToString(" + ") { "${formatMath(it)}²" } + ") = √(${formatMath(l2 * l2, 6)}) = ${formatMath(l2, 8)}")
   )
  }
  LinalgOp.Angle -> {
   val a = extractVectorOrFlatten(vecAText).first
   val b = extractVectorOrFlatten(vecBText).first
   val cosSim = MathEngine.cosineSimilarity(a, b)
   val rad = MathEngine.angleRadians(a, b)
   val deg = MathEngine.angleDegrees(a, b)
   LinalgComputation(
    headline = "Angle θ = ${formatMath(deg, 4)}° (${formatMath(rad, 6)} rad)",
    formula = "cos(θ) = (a · b) / (‖a‖ ‖b‖)",
    metrics = listOf(
     "Cosine Similarity" to formatMath(cosSim, 8),
     "Angle (Degrees)" to "${formatMath(deg, 4)}°",
     "Angle (Radians)" to "${formatMath(rad, 6)} rad",
     "Dot Product a · b" to formatMath(MathEngine.dot(a, b), 6)
    )
   )
  }
  LinalgOp.Projection -> {
   val a = extractVectorOrFlatten(vecAText).first
   val b = extractVectorOrFlatten(vecBText).first
   val proj = MathEngine.project(a, b)
   val rej = a.indices.map { a[it] - proj[it] }
   val scale = MathEngine.dot(a, b) / MathEngine.dot(b, b)
   LinalgComputation(
    headline = "Orthogonal Projection of a onto b",
    formula = "proj_b(a) = ((a · b) / (b · b)) b",
    resultMatrix = listOf(proj),
    metrics = listOf(
     "Scalar multiplier" to formatMath(scale, 6),
     "‖proj_b(a)‖" to formatMath(MathEngine.norm(proj), 6),
     "Rejection ‖a − proj‖" to formatMath(MathEngine.norm(rej), 6)
    ),
    steps = listOf(
     "Projection proj_b(a) = [${proj.joinToString(", ") { formatMath(it, 6) }}]",
     "Orthogonal rejection a − proj_b(a) = [${rej.joinToString(", ") { formatMath(it, 6) }}]"
    )
   )
  }
  LinalgOp.Cross -> {
   val a = extractVectorOrFlatten(vecAText).first
   val b = extractVectorOrFlatten(vecBText).first
   val c = MathEngine.cross(a, b)
   val area = MathEngine.norm(c)
   LinalgComputation(
    headline = "Cross Product a × b",
    formula = "a × b = [a₂b₃ − a₃b₂,  a₃b₁ − a₁b₃,  a₁b₂ − a₂b₁]",
    resultMatrix = listOf(c),
    metrics = listOf(
     "Parallelogram Area ‖a × b‖" to formatMath(area, 8),
     "Triangle Area ½‖a × b‖" to formatMath(area / 2.0, 8)
    )
   )
  }
  LinalgOp.VecAdd -> {
   val a = extractVectorOrFlatten(vecAText).first
   val b = extractVectorOrFlatten(vecBText).first
   require(a.size == b.size) { "Vectors must have the same dimension (${a.size} vs ${b.size})." }
   val sum = a.indices.map { a[it] + b[it] }
   val diff = a.indices.map { a[it] - b[it] }
   val dist = MathEngine.norm(diff)
   LinalgComputation(
    headline = "Vector Sum, Difference & Euclidean Distance",
    formula = "d(a, b) = ‖a − b‖₂",
    resultMatrix = listOf(sum),
    metrics = listOf(
     "a + b" to "[${sum.joinToString(", ") { formatMath(it, 4) }}]",
     "a − b" to "[${diff.joinToString(", ") { formatMath(it, 4) }}]",
     "Distance ‖a − b‖₂" to formatMath(dist, 8)
    )
   )
  }
 }
}

@Composable private fun MatrixEditorCard(
 title: String,
 text: String,
 onTextChange: (String) -> Unit,
 isVector: Boolean,
 modifier: Modifier = Modifier,
 onSwap: (() -> Unit)? = null
) {
 val parsed = remember(text) { runCatching { MathEngine.matrix(text) }.getOrNull() }
 WorkbenchCard(
  title = title,
  subtitle = if (isVector) "Click any component in the bracket or type comma-separated values" else "Edit cells directly in the matrix grid or type below",
  modifier = modifier,
  headerEnd = {
   if (onSwap != null) SmallChip("Swap A ↔ B", onClick = onSwap)
  }
 ) {
  // Quick dimension / preset buttons
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
   if (isVector) {
    SmallChip("2D [3, 4]") { onTextChange("3, 4") }
    SmallChip("3D [1, 2, 3]") { onTextChange("1, 2, 3") }
    SmallChip("4D") { onTextChange("1, 0, -1, 2") }
    SmallChip("Unit x") { onTextChange("1, 0, 0") }
    SmallChip("Random") {
     val r = List(3) { Random.nextInt(-5, 6) }.joinToString(", ")
     onTextChange(r)
    }
   } else {
    SmallChip("2×2") { onTextChange("1, 2; 3, 4") }
    SmallChip("3×3") { onTextChange("2, -1, 0; -1, 2, -1; 0, -1, 2") }
    SmallChip("2×3") { onTextChange("1, 2, 3; 4, 5, 6") }
    SmallChip("Identity I₂") { onTextChange("1, 0; 0, 1") }
    SmallChip("Identity I₃") { onTextChange("1, 0, 0; 0, 1, 0; 0, 0, 1") }
    SmallChip("Random") {
     val rows = parsed?.size?.coerceIn(2, 4) ?: 2
     val cols = parsed?.firstOrNull()?.size?.coerceIn(2, 4) ?: 2
     val s = List(rows) { List(cols) { Random.nextInt(-4, 6) }.joinToString(", ") }.joinToString("; ")
     onTextChange(s)
    }
   }
  }

  // Interactive visual grid when dimensions are <= 5x5
  if (parsed != null && parsed.size <= 5 && parsed[0].size <= 5) {
   Row(
    Modifier.fillMaxWidth().padding(vertical = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.Center
   ) {
    Text("[", style = DisplayStyle.copy(fontSize = 36.sp), color = Theme.colors.textMuted)
    Column(Modifier.padding(horizontal = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
     parsed.forEachIndexed { r, row ->
      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
       row.forEachIndexed { c, cellVal ->
        var cellText by remember(r, c, cellVal) { mutableStateOf(formatMath(cellVal, 4)) }
        BasicTextField(
         value = cellText,
         onValueChange = { newVal ->
          cellText = newVal.take(12)
          val num = runCatching { ExpressionParser(cellText).value() }.getOrNull()
          if (num != null) {
           val updated = parsed.mapIndexed { ri, rowList ->
            rowList.mapIndexed { ci, old -> if (ri == r && ci == c) num else old }
           }
           onTextChange(updated.joinToString("; ") { rl -> rl.joinToString(", ") { formatMath(it, 6) } })
          }
         },
         singleLine = true,
         textStyle = CodeStyle.copy(fontSize = 14.sp, color = Theme.colors.textPrimary, textAlign = TextAlign.Center),
         cursorBrush = SolidColor(Theme.colors.brandPrimary),
         modifier = Modifier
          .width(64.dp)
          .clip(RoundedCornerShape(8.dp))
          .border(1.dp, Theme.colors.borderStrong, RoundedCornerShape(8.dp))
          .background(Theme.colors.surfaceSecondary)
          .padding(horizontal = 6.dp, vertical = 8.dp)
        )
       }
      }
     }
    }
    Text("]", style = DisplayStyle.copy(fontSize = 36.sp), color = Theme.colors.textMuted)
   }
  }

  OutlinedTextField(
   value = text,
   onValueChange = { onTextChange(it.take(1000)) },
   singleLine = true,
   label = { Text(if (isVector) "Vector components (e.g. 3, 4 or 1, 2, 3)" else "Matrix rows separated by ';' (e.g. 1, 2; 3, 4)") },
   modifier = Modifier.fillMaxWidth(),
   textStyle = CodeStyle
  )
 }
}

@Composable private fun VisualMatrixDisplay(matrix: List<List<Double>>) {
 Surface(
  color = Theme.colors.surfaceSecondary,
  shape = RoundedCornerShape(12.dp),
  modifier = Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp))
 ) {
  SelectionContainer {
   Row(
    Modifier.padding(16.dp).horizontalScroll(rememberScrollState()),
    verticalAlignment = Alignment.CenterVertically
   ) {
    Text("⎡\n⎢\n⎣", style = CodeStyle.copy(fontSize = 20.sp), color = Theme.colors.brandPrimary)
    Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
     matrix.forEach { row ->
      Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
       row.forEach { v ->
        Text(
         formatMath(v, 6).padStart(8, ' '),
         style = CodeStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
         color = Theme.colors.textPrimary
        )
       }
      }
     }
    }
    Text("⎤\n⎥\n⎦", style = CodeStyle.copy(fontSize = 20.sp), color = Theme.colors.brandPrimary)
   }
  }
 }
}

@Composable private fun VectorPlaneCanvas(a: List<Double>, b: List<Double>?, res: List<Double>?) {
 val measurer = rememberTextMeasurer()
 val ink = Theme.colors.textPrimary
 val grid = Theme.colors.borderDefault
 val colorA = Color(0xFF2D70B3)
 val colorB = Color(0xFFFA7E19)
 val colorRes = Theme.colors.brandPrimary
 val maxCoord = remember(a, b, res) {
  val all = buildList {
   add(abs(a[0])); add(abs(a[1]))
   if (b != null && b.size >= 2) { add(abs(b[0])); add(abs(b[1])) }
   if (res != null && res.size >= 2) { add(abs(res[0])); add(abs(res[1])) }
  }
  (all.maxOrNull() ?: 5.0).coerceAtLeast(2.0) * 1.25
 }
 Canvas(
  Modifier
   .fillMaxWidth()
   .height(220.dp)
   .clip(RoundedCornerShape(12.dp))
   .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp))
   .background(Theme.colors.surfaceSecondary)
 ) {
  val cx = size.width / 2f; val cy = size.height / 2f
  val scale = min(size.width, size.height) / 2f / maxCoord.toFloat()
  fun toOffset(x: Double, y: Double) = Offset((cx + x * scale).toFloat(), (cy - y * scale).toFloat())
  drawLine(grid, Offset(0f, cy), Offset(size.width, cy), 1.5f)
  drawLine(grid, Offset(cx, 0f), Offset(cx, size.height), 1.5f)
  fun drawVec(v: List<Double>, color: Color, tag: String) {
   val end = toOffset(v[0], v[1])
   drawLine(color, Offset(cx, cy), end, strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
   drawCircle(color, 4.5.dp.toPx(), end)
   drawText(measurer, "$tag (${formatMath(v[0], 2)}, ${formatMath(v[1], 2)})", end + Offset(6f, -18f), TextStyle(fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold))
  }
  if (b != null && b.size >= 2) drawVec(b, colorB, "b")
  drawVec(a, colorA, "a")
  if (res != null && res.size >= 2) drawVec(res, colorRes, "result")
 }
}

// ───────────────────────────── 4. Statistics & Regression Workbench ─────────────────────────────

private val StatPresets = listOf(
 "Exam Scores" to "72, 75, 78, 80, 82, 85, 85, 88, 90, 92, 95, 98",
 "Bell Curve" to "10, 12, 14, 15, 15, 16, 16, 16, 17, 17, 18, 19, 21",
 "Skewed & Outlier" to "12, 14, 15, 15, 16, 17, 18, 19, 20, 48",
 "ML Losses" to "0.92, 0.74, 0.61, 0.49, 0.41, 0.35, 0.31, 0.28, 0.26, 0.25"
)

@Composable private fun StatisticsWorkbench(repository: ProgressRepository) {
 var mode by remember { mutableStateOf(repository.getSetting("math_stat_mode", "1-Variable Summary")) }
 var rawData by remember { mutableStateOf(repository.getSetting("math_stat_data", StatPresets.first().second)) }
 var regX by remember { mutableStateOf(repository.getSetting("math_reg_x", "1, 2, 3, 4, 5, 6, 7, 8")) }
 var regY by remember { mutableStateOf(repository.getSetting("math_reg_y", "2.1, 3.9, 6.2, 8.0, 9.9, 12.1, 14.0, 15.8")) }

 WorkbenchCard(
  title = "Statistics & Data Analysis Workbench",
  subtitle = "Explore descriptive statistics, quartiles, outliers, histograms, box plots, and linear regression"
 ) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   listOf("1-Variable Summary", "2-Variable Linear Regression (x, y)").forEach { m ->
    SmallChip(m, active = mode == m) { mode = m; repository.setSetting("math_stat_mode", m) }
   }
  }
  if (mode == "1-Variable Summary") {
   Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
    Text("Presets:", style = AppTypography.caption, color = Theme.colors.textMuted)
    StatPresets.forEach { (label, values) ->
     SmallChip(label) { rawData = values; repository.setSetting("math_stat_data", values) }
    }
   }
   OutlinedTextField(
    value = rawData,
    onValueChange = { rawData = it.take(4000); repository.setSetting("math_stat_data", rawData) },
    singleLine = false,
    maxLines = 3,
    label = { Text("Dataset values (separated by commas, spaces, or semicolons)") },
    modifier = Modifier.fillMaxWidth(),
    textStyle = CodeStyle
   )
  } else {
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    OutlinedTextField(
     value = regX,
     onValueChange = { regX = it.take(2000); repository.setSetting("math_reg_x", regX) },
     singleLine = true,
     label = { Text("Predictor X values") },
     modifier = Modifier.weight(1f),
     textStyle = CodeStyle
    )
    OutlinedTextField(
     value = regY,
     onValueChange = { regY = it.take(2000); repository.setSetting("math_reg_y", regY) },
     singleLine = true,
     label = { Text("Response Y values") },
     modifier = Modifier.weight(1f),
     textStyle = CodeStyle
    )
   }
  }
 }

 if (mode == "1-Variable Summary") {
  val analysis = remember(rawData) { runCatching { MathEngine.analyzeData(MathEngine.numberList(rawData)) } }
  analysis.fold(
   onSuccess = { d ->
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
     WorkbenchCard(
      title = "Descriptive Summary (n = ${d.n})",
      subtitle = "Sorted: [${d.sorted.take(12).joinToString(", ") { formatMath(it, 3) }}${if (d.sorted.size > 12) ", …" else ""}]",
      modifier = Modifier.weight(1.2f)
     ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       MetricTile("Mean (μ)", formatMath(d.mean, 6), "Sum = ${formatMath(d.sum, 4)}", Modifier.weight(1f))
       MetricTile("Median (Q2)", formatMath(d.median, 6), "50th percentile", Modifier.weight(1f))
       MetricTile("Mode", if (d.modes.isEmpty()) "None" else d.modes.joinToString { formatMath(it, 4) }, "Most frequent", Modifier.weight(1f))
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       MetricTile("Pop. Std Dev (σ)", formatMath(d.popStdDev, 6), "σ² = ${formatMath(d.popVariance, 6)}", Modifier.weight(1f))
       MetricTile("Sample Std Dev (s)", if (d.n > 1) formatMath(d.sampleStdDev, 6) else "n/a", if (d.n > 1) "s² = ${formatMath(d.sampleVariance, 6)}" else "Needs n ≥ 2", Modifier.weight(1f))
       MetricTile("95% CI for Mean", if (d.n > 1) "[${formatMath(d.ci95Low, 3)}, ${formatMath(d.ci95High, 3)}]" else "n/a", if (d.n > 1) "SEM = ${formatMath(d.stdError, 4)}" else "", Modifier.weight(1f))
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       MetricTile("5-Number Summary", "${formatMath(d.min, 2)} · ${formatMath(d.q1, 2)} · ${formatMath(d.q2, 2)} · ${formatMath(d.q3, 2)} · ${formatMath(d.max, 2)}", "Min · Q1 · Med · Q3 · Max", Modifier.weight(1.4f))
       MetricTile("Range & IQR", "R = ${formatMath(d.range, 4)}", "IQR = ${formatMath(d.iqr, 4)}", Modifier.weight(0.8f))
       MetricTile("Outliers (1.5×IQR)", if (d.outliers.isEmpty()) "None" else d.outliers.joinToString { formatMath(it, 3) }, "Skew = ${formatMath(d.skewness, 3)}", Modifier.weight(0.8f))
      }
     }

     WorkbenchCard(
      title = "Histogram, Bell Curve & Box Plot",
      subtitle = "Green line = Mean (${formatMath(d.mean, 2)}) · Orange dashed = Median (${formatMath(d.median, 2)})",
      modifier = Modifier.weight(1f)
     ) {
      HistogramAndBoxPlotCanvas(d)
     }
    }
   },
   onFailure = { err ->
    WorkbenchCard("Invalid Data Input", err.message ?: "Could not parse numbers") {
     Text("Enter numbers separated by commas or spaces, e.g. 4, 8, 15, 16, 23, 42", style = AppTypography.body2, color = Theme.colors.textSecondary)
    }
   }
  )
 } else {
  val reg = remember(regX, regY) {
   runCatching { MathEngine.linearRegression(MathEngine.numberList(regX), MathEngine.numberList(regY)) }
  }
  reg.fold(
   onSuccess = { r ->
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
     WorkbenchCard(
      title = "Least-Squares Linear Regression (n = ${r.n})",
      subtitle = r.equation,
      modifier = Modifier.weight(1.1f)
     ) {
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       MetricTile("Regression Line", r.equation, "Slope m = ${formatMath(r.slope, 6)}", Modifier.weight(1.2f))
       MetricTile("Correlation (r)", formatMath(r.r, 6), "R² = ${formatMath(r.rSquared, 6)}", Modifier.weight(1f))
      }
      Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
       MetricTile("Intercept (b)", formatMath(r.intercept, 6), "x̄ = ${formatMath(r.meanX, 3)}, ȳ = ${formatMath(r.meanY, 3)}", Modifier.weight(1f))
       MetricTile("Sample Covariance", formatMath(r.covariance, 6), "MSE = ${formatMath(r.residuals.sumOf { it * it } / r.n, 6)}", Modifier.weight(1f))
      }
     }
     WorkbenchCard(
      title = "Scatter Plot & Best-Fit Line",
      subtitle = "${r.equation}   (R² = ${formatMath(r.rSquared * 100, 2)}%)",
      modifier = Modifier.weight(1f)
     ) {
      RegressionScatterCanvas(r)
     }
    }
   },
   onFailure = { err ->
    WorkbenchCard("Check Regression Inputs", err.message ?: "Invalid paired data") {
     Text("Ensure X and Y have the same number of values (at least 2 points).", style = AppTypography.body2, color = Theme.colors.textSecondary)
    }
   }
  )
 }
}

@Composable private fun HistogramAndBoxPlotCanvas(d: DataSummary) {
 val brand = Theme.colors.brandPrimary
 val accent = Color(0xFFFA7E19)
 val ink = Theme.colors.textPrimary
 val border = Theme.colors.borderDefault
 Canvas(
  Modifier
   .fillMaxWidth()
   .height(250.dp)
   .clip(RoundedCornerShape(12.dp))
   .border(1.dp, border, RoundedCornerShape(12.dp))
   .background(Theme.colors.surfaceSecondary)
 ) {
  val padL = 24f; val padR = 24f; val padT = 18f
  val histBottom = size.height * 0.68f
  val boxY = size.height * 0.84f
  val w = (size.width - padL - padR).coerceAtLeast(10f)
  val minV = d.min; val maxV = if (d.max > d.min) d.max else d.min + 1.0
  fun toX(v: Double) = (padL + ((v - minV) / (maxV - minV)).coerceIn(0.0, 1.0) * w).toFloat()

  // Histogram bins
  val binsCount = ceil(sqrt(d.n.toDouble())).toInt().coerceIn(4, 12)
  val bins = IntArray(binsCount)
  for (v in d.sorted) {
   val idx = if (maxV == minV) 0 else (((v - minV) / (maxV - minV)) * binsCount).toInt().coerceIn(0, binsCount - 1)
   bins[idx]++
  }
  val maxBin = (bins.maxOrNull() ?: 1).coerceAtLeast(1)
  val binW = w / binsCount
  val histH = histBottom - padT
  for (i in 0 until binsCount) {
   val bh = (bins[i].toFloat() / maxBin) * (histH - 8f)
   val left = padL + i * binW
   drawRoundRect(
    color = brand.copy(alpha = 0.32f),
    topLeft = Offset(left + 2f, histBottom - bh),
    size = Size((binW - 4f).coerceAtLeast(2f), bh),
    cornerRadius = CornerRadius(4f, 4f)
   )
   drawRoundRect(
    color = brand,
    topLeft = Offset(left + 2f, histBottom - bh),
    size = Size((binW - 4f).coerceAtLeast(2f), bh),
    cornerRadius = CornerRadius(4f, 4f),
    style = Stroke(1.5f)
   )
  }
  // Mean & Median vertical lines
  val meanX = toX(d.mean); val medX = toX(d.median)
  drawLine(brand, Offset(meanX, padT), Offset(meanX, histBottom), strokeWidth = 2.5f)
  drawLine(accent, Offset(medX, padT), Offset(medX, histBottom), strokeWidth = 2.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)))
  drawLine(ink.copy(alpha = 0.4f), Offset(padL, histBottom), Offset(size.width - padR, histBottom), 1.5f)

  // Horizontal Box-and-Whisker Plot below histogram
  val xMin = toX(d.min); val xQ1 = toX(d.q1); val xMed = toX(d.q2); val xQ3 = toX(d.q3); val xMax = toX(d.max)
  val boxH = 22f
  drawLine(ink, Offset(xMin, boxY), Offset(xQ1, boxY), 2f)
  drawLine(ink, Offset(xQ3, boxY), Offset(xMax, boxY), 2f)
  drawLine(ink, Offset(xMin, boxY - 7f), Offset(xMin, boxY + 7f), 2f)
  drawLine(ink, Offset(xMax, boxY - 7f), Offset(xMax, boxY + 7f), 2f)
  drawRoundRect(brand.copy(alpha = 0.25f), Offset(xQ1, boxY - boxH / 2), Size((xQ3 - xQ1).coerceAtLeast(3f), boxH), CornerRadius(4f))
  drawRoundRect(brand, Offset(xQ1, boxY - boxH / 2), Size((xQ3 - xQ1).coerceAtLeast(3f), boxH), CornerRadius(4f), style = Stroke(2f))
  drawLine(accent, Offset(xMed, boxY - boxH / 2), Offset(xMed, boxY + boxH / 2), 3f)
 }
}

@Composable private fun RegressionScatterCanvas(r: RegressionSummary) {
 val brand = Theme.colors.brandPrimary
 val dotColor = Color(0xFF2D70B3)
 val grid = Theme.colors.borderDefault
 Canvas(
  Modifier
   .fillMaxWidth()
   .height(240.dp)
   .clip(RoundedCornerShape(12.dp))
   .border(1.dp, grid, RoundedCornerShape(12.dp))
   .background(Theme.colors.surfaceSecondary)
 ) {
  val pad = 28f
  val xs = r.points.map { it.first }; val ys = r.points.map { it.second }
  val minX = xs.minOrNull() ?: 0.0; val maxX = (xs.maxOrNull() ?: 1.0).let { if (it > minX) it else minX + 1.0 }
  val minY = min(ys.minOrNull() ?: 0.0, min(r.slope * minX + r.intercept, r.slope * maxX + r.intercept))
  val maxY = max(ys.maxOrNull() ?: 1.0, max(r.slope * minX + r.intercept, r.slope * maxX + r.intercept)).let { if (it > minY) it else minY + 1.0 }
  val w = size.width - 2 * pad; val h = size.height - 2 * pad
  fun px(x: Double) = (pad + ((x - minX) / (maxX - minX)) * w).toFloat()
  fun py(y: Double) = (size.height - pad - ((y - minY) / (maxY - minY)) * h).toFloat()
  drawLine(grid, Offset(pad, size.height - pad), Offset(size.width - pad, size.height - pad), 1.5f)
  drawLine(grid, Offset(pad, pad), Offset(pad, size.height - pad), 1.5f)
  // Best-fit line
  drawLine(
   color = brand,
   start = Offset(px(minX), py(r.slope * minX + r.intercept)),
   end = Offset(px(maxX), py(r.slope * maxX + r.intercept)),
   strokeWidth = 2.8.dp.toPx()
  )
  // Data points
  r.points.forEach { (x, y) ->
   drawCircle(dotColor, 5.dp.toPx(), Offset(px(x), py(y)))
  }
 }
}

// ───────────────────────────── 5. Probability & Distributions Workbench ─────────────────────────────

private enum class ProbMode(val title: String) {
 Binomial("Binomial B(n, p)"),
 Normal("Normal N(μ, σ)"),
 Poisson("Poisson Pois(λ)"),
 Geometric("Geometric Geom(p)"),
 Combinatorics("Combinatorics & Bayes")
}

@Composable private fun ProbabilityWorkbench(repository: ProgressRepository) {
 var mode by remember {
  mutableStateOf(
   runCatching { ProbMode.valueOf(repository.getSetting("math_prob_mode", ProbMode.Binomial.name)) }
    .getOrDefault(ProbMode.Binomial)
  )
 }
 var binN by remember { mutableStateOf("12") }
 var binK by remember { mutableStateOf("5") }
 var binP by remember { mutableStateOf("0.4") }

 var normMu by remember { mutableStateOf("0") }
 var normSigma by remember { mutableStateOf("1") }
 var normX by remember { mutableStateOf("1.25") }

 var poisLambda by remember { mutableStateOf("4.5") }
 var poisK by remember { mutableStateOf("4") }

 var geomP by remember { mutableStateOf("0.25") }
 var geomK by remember { mutableStateOf("3") }

 var combN by remember { mutableStateOf("10") }
 var combR by remember { mutableStateOf("3") }
 var bayesPriorA by remember { mutableStateOf("0.01") }
 var bayesBGivenA by remember { mutableStateOf("0.95") }
 var bayesBGivenNotA by remember { mutableStateOf("0.05") }

 WorkbenchCard(
  title = "Probability & Distributions Studio",
  subtitle = "Compute exact & cumulative probabilities, expectations, combinatorics, Bayes' theorem, and distribution plots"
 ) {
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   ProbMode.values().forEach { m ->
    SmallChip(m.title, active = mode == m) { mode = m; repository.setSetting("math_prob_mode", m.name) }
   }
  }
 }

 when (mode) {
  ProbMode.Binomial -> {
   WorkbenchCard("Binomial Distribution X ~ Bin(n, p)", "Number of successes k in n independent Bernoulli trials") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
     OutlinedTextField(binN, { binN = it.take(16) }, singleLine = true, label = { Text("Trials n (0–1000)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
     OutlinedTextField(binK, { binK = it.take(16) }, singleLine = true, label = { Text("Successes k (0–n)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
     OutlinedTextField(binP, { binP = it.take(32) }, singleLine = true, label = { Text("Probability p (0–1, e.g. 0.5 or 1/6)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
    }
    val calc = remember(binN, binK, binP) {
     runCatching {
      val n = MathEngine.wholeNumber(binN, "Trials n")
      val k = MathEngine.wholeNumber(binK, "Successes k")
      val p = ExpressionParser(binP).value()
      val exact = MathEngine.binomial(n, k, p)
      val cdf = (0..k).sumOf { MathEngine.binomial(n, it, p) }.coerceIn(0.0, 1.0)
      val ge = (1.0 - cdf + exact).coerceIn(0.0, 1.0)
      val bars = (0..min(n, 60)).map { i -> i to MathEngine.binomial(n, i, p) }
      Triple(listOf("P(X = $k)" to formatMath(exact, 8), "P(X ≤ $k)" to formatMath(cdf, 8), "P(X ≥ $k)" to formatMath(ge, 8), "Mean np" to formatMath(n * p, 6), "Variance np(1−p)" to formatMath(n * p * (1 - p), 6), "Std Dev σ" to formatMath(sqrt(n * p * (1 - p)), 6)), bars, k)
     }
    }
    calc.fold(
     onSuccess = { (metrics, bars, targetK) ->
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
       metrics.forEach { (l, v) -> MetricTile(l, v) }
      }
      DiscretePmfCanvas(bars, targetK)
     },
     onFailure = { Text("⚠ ${it.message}", color = Theme.colors.error, style = AppTypography.body2) }
    )
   }
  }
  ProbMode.Normal -> {
   WorkbenchCard("Normal (Gaussian) Distribution X ~ N(μ, σ²)", "Continuous bell curve with mean μ and standard deviation σ") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
     OutlinedTextField(normMu, { normMu = it.take(32) }, singleLine = true, label = { Text("Mean μ") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
     OutlinedTextField(normSigma, { normSigma = it.take(32) }, singleLine = true, label = { Text("Std Dev σ (> 0)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
     OutlinedTextField(normX, { normX = it.take(32) }, singleLine = true, label = { Text("Value x") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
    }
    val calc = remember(normMu, normSigma, normX) {
     runCatching {
      val mu = ExpressionParser(normMu).value()
      val s = ExpressionParser(normSigma).value()
      val x = ExpressionParser(normX).value()
      val pdf = MathEngine.normalPdf(x, mu, s)
      val cdf = MathEngine.normalCdf(x, mu, s)
      val z = (x - mu) / s
      val between = abs(MathEngine.normalCdf(mu + abs(z) * s, mu, s) - MathEngine.normalCdf(mu - abs(z) * s, mu, s))
      Triple(mu, s, x) to listOf(
       "Z-score (x−μ)/σ" to formatMath(z, 6),
       "CDF P(X ≤ x)" to formatMath(cdf, 8),
       "Upper Tail P(X ≥ x)" to formatMath(1.0 - cdf, 8),
       "Central P(|Z| ≤ ${formatMath(abs(z), 2)})" to formatMath(between, 8),
       "Density f(x)" to formatMath(pdf, 8)
      )
     }
    }
    calc.fold(
     onSuccess = { (params, metrics) ->
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
       metrics.forEach { (l, v) -> MetricTile(l, v) }
      }
      NormalCurveCanvas(params.first, params.second, params.third)
     },
     onFailure = { Text("⚠ ${it.message}", color = Theme.colors.error, style = AppTypography.body2) }
    )
   }
  }
  ProbMode.Poisson -> {
   WorkbenchCard("Poisson Distribution X ~ Pois(λ)", "Count of events in a fixed interval with average rate λ") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
     OutlinedTextField(poisLambda, { poisLambda = it.take(32) }, singleLine = true, label = { Text("Average rate λ (> 0)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
     OutlinedTextField(poisK, { poisK = it.take(16) }, singleLine = true, label = { Text("Event count k (≥ 0)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
    }
    val calc = remember(poisLambda, poisK) {
     runCatching {
      val lam = ExpressionParser(poisLambda).value()
      val k = MathEngine.wholeNumber(poisK, "Count k")
      val exact = MathEngine.poissonPmf(lam, k)
      val cdf = MathEngine.poissonCdf(lam, k)
      val maxK = max(k + 8, (lam + 4 * sqrt(lam)).toInt()).coerceIn(10, 60)
      val bars = (0..maxK).map { i -> i to MathEngine.poissonPmf(lam, i) }
      Triple(listOf("P(X = $k)" to formatMath(exact, 8), "P(X ≤ $k)" to formatMath(cdf, 8), "P(X ≥ $k)" to formatMath((1.0 - cdf + exact).coerceIn(0.0, 1.0), 8), "Mean λ" to formatMath(lam, 6), "Variance λ" to formatMath(lam, 6), "Std Dev √λ" to formatMath(sqrt(lam), 6)), bars, k)
     }
    }
    calc.fold(
     onSuccess = { (metrics, bars, targetK) ->
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
       metrics.forEach { (l, v) -> MetricTile(l, v) }
      }
      DiscretePmfCanvas(bars, targetK)
     },
     onFailure = { Text("⚠ ${it.message}", color = Theme.colors.error, style = AppTypography.body2) }
    )
   }
  }
  ProbMode.Geometric -> {
   WorkbenchCard("Geometric Distribution X ~ Geom(p)", "Number of trials k until the first success") {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
     OutlinedTextField(geomP, { geomP = it.take(32) }, singleLine = true, label = { Text("Success probability p (0 < p ≤ 1)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
     OutlinedTextField(geomK, { geomK = it.take(16) }, singleLine = true, label = { Text("Trial number k (≥ 1)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
    }
    val calc = remember(geomP, geomK) {
     runCatching {
      val p = ExpressionParser(geomP).value()
      val k = MathEngine.wholeNumber(geomK, "Trial k")
      val exact = MathEngine.geometricPmf(p, k)
      val cdf = MathEngine.geometricCdf(p, k)
      val mean = 1.0 / p; val variance = (1.0 - p) / (p * p)
      val maxK = max(k + 6, (mean * 3).toInt()).coerceIn(8, 45)
      val bars = (1..maxK).map { i -> i to MathEngine.geometricPmf(p, i) }
      Triple(listOf("P(X = $k)" to formatMath(exact, 8), "P(X ≤ $k)" to formatMath(cdf, 8), "P(X ≥ $k)" to formatMath((1.0 - cdf + exact).coerceIn(0.0, 1.0), 8), "Mean 1/p" to formatMath(mean, 6), "Variance (1−p)/p²" to formatMath(variance, 6)), bars, k)
     }
    }
    calc.fold(
     onSuccess = { (metrics, bars, targetK) ->
      Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
       metrics.forEach { (l, v) -> MetricTile(l, v) }
      }
      DiscretePmfCanvas(bars, targetK)
     },
     onFailure = { Text("⚠ ${it.message}", color = Theme.colors.error, style = AppTypography.body2) }
    )
   }
  }
  ProbMode.Combinatorics -> {
   Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
    WorkbenchCard("Combinatorics (nCr, nPr, n!)", "Counting subsets and ordered arrangements", Modifier.weight(1f)) {
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedTextField(combN, { combN = it.take(12) }, singleLine = true, label = { Text("Total items n") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
      OutlinedTextField(combR, { combR = it.take(12) }, singleLine = true, label = { Text("Chosen r") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
     }
     val out = remember(combN, combR) {
      runCatching {
       val n = MathEngine.wholeNumber(combN, "n")
       val r = MathEngine.wholeNumber(combR, "r")
       val c = MathEngine.combinations(n, r)
       val p = MathEngine.permutations(n, r)
       val fact = MathFunctions.gamma(n + 1.0)
       listOf("Combinations C($n, $r)" to formatMath(c, 6), "Permutations P($n, $r)" to formatMath(p, 6), "$n!" to formatMath(fact, 6))
      }
     }
     out.fold(
      onSuccess = { list -> list.forEach { (k, v) -> MetricTile(k, v, modifier = Modifier.fillMaxWidth()) } },
      onFailure = { Text("⚠ ${it.message}", color = Theme.colors.error, style = AppTypography.body2) }
     )
    }

    WorkbenchCard("Bayes' Theorem Calculator", "P(A | B) = P(B | A) P(A) / P(B)", Modifier.weight(1f)) {
     Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      OutlinedTextField(bayesPriorA, { bayesPriorA = it.take(16) }, singleLine = true, label = { Text("Prior P(A)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
      OutlinedTextField(bayesBGivenA, { bayesBGivenA = it.take(16) }, singleLine = true, label = { Text("P(B | A)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
      OutlinedTextField(bayesBGivenNotA, { bayesBGivenNotA = it.take(16) }, singleLine = true, label = { Text("P(B | ¬A)") }, modifier = Modifier.weight(1f), textStyle = CodeStyle)
     }
     val out = remember(bayesPriorA, bayesBGivenA, bayesBGivenNotA) {
      runCatching {
       val pa = ExpressionParser(bayesPriorA).value()
       val pba = ExpressionParser(bayesBGivenA).value()
       val pbna = ExpressionParser(bayesBGivenNotA).value()
       require(pa in 0.0..1.0 && pba in 0.0..1.0 && pbna in 0.0..1.0) { "All probabilities must be in [0, 1]" }
       val pb = pba * pa + pbna * (1.0 - pa)
       require(pb > 0) { "Total probability P(B) must be > 0" }
       val post = (pba * pa) / pb
       listOf(
        "Posterior P(A | B)" to "${formatMath(post, 8)} (${formatMath(post * 100, 2)}%)",
        "Marginal P(B)" to formatMath(pb, 8),
        "Complementary P(¬A | B)" to formatMath(1.0 - post, 8)
       )
      }
     }
     out.fold(
      onSuccess = { list -> list.forEach { (k, v) -> MetricTile(k, v, modifier = Modifier.fillMaxWidth()) } },
      onFailure = { Text("⚠ ${it.message}", color = Theme.colors.error, style = AppTypography.body2) }
     )
    }
   }
  }
 }
}

@Composable private fun DiscretePmfCanvas(bars: List<Pair<Int, Double>>, targetK: Int) {
 val brand = Theme.colors.brandPrimary
 val accent = Color(0xFFFA7E19)
 val muted = Theme.colors.borderStrong
 Canvas(
  Modifier
   .fillMaxWidth()
   .height(210.dp)
   .clip(RoundedCornerShape(12.dp))
   .border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(12.dp))
   .background(Theme.colors.surfaceSecondary)
 ) {
  if (bars.isEmpty()) return@Canvas
  val padL = 24f; val padR = 24f; val padT = 18f; val padB = 24f
  val w = size.width - padL - padR; val h = size.height - padT - padB
  val maxP = (bars.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1e-6)
  val bw = w / bars.size
  bars.forEachIndexed { i, (k, prob) ->
   val bh = ((prob / maxP) * h).toFloat().coerceAtLeast(1f)
   val color = when {
    k == targetK -> accent
    k < targetK -> brand
    else -> muted.copy(alpha = 0.55f)
   }
   drawRoundRect(
    color = color,
    topLeft = Offset(padL + i * bw + 2f, padT + h - bh),
    size = Size((bw - 4f).coerceAtLeast(2f), bh),
    cornerRadius = CornerRadius(3f, 3f)
   )
  }
  drawLine(muted, Offset(padL, padT + h), Offset(size.width - padR, padT + h), 1.5f)
 }
}

@Composable private fun NormalCurveCanvas(mu: Double, sigma: Double, xVal: Double) {
 val brand = Theme.colors.brandPrimary
 val accent = Color(0xFFFA7E19)
 val border = Theme.colors.borderDefault
 Canvas(
  Modifier
   .fillMaxWidth()
   .height(210.dp)
   .clip(RoundedCornerShape(12.dp))
   .border(1.dp, border, RoundedCornerShape(12.dp))
   .background(Theme.colors.surfaceSecondary)
 ) {
  val padL = 24f; val padR = 24f; val padT = 18f; val padB = 24f
  val w = size.width - padL - padR; val h = size.height - padT - padB
  val xMin = mu - 4.0 * sigma; val xMax = mu + 4.0 * sigma
  val maxPdf = MathEngine.normalPdf(mu, mu, sigma)
  fun px(x: Double) = (padL + ((x - xMin) / (xMax - xMin)) * w).toFloat()
  fun py(d: Double) = (padT + h - (d / maxPdf) * h).toFloat()

  // Shaded CDF area for X <= xVal
  val shadeEnd = xVal.coerceIn(xMin, xMax)
  if (shadeEnd > xMin) {
   val shadePath = Path()
   val steps = 160
   val dx = (shadeEnd - xMin) / steps
   shadePath.moveTo(px(xMin), padT + h)
   for (i in 0..steps) {
    val xi = xMin + i * dx
    shadePath.lineTo(px(xi), py(MathEngine.normalPdf(xi, mu, sigma)))
   }
   shadePath.lineTo(px(shadeEnd), padT + h)
   shadePath.close()
   drawPath(shadePath, brand.copy(alpha = 0.28f))
  }

  // Full bell curve stroke
  val curvePath = Path()
  val steps = 240
  val dx = (xMax - xMin) / steps
  for (i in 0..steps) {
   val xi = xMin + i * dx
   val pt = Offset(px(xi), py(MathEngine.normalPdf(xi, mu, sigma)))
   if (i == 0) curvePath.moveTo(pt.x, pt.y) else curvePath.lineTo(pt.x, pt.y)
  }
  drawPath(curvePath, brand, style = Stroke(2.6.dp.toPx()))
  val targetPx = px(xVal.coerceIn(xMin, xMax))
  drawLine(accent, Offset(targetPx, padT), Offset(targetPx, padT + h), 2.5f)
  drawLine(border, Offset(padL, padT + h), Offset(size.width - padR, padT + h), 1.5f)
 }
}
