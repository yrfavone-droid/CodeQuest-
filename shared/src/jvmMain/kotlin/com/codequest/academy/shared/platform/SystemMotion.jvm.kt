package com.codequest.academy.shared.platform

import com.sun.jna.Native
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary

interface MotionUser32 : StdCallLibrary {
    fun SystemParametersInfoW(action: Int, parameter: Int, value: IntByReference, flags: Int): Boolean
}
private val motionApi by lazy { Native.load("user32", MotionUser32::class.java) }

actual fun systemReducedMotion(): Boolean = runCatching {
    if (!System.getProperty("os.name").startsWith("Windows")) return@runCatching false
    val animation = IntByReference(1)
    motionApi.SystemParametersInfoW(0x1042, 0, animation, 0) && animation.value == 0
}.getOrDefault(true) // Conservative: no animation when the native preference cannot be read.
