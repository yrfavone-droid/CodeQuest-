package com.codequest.academy.shared.ui.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import com.codequest.academy.shared.data.ProgressRepository
import com.codequest.academy.shared.labs.*
import com.codequest.academy.shared.ui.components.*
import com.codequest.academy.shared.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.math.*

@Composable private fun LabPanel(title:String,modifier:Modifier=Modifier,content:@Composable ColumnScope.()->Unit) {
 Column(modifier.background(Theme.colors.surfacePrimary).padding(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
  Text(title,style=AppTypography.h3,color=Theme.colors.textPrimary);content()
 }
}
@Composable private fun DividerHandle(label:String,onDrag:(Float)->Unit) {
 val density=LocalDensity.current
 Box(Modifier.width(8.dp).fillMaxHeight().background(Theme.colors.borderDefault).keyboardFocusRing().semantics {contentDescription="$label. Use left and right arrow keys."}.onKeyEvent {if(it.type==KeyEventType.KeyDown && it.key in listOf(Key.DirectionLeft,Key.DirectionRight)){onDrag(if(it.key==Key.DirectionLeft)-10f else 10f);true}else false}.focusable().pointerInput(Unit) {detectDragGestures {change,amount->change.consume();onDrag(with(density){amount.x.toDp().value})}})
}
@Composable fun PythonLabScreen(repository:ProgressRepository, navigation:com.codequest.academy.shared.ui.navigation.Navigation) {
 val store=repository.labs;val scope=rememberCoroutineScope()
 var selected by remember {mutableStateOf(repository.getSetting("python_exercise","py-types"))}
 val exercise=PythonExercises.all.firstOrNull {it.id==selected}?:PythonExercises.all.first()
 var source by remember {mutableStateOf(store.projects().firstOrNull {it.id=="autosave"}?.source ?: repository.getSetting("python_lab_draft",exercise.starter))}
 val latestSource by rememberUpdatedState(source)
 var stdin by remember {mutableStateOf(repository.getSetting("python_stdin",""))}
 var running by remember {mutableStateOf(false)};var result by remember {mutableStateOf<PythonResult?>(null)}
 var hints by remember {mutableStateOf(0)};var showProjects by remember {mutableStateOf(false)};var projectName by remember {mutableStateOf("My Python project")}
 var notice by remember {mutableStateOf("")};var left by remember {mutableStateOf(true)};var right by remember {mutableStateOf(true)}
 var leftWidth by remember {mutableStateOf(repository.getSetting("python_left_width","250").toFloatOrNull()?:250f)}
 var rightWidth by remember {mutableStateOf(repository.getSetting("python_right_width","300").toFloatOrNull()?:300f)}
 var summary by remember(selected) {mutableStateOf(store.summary("python",selected))}
 fun save() {store.saveProject("autosave","Last workspace",source);store.saveProject("project:${projectName.trim()}",projectName.trim().ifEmpty {"My Python project"},source);notice="Project saved locally"}
 fun execute(tests:Boolean) {
  if(running)return
  val submitted=source;val currentExercise=exercise;val usedHints=hints;val input=stdin
  running=true;result=null
  scope.launch {
   val start=System.currentTimeMillis();val answer=PythonRunner.run(PythonRequest(submitted,input,if(tests) currentExercise.tests else emptyList()))
   result=answer;running=false
   if(tests) {val passed=answer.status=="success" && answer.tests.size==currentExercise.tests.size && answer.tests.all {it.passed};store.record("python",currentExercise.id,currentExercise.id,submitted,Json.encodeToString(answer),passed,usedHints,System.currentTimeMillis()-start);summary=store.summary("python",selected)}
  }
 }
 LaunchedEffect(source) {delay(400);store.saveProject("autosave","Last workspace",source)}
 DisposableEffect(Unit) {onDispose {store.saveProject("autosave","Last workspace",latestSource);PythonRunner.stop()}}
 Column(Modifier.fillMaxSize().background(Theme.colors.appBackground)) {
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp),verticalAlignment=Alignment.CenterVertically) {
   SecondaryButton(if(left) "Hide instructions" else "Instructions",{left=!left})
   if(PythonRunner.available()) {PrimaryButton(if(running) "Running…" else "Run",{execute(false)},enabled=!running);SecondaryButton("Run tests",{execute(true)},enabled=!running)}
   SecondaryButton("Stop",{PythonRunner.stop()},enabled=running)
   SecondaryButton("Reset",{source=exercise.starter;result=null;hints=0},enabled=!running)
   SecondaryButton("Projects",{showProjects=true},enabled=!running)
   SecondaryButton("Open .py",{runCatching {PythonRunner.chooseOpen()?.let {source=it}}.onFailure {notice=it.message.orEmpty()}},enabled=!running)
   SecondaryButton("Save .py",{runCatching {PythonRunner.chooseSave(source)}.onFailure {notice=it.message.orEmpty()}})
   SecondaryButton(if(right) "Hide output" else "Output",{right=!right})
  }
  if(!PythonRunner.available()) Text("Execution unavailable: this platform does not have a verified isolated runtime.",Modifier.padding(12.dp),color=Theme.colors.textPrimary)
  if(notice.isNotEmpty()) Text(notice,Modifier.padding(horizontal=16.dp),style=AppTypography.caption,color=Theme.colors.textSecondary)
  BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
   val narrow=maxWidth<1100.dp
   Row(Modifier.fillMaxSize()) {
    if(left) {LabPanel("Exercises",Modifier.width((if(narrow) min(leftWidth,220f) else leftWidth).dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
      var expanded by remember {mutableStateOf(false)}
      Box {SecondaryButton(exercise.title,{expanded=true},enabled=!running);DropdownMenu(expanded,{expanded=false}) {PythonExercises.all.forEach {e->DropdownMenuItem(onClick={store.saveProject("draft:${exercise.id}",exercise.title,source);selected=e.id;repository.setSetting("python_exercise",e.id);source=store.projects().firstOrNull {it.id=="draft:${e.id}"}?.source?:e.starter;hints=0;result=null;expanded=false}) {Text(e.title)}}}}
      Text("${exercise.lessonId} · ${exercise.objective}",style=AppTypography.body2,color=Theme.colors.textSecondary)
      SecondaryButton("Open curriculum lesson",{navigation.navigateToLearningHubLesson(exercise.lessonId)},enabled=!running)
      Text(exercise.statement,style=AppTypography.body1,color=Theme.colors.textPrimary)
      Text("Limits: 10 CPU seconds / 30 elapsed seconds · 512 MiB · 64 KiB output. Files exist only for this run. Python, NumPy and Pandas are included.",style=AppTypography.caption,color=Theme.colors.textSecondary)
      Text("Example / public test",style=AppTypography.body2,fontWeight=FontWeight.Bold,color=Theme.colors.textPrimary)
      SelectionContainer {Text(exercise.tests.first().code,style=CodeStyle,color=Theme.colors.textPrimary)}
      SecondaryButton("Hint (${hints.coerceAtMost(exercise.hints.size)}/${exercise.hints.size})",{hints=(hints+1).coerceAtMost(exercise.hints.size)})
      exercise.hints.take(hints).forEach {Text(it,style=AppTypography.body2,color=Theme.colors.textSecondary)}
      Text("Validation: ${exercise.tests.size} tests",style=AppTypography.body2,color=Theme.colors.textPrimary)
      Text("Mastery ${summary.mastery}/100 · ${summary.correct}/${summary.attempts} recent attempts passed",style=AppTypography.caption,color=Theme.colors.textSecondary)
     };DividerHandle("Resize instructions") {leftWidth=(leftWidth+it).coerceIn(200f,400f);repository.setSetting("python_left_width",leftWidth.toString())}}
    Column(Modifier.weight(1f).fillMaxHeight()) {
     Text("main.py   ·   Ctrl+Enter Run   ·   Shift+Esc Stop   ·   Ctrl+S Save",Modifier.padding(12.dp),style=AppTypography.caption,color=Theme.colors.textSecondary)
     PythonEditor(source,{if(it.length<=200000)source=it},Theme.colors.isDark,{execute(false)},{PythonRunner.stop()},::save,Modifier.weight(1f).fillMaxWidth())
     OutlinedTextField(stdin,{stdin=it;repository.setSetting("python_stdin",it)},label={Text("Standard input (one input per line)")},modifier=Modifier.fillMaxWidth().height(90.dp),textStyle=CodeStyle)
    }
    if(right) {DividerHandle("Resize output") {rightWidth=(rightWidth-it).coerceIn(220f,450f);repository.setSetting("python_right_width",rightWidth.toString())}
     LabPanel("Console & tests",Modifier.width((if(narrow) min(rightWidth,240f) else rightWidth).dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
      SecondaryButton("Clear output",{result=null},enabled=!running)
      if(running) {LinearProgressIndicator(Modifier.fillMaxWidth());Text("Executing in isolated Python…",color=Theme.colors.textPrimary)}
      result?.let {r->
       Text(r.status.replace('_',' '),style=AppTypography.h3,color=Theme.colors.textPrimary)
       SelectionContainer {Text(r.output.ifEmpty {"(No standard output)"},style=CodeStyle,color=Theme.colors.textPrimary)}
       if(r.error.isNotEmpty()) SelectionContainer {Text(r.error,style=CodeStyle,color=Theme.colors.textPrimary)}
       r.tests.forEach {t->Text("${if(t.passed) "✓ Passed" else "× Failed"} · ${t.name}${if(t.message.isNotEmpty()) "\n${t.message}" else ""}",style=AppTypography.body2,color=Theme.colors.textPrimary)}
       if(r.tests.isNotEmpty() && r.tests.all {it.passed} && r.status=="success") Text(exercise.explanation,style=AppTypography.body1,color=Theme.colors.textPrimary)
      }
     }
    }
   }
  }
 }
 if(showProjects) AlertDialog(onDismissRequest={showProjects=false},title={Text("Local Python projects")},text={Column(Modifier.heightIn(max=400.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)) {OutlinedTextField(projectName,{projectName=it.take(100)},label={Text("Project name")});store.projects().filter {it.id.startsWith("project:")}.forEach {p->SecondaryButton("Open ${p.name}",{source=p.source;projectName=p.name;showProjects=false})}}},confirmButton={PrimaryButton("Save project",{save();showProjects=false})},dismissButton={SecondaryButton("Close",{showProjects=false})})
}

@Composable fun MathLabScreen(repository:ProgressRepository) {
 var topic by remember {mutableStateOf(runCatching {MathTopic.valueOf(repository.getSetting("math_topic","Algebra"))}.getOrDefault(MathTopic.Algebra))}
 var seed by remember {mutableStateOf(repository.getSetting("math_seed","1").toIntOrNull()?:1)}
 var difficulty by remember {mutableStateOf(repository.getSetting("math_difficulty","1").toIntOrNull()?.coerceIn(1,3)?:1)}
 var mixed by remember {mutableStateOf(false)};var tool by remember {mutableStateOf("Practice")}
 var reasoning by remember {mutableStateOf(false)}
 val problem=remember(topic,difficulty,seed,reasoning) {MathEngine.generate(topic,difficulty,seed).let {if(reasoning)it.copy(id=it.id+":reasoning",statement="Explain and justify the method for this problem:\n"+it.statement,type=MathAnswerType.Explanation)else it}}
 var answer by remember(problem.id) {mutableStateOf(repository.getSetting("math_answer:${problem.id}",""))};var hints by remember(problem.id) {mutableStateOf(0)}
 var verdict by remember(problem.id) {mutableStateOf<MathVerdict?>(null)};var start by remember(problem.id) {mutableStateOf(System.currentTimeMillis())}
 var reviewSaved by remember(problem.id) {mutableStateOf("")}
 var summary by remember(topic) {mutableStateOf(repository.labs.summary("math",topic.name))}
 Column(Modifier.fillMaxSize().background(Theme.colors.appBackground).verticalScroll(rememberScrollState()).padding(24.dp),verticalArrangement=Arrangement.spacedBy(18.dp)) {
  Text("Math Lab",style=DisplayStyle,color=Theme.colors.textPrimary)
  Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Practice","Graph","Matrix / vector","Statistics","Probability").forEach {name->if(tool==name)PrimaryButton(name,{tool=name}) else SecondaryButton(name,{tool=name})}}
  if(tool!="Practice") MathCalculator(tool) else {
   Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.horizontalScroll(rememberScrollState())) {
    var select by remember {mutableStateOf(false)}
    Box {SecondaryButton(topic.title,{select=true});DropdownMenu(select,{select=false}) {MathTopic.values().forEach {t->DropdownMenuItem(onClick={topic=t;repository.setSetting("math_topic",t.name);select=false}) {Text(t.title)}}}}
    for(d in 1..3) if(difficulty==d)PrimaryButton("Level $d",{difficulty=d}) else SecondaryButton("Level $d",{difficulty=d;repository.setSetting("math_difficulty",d.toString())})
    SecondaryButton(if(mixed) "Mixed review ✓" else "Mixed review",{mixed=!mixed})
    SecondaryButton(if(reasoning) "Reasoning self-review ✓" else "Reasoning self-review",{reasoning=!reasoning})
   }
   Text("${topic.title} / Problem $seed · Level $difficulty",style=AppTypography.caption,color=Theme.colors.textSecondary)
   Text(problem.statement,style=AppTypography.h2,color=Theme.colors.textPrimary,modifier=Modifier.widthIn(max=900.dp))
   FormulaBlock(problem.formula)
   problem.graph?.let {GraphPlot(it)}
   Text(when(problem.type) {MathAnswerType.Number->"Accepts integers, decimals, fractions and arithmetic. Tolerance: 0.000001 × max(1, |answer|).";MathAnswerType.Expression->"Enter an expanded polynomial in x. Spaces, term order, * and ² are normalized.";MathAnswerType.Matrix->"Enter comma-separated cells and semicolon-separated rows: 1,2;3,4";MathAnswerType.Vector->"Enter comma-separated components: 1,2";MathAnswerType.Explanation->"Submit your reasoning before revealing the review rubric. Self-assessments are stored separately from automatically verified mastery.";else->"Choose one answer."},style=AppTypography.caption,color=Theme.colors.textSecondary)
   if(problem.type==MathAnswerType.Choice) problem.choices.forEach {choice->
    OutlinedButton(onClick={if(verdict==null)answer=choice},enabled=verdict==null,modifier=Modifier.fillMaxWidth(),border=BorderStroke(if(answer==choice)2.dp else 1.dp,Theme.colors.borderStrong)) {Text((if(answer==choice)"● " else "○ ")+choice,color=Theme.colors.textPrimary)}
   } else OutlinedTextField(answer,{answer=it.take(256);repository.setSetting("math_answer:${problem.id}",answer)},label={Text("Your answer")},enabled=verdict==null,modifier=Modifier.fillMaxWidth().widthIn(max=800.dp),textStyle=AppTypography.body1)
   Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
    SecondaryButton("Hint",{hints=(hints+1).coerceAtMost(problem.steps.size)},enabled=verdict==null)
    PrimaryButton(if(reasoning) "Submit reasoning" else "Check answer",{val result=MathEngine.check(problem,answer);verdict=result;if(!reasoning){repository.labs.record("math",topic.name,problem.id,answer,result.message,result.correct,hints,System.currentTimeMillis()-start);summary=repository.labs.summary("math",topic.name)}},enabled=answer.trim().length>=(if(reasoning)10 else 1) && verdict==null)
    SecondaryButton("Retry",{verdict=null;answer="";reviewSaved="";start=System.currentTimeMillis()})
    SecondaryButton("Next problem",{seed++;repository.setSetting("math_seed",seed.toString());if(mixed){topic=MathTopic.values()[seed%MathTopic.values().size];repository.setSetting("math_topic",topic.name)}})
   }
   if(verdict==null) problem.steps.take(hints).forEach {Text("Hint: $it",color=Theme.colors.textPrimary)}
   verdict?.let {v->LabPanel(if(reasoning) "Reasoning review" else if(v.correct) "✓ Correct" else "× Review your answer",Modifier.fillMaxWidth().border(1.dp,Theme.colors.borderStrong,RoundedCornerShape(16.dp))) {
    Text(v.message,color=Theme.colors.textPrimary);problem.steps.forEachIndexed {i,step->Text("${i+1}. $step",style=AppTypography.body1,color=Theme.colors.textPrimary)}
    if(reasoning) {
     Text("Rubric: name the governing principle, show each transformation, and justify the result and its assumptions.",color=Theme.colors.textPrimary)
     Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Needs Review","Mastered").forEach {choice->SecondaryButton(choice,{repository.labs.record("math-self-review",topic.name,problem.id,answer,"Self-assessed: $choice",choice=="Mastered",hints,System.currentTimeMillis()-start);reviewSaved="Saved locally: $choice (self-assessed)"},enabled=reviewSaved.isEmpty())}}
     if(reviewSaved.isNotEmpty())Text(reviewSaved,color=Theme.colors.textSecondary)
    }
   }}
   Text("Topic mastery ${summary.mastery}/100 · ${summary.correct} correct / ${summary.attempts} recent attempts",style=AppTypography.body2,color=Theme.colors.textSecondary)
   LinearProgressIndicator(summary.mastery/100f,Modifier.fillMaxWidth(),color=Theme.colors.brandPrimary,backgroundColor=Theme.colors.surfaceTertiary)
  }
 }
}

@Composable private fun FormulaBlock(text:String) {Surface(color=Theme.colors.surfaceSecondary,shape=RoundedCornerShape(14.dp),modifier=Modifier.fillMaxWidth()) {SelectionContainer {Text(text,Modifier.padding(20.dp),style=AppTypography.h3,color=Theme.colors.textPrimary)}}}
@Composable private fun GraphPlot(expression:String) {
 val points=remember(expression) {(-100..100).mapNotNull {i->val x=i/10.0;runCatching {x to ExpressionParser(expression,x).value()}.getOrNull()} }
 val line=Theme.colors.textPrimary;val grid=Theme.colors.borderDefault
 Canvas(Modifier.fillMaxWidth().height(230.dp).background(Theme.colors.surfaceSecondary).semantics {contentDescription="Graph of $expression. x and y range from minus ten to ten."}) {
  fun position(x:Double,y:Double)=Offset(((x+10)/20*size.width).toFloat(),((10-y)/20*size.height).toFloat())
  for(i in -10..10) {drawLine(grid,position(i.toDouble(),-10.0),position(i.toDouble(),10.0));drawLine(grid,position(-10.0,i.toDouble()),position(10.0,i.toDouble()))}
  drawLine(line,position(0.0,-10.0),position(0.0,10.0),2f);drawLine(line,position(-10.0,0.0),position(10.0,0.0),2f)
  points.zipWithNext().forEach {(a,b)->if(a.second in -10.0..10.0 && b.second in -10.0..10.0)drawLine(line,position(a.first,a.second),position(b.first,b.second),2f)}
 }
 Text("x: −10 … 10  ·  y: −10 … 10",style=AppTypography.caption,color=Theme.colors.textSecondary)
 Text((-2..2).joinToString("  ·  ") {x->"f($x) = "+runCatching {ExpressionParser(expression,x.toDouble()).value().toString()}.getOrDefault("undefined")},style=AppTypography.caption,color=Theme.colors.textSecondary)
}
@Composable private fun MathCalculator(tool:String) {
 var a by remember(tool) {mutableStateOf(when(tool){"Graph"->"x^2";"Matrix / vector"->"1,2;3,4";"Statistics"->"1,2,3,4";else->"10"})};var b by remember(tool) {mutableStateOf(if(tool=="Matrix / vector") "1,0;0,1" else "3")};var probability by remember {mutableStateOf("0.5")};var result by remember(tool) {mutableStateOf("")};var plotted by remember {mutableStateOf("x^2")}
 Text(tool,style=AppTypography.h2,color=Theme.colors.textPrimary)
 var operation by remember {mutableStateOf("Matrix product")}
 if(tool=="Matrix / vector")Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {listOf("Matrix product","Dot product","Vector norm").forEach {name->SecondaryButton((if(operation==name)"✓ " else "")+name,{operation=name})}}
 OutlinedTextField(a,{a=it.take(256)},label={Text(when(tool){"Graph"->"f(x), e.g. sin(x), x^2 or exp(x)";"Statistics"->"Values separated by commas";"Matrix / vector"->"Matrix A (or one-row vector)";else->"Trials n (0–100)"})},modifier=Modifier.fillMaxWidth())
 if(tool in listOf("Matrix / vector","Probability")) OutlinedTextField(b,{b=it.take(256)},label={Text(if(tool=="Probability") "Successes k" else "Matrix B")},modifier=Modifier.fillMaxWidth())
 if(tool=="Probability") OutlinedTextField(probability,{probability=it},label={Text("Probability p (0–1)")})
 PrimaryButton(if(tool=="Graph") "Plot" else "Calculate",{result=runCatching {when(tool) {
  "Graph"->{ExpressionParser(a,1.0).value();plotted=a;"Graph updated."}
  "Statistics"->MathEngine.statistics(a.split(',').map {ExpressionParser(it).value()})
  "Matrix / vector"->when(operation){"Dot product"->"a · b = ${MathEngine.dot(MathEngine.vector(a),MathEngine.vector(b))}";"Vector norm"->"‖a‖ = ${MathEngine.norm(MathEngine.vector(a))}";else->MathEngine.multiply(MathEngine.matrix(a),MathEngine.matrix(b)).joinToString("\n") {it.joinToString("   ","[ "," ]")}}
  else->"P(X = $b) = ${MathEngine.binomial(a.toInt(),b.toInt(),ExpressionParser(probability).value())}"
 }}.getOrElse {"Input error: ${it.message}"}})
 if(tool=="Graph") GraphPlot(plotted)
 if(result.isNotEmpty()) SelectionContainer {Text(result,style=CodeStyle,color=Theme.colors.textPrimary)}
}
