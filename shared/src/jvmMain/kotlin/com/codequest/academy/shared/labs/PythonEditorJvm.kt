package com.codequest.academy.shared.labs

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Token
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.Color
import java.awt.Font
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.KeyStroke
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

@Composable actual fun PythonEditor(source:String,onChange:(String)->Unit,dark:Boolean,onRun:()->Unit,onStop:()->Unit,onSave:()->Unit,modifier:Modifier) {
 val change by rememberUpdatedState(onChange); val run by rememberUpdatedState(onRun); val stop by rememberUpdatedState(onStop); val save by rememberUpdatedState(onSave)
 SwingPanel(modifier=modifier,factory={
  val editor=RSyntaxTextArea().apply {
   text=source; syntaxEditingStyle=SyntaxConstants.SYNTAX_STYLE_PYTHON; isCodeFoldingEnabled=true; isAutoIndentEnabled=true; tabSize=4; tabsEmulated=true
   isBracketMatchingEnabled=true; setAnimateBracketMatching(false)
   accessibleContext.accessibleName="Python code editor"
   val fontStream=javaClass.getResourceAsStream("/font/jetbrains_mono_variable.ttf")
   font=fontStream?.use {Font.createFont(Font.TRUETYPE_FONT,it).deriveFont(14f)}?:Font(Font.MONOSPACED,Font.PLAIN,14)
   document.addDocumentListener(object:DocumentListener {override fun insertUpdate(e:DocumentEvent){change(text)};override fun removeUpdate(e:DocumentEvent){change(text)};override fun changedUpdate(e:DocumentEvent){} })
   fun shortcut(key:String,name:String,action:()->Unit) {inputMap.put(KeyStroke.getKeyStroke(key),name);actionMap.put(name,object:AbstractAction(){override fun actionPerformed(e:ActionEvent){action()}})}
   shortcut("control ENTER","nous-run") {run()};shortcut("shift ESCAPE","nous-stop") {stop()};shortcut("control S","nous-save") {save()}
  }; RTextScrollPane(editor).apply {lineNumbersEnabled=true;border=javax.swing.BorderFactory.createEmptyBorder()}
 },update={ pane ->
  val editor=pane.textArea as RSyntaxTextArea
  if(editor.text!=source) {val position=editor.caretPosition;editor.text=source;editor.caretPosition=position.coerceAtMost(source.length)}
  val bg=if(dark) Color(22,22,22) else Color.WHITE;val fg=if(dark) Color(230,230,230) else Color(36,36,36);val muted=if(dark) Color(174,174,174) else Color(101,101,101)
  editor.background=bg;editor.foreground=fg;editor.caretColor=fg;editor.currentLineHighlightColor=if(dark) Color(32,32,32) else Color(247,247,247)
  editor.selectionColor=if(dark) Color(75,75,75) else Color(210,210,210);editor.selectedTextColor=fg;editor.matchedBracketBGColor=if(dark) Color(48,48,48) else Color(226,226,226);editor.matchedBracketBorderColor=fg
  val scheme=editor.syntaxScheme
  for(i in 0 until scheme.styleCount) scheme.getStyle(i)?.let {it.foreground=fg;it.background=null;it.font=editor.font}
  listOf(Token.COMMENT_EOL,Token.COMMENT_MULTILINE,Token.LITERAL_STRING_DOUBLE_QUOTE).forEach {scheme.getStyle(it).foreground=muted}
  listOf(Token.RESERVED_WORD,Token.RESERVED_WORD_2,Token.FUNCTION).forEach {scheme.getStyle(it).font=editor.font.deriveFont(Font.BOLD)}
  pane.gutter.background=bg;pane.gutter.lineNumberColor=muted;pane.gutter.borderColor=muted;pane.gutter.foldIndicatorForeground=fg
  editor.repaint()
 })
}
