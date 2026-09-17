package com.cato.resourcecalc.updates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppVersionTest {
    @Test
    fun parsesAndOrdersReleaseVersions() {
        val alpha = AppVersion.parse("v0.1.0-alpha.1")!!
        val alphaTwo = AppVersion.parse("0.1.0-alpha.2")!!
        val stable = AppVersion.parse("0.1.0")!!
        assertTrue(alpha < alphaTwo)
        assertTrue(alphaTwo < stable)
        assertEquals("0.1.0-alpha.1", alpha.toString())
    }

    @Test
    fun rejectsNonVersionTags() {
        assertNull(AppVersion.parse("latest"))
        assertNull(AppVersion.parse("release-2026"))
    }
}

