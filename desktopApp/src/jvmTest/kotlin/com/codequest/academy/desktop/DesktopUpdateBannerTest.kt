package com.codequest.academy.desktop

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopUpdateBannerTest {
    @Test
    fun `update action accepts verified production and local development URLs`() {
        assertTrue(isSupportedUpdateUrl("https://nous-ai-academy.vercel.app/api/download?os=windows"))
        assertTrue(isSupportedUpdateUrl("http://localhost:3000/api/download?os=windows"))
    }

    @Test
    fun `update action rejects unsafe or malformed URLs`() {
        assertFalse(isSupportedUpdateUrl("file:///C:/Windows/System32/cmd.exe"))
        assertFalse(isSupportedUpdateUrl("javascript:alert(1)"))
        assertFalse(isSupportedUpdateUrl("not a url"))
    }
}
