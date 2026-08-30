package com.codequest.academy.shared.platform

actual fun applicationVersion(): String = System.getProperty("nous.version", "1.5.0")
