package com.ojhdtapp.parabox.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilAudioFormatTest {

    @Test
    fun formatAudioTimeMsHandlesSecondsMinutesAndHours() {
        assertEquals("0:00", 0.formatAudioTimeMs())
        assertEquals("0:00", (-500).formatAudioTimeMs())
        assertEquals("0:01", 600.formatAudioTimeMs())
        assertEquals("0:05", 5_000.formatAudioTimeMs())
        assertEquals("0:59", 59_000.formatAudioTimeMs())
        assertEquals("1:00", 60_000.formatAudioTimeMs())
        assertEquals("1:05", 65_200.formatAudioTimeMs())
        assertEquals("1:02:03", 3_723_000.formatAudioTimeMs())
    }
}
