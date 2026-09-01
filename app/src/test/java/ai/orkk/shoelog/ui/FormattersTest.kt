package ai.orkk.shoelog.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormattersTest {
    @Test
    fun formatsMetersAsOneDecimalKilometers() {
        assertEquals("12.3 km", formatDistance(12_340))
    }

    @Test
    fun formatsMissingDistanceAsKoreanMessage() {
        assertEquals("거리 정보 없음", formatDistance(null))
    }
}
