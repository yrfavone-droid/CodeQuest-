package com.codequest.academy.shared.labs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable expect fun PythonEditor(source:String,onChange:(String)->Unit,dark:Boolean,onRun:()->Unit,onStop:()->Unit,onSave:()->Unit,modifier:Modifier)
