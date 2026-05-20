package com.suvojeetsengupta.suvform

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
        // Debug build appends `.debug` suffix per buildTypes config.
        val expected = "com.suvojeetsengupta.suvform"
        val actual = appContext.packageName
        assertEquals(expected, actual.removeSuffix(".debug"))
    }
}
