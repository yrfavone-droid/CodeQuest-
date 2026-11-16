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
 var selected by remember {mutableStateOf(navigation.selectedPythonExerciseId ?: repository.getSetting("python_exercise","py-types"))}
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
 LaunchedEffect(navigation.selectedPythonExerciseId) {
  val target = navigation.selectedPythonExerciseId
  if (target != null && target != selected) {
   selected = target
   repository.setSetting("python_exercise", target)
   val ex = PythonExercises.all.firstOrNull { it.id == target }
   if (ex != null) {
    source = store.projects().firstOrNull { it.id == "draft:${ex.id}" }?.source ?: ex.starter
    hints = 0
    result = null
   }
  }
 }
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

