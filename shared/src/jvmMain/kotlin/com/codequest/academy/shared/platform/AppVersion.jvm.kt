package com.codequest.academy.shared.platform

actual fun applicationVersion(): String = System.getProperty("codequest.version", "1.2.2")
