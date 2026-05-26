package com.superwall.sdk.network.device

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.Locale

class DeviceHelperAsPaddedTest {
    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
    }

    @After
    fun tearDown() {
        Locale.setDefault(previousLocale)
    }

    @Test
    fun `pads major, minor and patch with ASCII digits under en-US locale`() {
        Locale.setDefault(Locale.US)
        assertEquals("000.000.003", "0.0.3".asPadded())
        assertEquals("001.002.003", "1.2.3".asPadded())
        assertEquals("012.034.056", "12.34.56".asPadded())
    }

    @Test
    fun `keeps ASCII digits when default locale renders digits as Arabic-Indic`() {
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        assertEquals("000.000.003", "0.0.3".asPadded())
        assertEquals("001.002.003", "1.2.3".asPadded())
    }

    @Test
    fun `keeps ASCII digits when default locale renders digits as Extended Arabic-Indic`() {
        Locale.setDefault(Locale.forLanguageTag("fa-IR"))
        assertEquals("000.000.003", "0.0.3".asPadded())
    }

    @Test
    fun `keeps ASCII digits when default locale renders digits as Bengali`() {
        Locale.setDefault(Locale.forLanguageTag("bn-BD"))
        assertEquals("000.000.003", "0.0.3".asPadded())
    }

    @Test
    fun `pads beta appendix numerically with ASCII digits under non-Latin locale`() {
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        assertEquals("001.002.003-beta.004", "1.2.3-beta.4".asPadded())
    }

    @Test
    fun `preserves non-numeric appendix without a dot`() {
        Locale.setDefault(Locale.forLanguageTag("ar-EG"))
        assertEquals("001.002.003-rc", "1.2.3-rc".asPadded())
    }
}
