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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
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
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.labs.*
import com.codequest.academy.shared.ui.components.*
import com.codequest.academy.shared.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

private val MathTools = listOf("Practice", "Graph", "Matrix / vector", "Statistics", "Probability")

@Composable fun MathLabScreen(repository: ProgressRepository) {
 var tool by remember { mutableStateOf(repository.getSetting("math_tool", "Practice").let { if (it in MathTools) it else "Practice" }) }
 Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
  Text("Math Lab", style = DisplayStyle, color = Theme.colors.textPrimary)
  Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
   MathTools.forEach { name ->
    val select = { tool = name; repository.setSetting("math_tool", name) }
    if (tool == name) PrimaryButton(name, select) else SecondaryButton(name, select)
   }
  }
  when (tool) {
   "Graph" -> GraphingCalculator(repository, Modifier.weight(1f).fillMaxWidth())
   "Practice" -> Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) { MathPractice(repository) }
   else -> Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) { MathCalculator(tool) }
  }
 }
}

// ───────────────────────────── Practice ─────────────────────────────

@Composable private fun MathPanel(title: String, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
 Column(modifier.background(Theme.colors.surfacePrimary).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
  Text(title, style = AppTypography.h3, color = Theme.colors.textPrimary); content()
 }
}

@Composable private fun MathPractice(repository: ProgressRepository) {
 var topic by remember { mutableStateOf(runCatching { MathTopic.valueOf(repository.getSetting("math_topic", "Algebra")) }.getOrDefault(MathTopic.Algebra)) }
 var seed by remember { mutableStateOf(repository.getSetting("math_seed", "1").toIntOrNull() ?: 1) }
 var difficulty by remember { mutableStateOf(repository.getSetting("math_difficulty", "1").toIntOrNull()?.coerceIn(1, 3) ?: 1) }
 var mixed by remember { mutableStateOf(false) }
 var reasoning by remember { mutableStateOf(false) }
 val problem = remember(topic, difficulty, seed, reasoning) { MathEngine.generate(topic, difficulty, seed).let { if (reasoning) it.copy(id = it.id + ":reasoning", statement = "Explain and justify the method for this problem:\n" + it.statement, type = MathAnswerType.Explanation) else it } }
 var answer by remember(problem.id) { mutableStateOf(repository.getSetting("math_answer:${problem.id}", "")) }
 var hints by remember(problem.id) { mutableStateOf(0) }
 var verdict by remember(problem.id) { mutableStateOf<MathVerdict?>(null) }
 var start by remember(problem.id) { mutableStateOf(System.currentTimeMillis()) }
 var reviewSaved by remember(problem.id) { mutableStateOf("") }
 var summary by remember(topic) { mutableStateOf(repository.labs.summary("math", topic.name)) }
 fun submit() {
  if (verdict != null || answer.trim().length < (if (reasoning) 10 else 1)) return
  val result = MathEngine.check(problem, answer); verdict = result
  if (!reasoning) { repository.labs.record("math", topic.name, problem.id, answer, result.message, result.correct, hints, System.currentTimeMillis() - start); summary = repository.labs.summary("math", topic.name) }
 }
 Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
  var select by remember { mutableStateOf(false) }
  Box { SecondaryButton(topic.title + " ▾", { select = true }); DropdownMenu(select, { select = false }) { MathTopic.values().forEach { t -> DropdownMenuItem(onClick = { topic = t; repository.setSetting("math_topic", t.name); select = false }) { Text(t.title) } } } }
  for (d in 1..3) { val pick = { difficulty = d; repository.setSetting("math_difficulty", d.toString()) }; if (difficulty == d) PrimaryButton("Level $d", pick) else SecondaryButton("Level $d", pick) }
  SecondaryButton(if (mixed) "Mixed review ✓" else "Mixed review", { mixed = !mixed })
  SecondaryButton(if (reasoning) "Reasoning self-review ✓" else "Reasoning self-review", { reasoning = !reasoning })
 }
 Text("${topic.title} / Problem $seed · Level $difficulty", style = AppTypography.caption, color = Theme.colors.textSecondary)
 Text(problem.statement, style = AppTypography.h2, color = Theme.colors.textPrimary, modifier = Modifier.widthIn(max = 900.dp))
 Surface(color = Theme.colors.surfaceSecondary, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) { SelectionContainer { Text(problem.formula, Modifier.padding(20.dp), style = AppTypography.h3, color = Theme.colors.textPrimary) } }
 problem.graph?.let { GraphPreview(it) }
 Text(when (problem.type) {
  MathAnswerType.Number -> "Enter a number, fraction or arithmetic such as 5/2, 2^3 or sqrt(2). \"x = 3\" is also accepted. Rounded decimals with 2+ places are accepted."
  MathAnswerType.Expression -> "Enter an expression in x, e.g. 6x + 4. Implicit multiplication (2x) and any term order are fine."
  MathAnswerType.Matrix -> "Enter comma-separated cells and semicolon-separated rows: 1,2;3,4  (or [[1,2],[3,4]])"
  MathAnswerType.Vector -> "Enter comma-separated components: 1,2  (or [1, 2])"
  MathAnswerType.Explanation -> "Submit your reasoning before revealing the review rubric. Self-assessments are stored separately from automatically verified mastery."
  else -> "Choose one answer."
 }, style = AppTypography.caption, color = Theme.colors.textSecondary)
 if (problem.type == MathAnswerType.Choice) problem.choices.forEach { choice ->
  OutlinedButton(onClick = { if (verdict == null) answer = choice }, enabled = verdict == null, modifier = Modifier.fillMaxWidth(), border = BorderStroke(if (answer == choice) 2.dp else 1.dp, Theme.colors.borderStrong)) { Text((if (answer == choice) "● " else "○ ") + choice, color = Theme.colors.textPrimary) }
 } else OutlinedTextField(answer, { answer = it.take(256); repository.setSetting("math_answer:${problem.id}", answer) }, label = { Text("Your answer") }, enabled = verdict == null, singleLine = !reasoning,
  modifier = Modifier.fillMaxWidth().widthIn(max = 800.dp).onPreviewKeyEvent { if (!reasoning && it.type == KeyEventType.KeyDown && it.key == Key.Enter) { submit(); true } else false }, textStyle = AppTypography.body1)
 Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
  SecondaryButton("Hint", { hints = (hints + 1).coerceAtMost(problem.steps.size) }, enabled = verdict == null)
  PrimaryButton(if (reasoning) "Submit reasoning" else "Check answer", { submit() }, enabled = answer.trim().length >= (if (reasoning) 10 else 1) && verdict == null)
  SecondaryButton("Retry", { verdict = null; answer = ""; repository.setSetting("math_answer:${problem.id}", ""); reviewSaved = ""; start = System.currentTimeMillis() })
  SecondaryButton("Next problem", { seed++; repository.setSetting("math_seed", seed.toString()); if (mixed) { topic = MathTopic.values()[seed % MathTopic.values().size]; repository.setSetting("math_topic", topic.name) } })
 }
 if (verdict == null) problem.steps.take(hints).forEach { Text("Hint: $it", color = Theme.colors.textPrimary) }
 verdict?.let { v -> MathPanel(if (reasoning) "Reasoning review" else if (v.correct) "✓ Correct" else "× Review your answer", Modifier.fillMaxWidth().border(1.dp, Theme.colors.borderStrong, RoundedCornerShape(16.dp))) {
  Text(v.message, color = Theme.colors.textPrimary); problem.steps.forEachIndexed { i, step -> Text("${i + 1}. $step", style = AppTypography.body1, color = Theme.colors.textPrimary) }
  if (reasoning) {
   Text("Rubric: name the governing principle, show each transformation, and justify the result and its assumptions.", color = Theme.colors.textPrimary)
   Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Needs Review", "Mastered").forEach { choice -> SecondaryButton(choice, { repository.labs.record("math-self-review", topic.name, problem.id, answer, "Self-assessed: $choice", choice == "Mastered", hints, System.currentTimeMillis() - start); reviewSaved = "Saved locally: $choice (self-assessed)" }, enabled = reviewSaved.isEmpty()) } }
   if (reviewSaved.isNotEmpty()) Text(reviewSaved, color = Theme.colors.textSecondary)
  }
 } }
 Text("Topic mastery ${summary.mastery}/100 · ${summary.correct} correct / ${summary.attempts} recent attempts", style = AppTypography.body2, color = Theme.colors.textSecondary)
 LinearProgressIndicator(summary.mastery / 100f, Modifier.fillMaxWidth(), color = Theme.colors.brandPrimary, backgroundColor = Theme.colors.surfaceTertiary)
}

@Composable private fun GraphPreview(expression: String) {
 val viewport = remember(expression) { GraphViewport() }
 val compiled = remember(expression) { GraphCompiler.compile(listOf(expression)) }
 Box(Modifier.fillMaxWidth().height(320.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp))) {
  GraphCanvas(compiled, listOf(true), listOf(graphColor(1)), viewport, selectedIndex = 0, wheelZoom = false, modifier = Modifier.fillMaxSize())
  ZoomControls(viewport, Modifier.align(Alignment.TopEnd).padding(8.dp))
 }
 Text("Graph of y = $expression · drag to move, use + / − to zoom, hover the curve to read values", style = AppTypography.caption, color = Theme.colors.textSecondary)
}

// ───────────────────────────── Graphing calculator ─────────────────────────────

private val GraphPalette = listOf(Color(0xFFC74440), Color(0xFF2D70B3), Color(0xFF388C46), Color(0xFF6042A6), Color(0xFFFA7E19), Color(0xFF000000))
@Composable private fun graphColor(id: Int): Color { val c = GraphPalette[((id % 6) + 6) % 6]; return if (c == Color(0xFF000000) && Theme.colors.isDark) Color.White else c }

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
 "y = 2x + 1" to "Line", "y = x^3 - 3x" to "Cubic", "y = 1/x" to "Hyperbola (asymptotes)", "y = tan(x)" to "Tangent",
 "y = sqrt(x)" to "Square root", "y = |x - 2|" to "Absolute value", "y = e^x" to "Exponential", "y = ln(x)" to "Natural log",
 "x^2 + y^2 = 25" to "Circle", "x^2/9 + y^2/4 = 1" to "Ellipse", "y^2 = x" to "Sideways parabola", "x = 3" to "Vertical line",
 "y > x^2" to "Shaded region", "x^2 + y^2 < 4" to "Disk", "f(x) = x^2 - 4" to "Define a function", "y = f'(x)" to "Derivative of f",
 "y = {x < 0: -x, x^2}" to "Piecewise", "y = sin(x) {0 < x < 2pi}" to "Domain restriction", "(2, 3)" to "Point", "y = m x + b" to "Sliders m, b"
)

@Composable private fun GraphingCalculator(repository: ProgressRepository, modifier: Modifier) {
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
 var selectedId by remember { mutableStateOf<Int?>(null) }
 var focusId by remember { mutableStateOf<Int?>(null) }
 var showHelp by remember { mutableStateOf(false) }
 var examples by remember { mutableStateOf(false) }
 val requesters = remember { mutableMapOf<Int, FocusRequester>() }
 val texts = rows.map { it.text }
 val compiled = remember(texts) { GraphCompiler.compile(texts) }
 val snapshot = rows.toList()
 LaunchedEffect(snapshot) { delay(500); repository.setSetting("math_graph_rows", snapshot.joinToString("\n") { (if (it.visible) "" else HiddenPrefix) + it.text.replace('\n', ' ') }) }
 LaunchedEffect(focusId) { val id = focusId ?: return@LaunchedEffect; delay(30); runCatching { requesters[id]?.requestFocus() }; focusId = null }

 fun insertAfter(index: Int, text: String = ""): Int { val id = nextId++; rows.add((index + 1).coerceIn(0, rows.size), GraphRow(id, text)); return id }
 fun addSliders(index: Int, names: List<String>) {
  var at = index
  names.filter { n -> rows.none { r -> MathText.normalize(r.text).replace(" ", "").startsWith("$n=") } }.forEach { insertAfter(at, "$it = 1"); at++ }
 }
 fun appendExpression(text: String) {
  val emptyIndex = rows.indexOfLast { it.text.isBlank() }
  if (emptyIndex >= 0) rows[emptyIndex] = rows[emptyIndex].copy(text = text) else rows.add(GraphRow(nextId++, text))
  if (rows.last().text.isNotBlank()) rows.add(GraphRow(nextId++, ""))
  val idx = rows.indexOfFirst { it.text == text }
  if (idx >= 0) compiled.lines.getOrNull(idx) // compiled on next frame
 }

 val colors = rows.map { graphColor(it.id) }
 val selectedIndex = rows.indexOfFirst { it.id == selectedId }.takeIf { it >= 0 }

 Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
  // ── Expression list ──
  Column(Modifier.width(400.dp).fillMaxHeight().clip(RoundedCornerShape(14.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp)).background(Theme.colors.surfacePrimary)) {
   Row(Modifier.fillMaxWidth().background(Theme.colors.surfaceSecondary).padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
    SmallChip("+ Add") { focusId = insertAfter(rows.size - 1) }
    Box { SmallChip("Examples ▾") { examples = true }; DropdownMenu(examples, { examples = false }) { GraphExamples.forEach { (expr, label) -> DropdownMenuItem(onClick = { appendExpression(expr); examples = false }) { Text("$label   ", color = Theme.colors.textSecondary, style = AppTypography.caption); Text(expr, style = CodeStyle) } } } }
    SmallChip(if (showHelp) "Help ✓" else "Help") { showHelp = !showHelp }
    Spacer(Modifier.weight(1f))
    SmallChip("Clear") { rows.clear(); rows.add(GraphRow(nextId++, "")); selectedId = null }
   }
   if (showHelp) GraphHelp()
   Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
    rows.forEachIndexed { index, row ->
     val line = compiled.lines.getOrNull(index) ?: GraphLine(GraphItem.Blank)
     val fr = requesters.getOrPut(row.id) { FocusRequester() }
     key(row.id) {
      ExpressionRow(index, row, line, colors[index], row.id == selectedId, fr,
       onText = { t -> val i = rows.indexOfFirst { it.id == row.id }; if (i >= 0) { rows[i] = rows[i].copy(text = t); if (i == rows.size - 1 && t.isNotBlank()) rows.add(GraphRow(nextId++, "")) } },
       onToggle = { val i = rows.indexOfFirst { it.id == row.id }; if (i >= 0) rows[i] = rows[i].copy(visible = !rows[i].visible) },
       onDelete = { val i = rows.indexOfFirst { it.id == row.id }; if (i >= 0) { rows.removeAt(i); if (rows.isEmpty()) rows.add(GraphRow(nextId++, "")); focusId = rows[(i - 1).coerceAtLeast(0)].id } },
       onEnter = { val i = rows.indexOfFirst { it.id == row.id }; focusId = if (i == rows.size - 1 || rows[i + 1].text.isNotBlank()) insertAfter(i) else rows[i + 1].id },
       onMove = { delta -> val i = rows.indexOfFirst { it.id == row.id } + delta; if (i in rows.indices) focusId = rows[i].id },
       onFocus = { selectedId = row.id },
       onAddSliders = { names -> addSliders(rows.indexOfFirst { it.id == row.id }, names) })
     }
    }
    Spacer(Modifier.height(80.dp).fillMaxWidth().clickable { focusId = rows.last().id })
   }
  }
  // ── Graph ──
  Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(14.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(14.dp))) {
   GraphCanvas(compiled, rows.map { it.visible }, colors, viewport, selectedIndex, wheelZoom = true, modifier = Modifier.fillMaxSize())
   ZoomControls(viewport, Modifier.align(Alignment.TopEnd).padding(10.dp))
  }
 }
}

@Composable private fun SmallChip(text: String, onClick: () -> Unit) {
 Box(Modifier.clip(RoundedCornerShape(8.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(8.dp)).background(Theme.colors.surfacePrimary).clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 6.dp)) {
  Text(text, style = AppTypography.caption, color = Theme.colors.textPrimary, fontWeight = FontWeight.SemiBold)
 }
}

@Composable private fun GraphHelp() {
 val tips = listOf(
  "y = 2x + 1,  y = x^2,  y = sin(x),  y = |x|" to "functions of x (\"y =\" is optional)",
  "x^2 + y^2 = 9,  xy = 1" to "any equation in x and y",
  "y > x^2,  x^2 + y^2 <= 4" to "shaded inequalities",
  "x = y^2,  x = 3" to "functions of y / vertical lines",
  "a = 2,  y = a x" to "sliders (unknown letters offer a slider)",
  "f(x) = x^3,  y = f(x - 1),  y = f'(x)" to "your own functions and derivatives",
  "{x < 0: -x, x^2},  sin(x) {x > 0}" to "piecewise and restrictions",
  "(1, 2)" to "points;  2^10 shows its value",
  "sqrt cbrt ln log log_2 exp abs floor ceil round sin cos tan sec csc cot asin acos atan sinh cosh tanh min max mod pi e n!" to "built-ins"
 )
 Column(Modifier.fillMaxWidth().background(Theme.colors.surfaceSecondary).padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
  tips.forEach { (code, note) -> Text(code, style = CodeStyle.copy(fontSize = 12.sp), color = Theme.colors.textPrimary); Text(note, style = AppTypography.caption, color = Theme.colors.textSecondary) }
  Text("Graph: drag to pan, mouse wheel to zoom, hover a curve to trace it. Select an expression to see its roots, extrema and intersections.", style = AppTypography.caption, color = Theme.colors.textSecondary)
 }
}

@Composable private fun ExpressionRow(
 index: Int, row: GraphRow, line: GraphLine, color: Color, selected: Boolean, focusRequester: FocusRequester,
 onText: (String) -> Unit, onToggle: () -> Unit, onDelete: () -> Unit, onEnter: () -> Unit, onMove: (Int) -> Unit,
 onFocus: () -> Unit, onAddSliders: (List<String>) -> Unit
) {
 val item = line.item
 val drawable = line.drawable && line.missing.isEmpty()
 Column(Modifier.fillMaxWidth().background(if (selected) Theme.colors.surfaceSecondary else Theme.colors.surfacePrimary)) {
  Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 6.dp, end = 4.dp, top = 4.dp, bottom = 2.dp)) {
   Text("${index + 1}", style = AppTypography.caption, color = Theme.colors.textMuted, modifier = Modifier.width(22.dp))
   Box(Modifier.size(22.dp).clip(CircleShape)
    .background(if (drawable && row.visible) color else Color.Transparent)
    .border(2.dp, if (drawable) color else Theme.colors.borderDefault, CircleShape)
    .clickable(enabled = drawable, onClick = onToggle)
    .semantics { contentDescription = if (row.visible) "Hide expression ${index + 1}" else "Show expression ${index + 1}" })
   Spacer(Modifier.width(10.dp))
   BasicTextField(
    value = row.text, onValueChange = { onText(it.replace("\n", "").take(500)) }, singleLine = true,
    textStyle = CodeStyle.copy(color = Theme.colors.textPrimary, fontSize = 16.sp),
    cursorBrush = SolidColor(Theme.colors.textPrimary),
    modifier = Modifier.weight(1f).focusRequester(focusRequester).onFocusChanged { if (it.isFocused) onFocus() }
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
    decorationBox = { inner -> Box(Modifier.padding(vertical = 10.dp)) { if (row.text.isEmpty()) Text(if (index == 0) "Type an equation, e.g. y = x^2" else "", style = CodeStyle.copy(fontSize = 16.sp), color = Theme.colors.textMuted); inner() } }
   )
   Text("×", color = Theme.colors.textMuted, style = AppTypography.h3, modifier = Modifier.clip(CircleShape).clickable(onClick = onDelete).padding(horizontal = 8.dp, vertical = 2.dp).semantics { contentDescription = "Delete expression ${index + 1}" })
  }
  val info: Pair<String, Color>? = when (item) {
   is GraphItem.Invalid -> "⚠ ${item.message}" to Theme.colors.error
   is GraphItem.Value -> "= ${formatMath(item.value, 10)}" to Theme.colors.textSecondary
   is GraphItem.Constant -> "${item.name} = ${formatMath(item.value, 10)}" to Theme.colors.textSecondary
   is GraphItem.Point -> "point (${formatMath(item.x, 4)}, ${formatMath(item.y, 4)})" to Theme.colors.textSecondary
   is GraphItem.Definition -> if (item.plot == null && line.missing.isEmpty()) "function ${item.name}(${item.params.joinToString()})" to Theme.colors.textSecondary else null
   is GraphItem.Implicit -> if (item.relation.isInequality) "region ${item.relation.symbol}" to Theme.colors.textMuted else null
   else -> null
  }
  info?.let { (text, c) -> Text(text, style = AppTypography.caption, color = c, modifier = Modifier.padding(start = 60.dp, end = 10.dp, bottom = 6.dp)) }
  if (item is GraphItem.Slider) SliderRow(item, onText)
  if (line.missing.isNotEmpty()) Row(Modifier.padding(start = 60.dp, bottom = 8.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
   Text("add slider:", style = AppTypography.caption, color = Theme.colors.textSecondary)
   line.missing.forEach { name -> SmallChip(name) { onAddSliders(listOf(name)) } }
   if (line.missing.size > 1) SmallChip("all") { onAddSliders(line.missing) }
  }
  Divider(color = Theme.colors.borderDefault)
 }
}

@Composable private fun SliderRow(item: GraphItem.Slider, onText: (String) -> Unit) {
 val lo = min(-10.0, floor(item.value)); val hi = max(10.0, ceil(item.value))
 val step = GraphSampler.niceStep((hi - lo) / 200)
 Row(Modifier.padding(start = 56.dp, end = 12.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
  Text(formatMath(lo), style = AppTypography.caption, color = Theme.colors.textMuted)
  Slider(item.value.toFloat().coerceIn(lo.toFloat(), hi.toFloat()), { v ->
   val snapped = (v / step).roundToLong() * step
   onText("${item.name} = ${formatMath(snapped, 6)}")
  }, valueRange = lo.toFloat()..hi.toFloat(), modifier = Modifier.weight(1f).padding(horizontal = 6.dp),
   colors = SliderDefaults.colors(thumbColor = Theme.colors.brandPrimary, activeTrackColor = Theme.colors.brandPrimary))
  Text(formatMath(hi), style = AppTypography.caption, color = Theme.colors.textMuted)
 }
}

@Composable private fun ZoomControls(viewport: GraphViewport, modifier: Modifier) {
 Column(modifier.clip(RoundedCornerShape(10.dp)).border(1.dp, Theme.colors.borderDefault, RoundedCornerShape(10.dp)).background(Theme.colors.surfacePrimary)) {
  listOf("+" to "Zoom in", "−" to "Zoom out", "⌂" to "Default view").forEach { (label, desc) ->
   Box(Modifier.size(38.dp).clickable { when (label) { "+" -> viewport.zoomAt(0.6); "−" -> viewport.zoomAt(1 / 0.6); else -> viewport.reset() } }.semantics { contentDescription = desc }, contentAlignment = Alignment.Center) {
    Text(label, style = AppTypography.h3, color = Theme.colors.textPrimary)
   }
  }
 }
}

// ── Rendering ──

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
 fun fnOfX(expr: Expr): (Double) -> Double { val ev = Evaluator(ctx); return { x -> try { ev.at(x).eval(expr) } catch (_: Exception) { Double.NaN } } }
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
     while (px < t.w) { val y = f(t.dataX(px + 1f)); if (y.isFinite()) { val py = t.py(y).coerceIn(0f, t.h); rects += if (above) Rect(px, 0f, px + 2f, py) else Rect(px, py, px + 2f, t.h) }; px += 2f }
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
     while (py < t.h) { val x = g(t.dataY(py + 1f)); if (x.isFinite()) { val px = t.px(x).coerceIn(0f, t.w); rects += if (right) Rect(px, py, t.w, py + 2f) else Rect(0f, py, px, py + 2f) }; py += 2f }
     shades += SceneShade(color, rects)
    }
    val lines = GraphSampler.sample(g, t.yMin, t.yMax, t.h.toInt().coerceIn(200, 2400), t.xMin, t.xMax)
    strokes += SceneStroke(color, polylinePath(lines, { _, x -> t.px(x) }, { y, _ -> t.py(y) }), item.relation.strict)
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
       val inside = k < cols && run { val v = (field[k, j] + field[k + 1, j] + field[k, j + 1] + field[k + 1, j + 1]) / 4; v.isFinite() && item.relation.holds(v) }
       if (inside && runStart < 0) runStart = k
       if (!inside && runStart >= 0) { val top = t.h - (j + 1) * ch; rects += Rect(runStart * cw, top, k * cw, top + ch); runStart = -1 }
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
 // Points of interest for the selected explicit curve (like clicking a curve in Desmos)
 val sf = selected?.let { explicitFns[it] }
 if (sf != null) {
  val others = explicitFns.filterKeys { it != selected }.values.toList()
  GraphSampler.pointsOfInterest(sf, others, t.xMin, t.xMax, t.yMin, t.yMax).forEach { p ->
   if (p.y in t.yMin..t.yMax) pois += ScenePoi(Offset(t.px(p.x), t.py(p.y)), p)
  }
 }
 return Scene(t, shades, strokes, dots, traces, pois)
}

@Composable private fun GraphCanvas(compiled: CompiledGraph, visible: List<Boolean>, colors: List<Color>, viewport: GraphViewport, selectedIndex: Int?, wheelZoom: Boolean, modifier: Modifier) {
 val measurer = rememberTextMeasurer()
 var hover by remember { mutableStateOf<Offset?>(null) }
 val bg = Theme.colors.surfacePrimary
 val ink = Theme.colors.textPrimary
 val labelStyle = TextStyle(fontSize = 11.sp, color = Theme.colors.textSecondary)
 val w = viewport.width; val h = viewport.height
 val scene = if (w > 0 && h > 0) {
  val upp = 2 * viewport.radius / w
  val t = GraphTransform(viewport.cx - w / 2.0 * upp, viewport.cy + h / 2.0 * upp, upp, w.toFloat(), h.toFloat())
  remember(compiled, visible, colors, viewport.cx, viewport.cy, viewport.radius, w, h, selectedIndex) { buildScene(compiled, visible, colors, t, selectedIndex) }
 } else null
 Box(modifier.background(bg).onSizeChanged { s: IntSize -> viewport.width = s.width; viewport.height = s.height }
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
  }) {
  if (scene != null) {
   Canvas(Modifier.fillMaxSize()) {
    drawGrid(scene.t, ink, bg, measurer, labelStyle)
    clipRectSafe {
     scene.shades.forEach { s -> s.rects.forEach { r -> drawRect(s.color.copy(alpha = 0.16f), r.topLeft, r.size) } }
     val dash = PathEffect.dashPathEffect(floatArrayOf(12f, 9f))
     scene.strokes.forEach { s -> drawPath(s.path, s.color, style = Stroke(width = 2.6.dp.toPx(), pathEffect = if (s.dashed) dash else null, join = androidx.compose.ui.graphics.StrokeJoin.Round, cap = androidx.compose.ui.graphics.StrokeCap.Round)) }
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
     val kind = when (p.kind) { PoiKind.Root -> "root"; PoiKind.Maximum -> "maximum"; PoiKind.Minimum -> "minimum"; PoiKind.YIntercept -> "y-intercept"; PoiKind.Intersection -> "intersection" }
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

private inline fun DrawScope.clipRectSafe(block: DrawScope.() -> Unit) { drawContext.canvas.save(); drawContext.canvas.clipRect(0f, 0f, size.width, size.height); block(); drawContext.canvas.restore() }

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, at: Offset, ink: Color, bg: Color) {
 val layout = measurer.measure(text, TextStyle(fontSize = 12.sp, color = ink, fontWeight = FontWeight.Medium))
 val pad = 5f
 val x = at.x.coerceIn(2f, (size.width - layout.size.width - 2 * pad - 2f).coerceAtLeast(2f))
 val y = at.y.coerceIn(2f, (size.height - layout.size.height - 2 * pad - 2f).coerceAtLeast(2f))
 drawRoundRect(bg.copy(alpha = 0.92f), Offset(x, y), Size(layout.size.width + 2 * pad, layout.size.height + 2 * pad), androidx.compose.ui.geometry.CornerRadius(6f))
 drawRoundRect(ink.copy(alpha = 0.25f), Offset(x, y), Size(layout.size.width + 2 * pad, layout.size.height + 2 * pad), androidx.compose.ui.geometry.CornerRadius(6f), style = Stroke(1f))
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

// ───────────────────────────── Other tools ─────────────────────────────

@Composable private fun MathCalculator(tool: String) {
 var a by remember(tool) { mutableStateOf(when (tool) { "Matrix / vector" -> "1,2;3,4"; "Statistics" -> "4, 8, 15, 16, 23, 42"; else -> "10" }) }
 var b by remember(tool) { mutableStateOf(if (tool == "Matrix / vector") "1,0;0,1" else "3") }
 var probability by remember { mutableStateOf("0.5") }
 var operation by remember { mutableStateOf("Matrix product") }
 var result by remember(tool) { mutableStateOf("") }
 var isError by remember(tool) { mutableStateOf(false) }
 fun calculate() {
  val r = runCatching {
   when (tool) {
    "Statistics" -> MathEngine.statistics(MathEngine.numberList(a))
    "Matrix / vector" -> when (operation) {
     "Dot product" -> "a · b = ${formatMath(MathEngine.dot(MathEngine.vector(a), MathEngine.vector(b)), 10)}"
     "Vector norm" -> "‖a‖ = ${formatMath(MathEngine.norm(MathEngine.vector(a)), 10)}"
     else -> MathEngine.multiply(MathEngine.matrix(a), MathEngine.matrix(b)).joinToString("\n") { row -> row.joinToString("   ", "[ ", " ]") { formatMath(it, 8) } }
    }
    else -> MathEngine.binomialReport(a, b, probability)
   }
  }
  isError = r.isFailure
  result = r.getOrElse { "Input error: ${it.message ?: "invalid input"}" }
 }
 Text(tool, style = AppTypography.h2, color = Theme.colors.textPrimary)
 if (tool == "Matrix / vector") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Matrix product", "Dot product", "Vector norm").forEach { name -> SecondaryButton((if (operation == name) "✓ " else "") + name, { operation = name; result = "" }) } }
 val enter = Modifier.fillMaxWidth().widthIn(max = 800.dp).onPreviewKeyEvent { if (it.type == KeyEventType.KeyDown && it.key == Key.Enter) { calculate(); true } else false }
 OutlinedTextField(a, { a = it.take(2000) }, singleLine = true, label = { Text(when (tool) { "Statistics" -> "Values separated by commas, spaces or semicolons"; "Matrix / vector" -> if (operation == "Matrix product") "Matrix A, e.g. 1,2;3,4 or [[1,2],[3,4]]" else "Vector a, e.g. 3,4"; else -> "Trials n (0–1000)" }) }, modifier = enter)
 if (tool == "Probability" || (tool == "Matrix / vector" && operation != "Vector norm")) OutlinedTextField(b, { b = it.take(2000) }, singleLine = true, label = { Text(if (tool == "Probability") "Successes k" else if (operation == "Matrix product") "Matrix B" else "Vector b") }, modifier = enter)
 if (tool == "Probability") OutlinedTextField(probability, { probability = it.take(64) }, singleLine = true, label = { Text("Probability p (0–1), e.g. 0.5 or 1/6") }, modifier = enter)
 PrimaryButton("Calculate", { calculate() })
 if (result.isNotEmpty()) SelectionContainer { Text(result, style = CodeStyle, color = if (isError) Theme.colors.error else Theme.colors.textPrimary) }
}
