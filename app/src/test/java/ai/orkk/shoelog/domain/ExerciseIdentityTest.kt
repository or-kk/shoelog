package ai.orkk.shoelog.domain

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExerciseIdentityTest {
    private val start = Instant.parse("2026-08-30T21:00:00Z")
    private val end = Instant.parse("2026-08-30T21:42:00Z")

    @Test
    fun sameExerciseCreatesStableFallbackKey() {
        val first = ExerciseIdentity.fallbackKey("com.sec.android.app.shealth", start, end, 56)
        val second = ExerciseIdentity.fallbackKey("com.sec.android.app.shealth", start, end, 56)

        assertEquals(first, second)
        assertEquals(64, first.length)
    }

    @Test
    fun changedStartTimeCreatesDifferentFallbackKey() {
        val first = ExerciseIdentity.fallbackKey("com.sec.android.app.shealth", start, end, 56)
        val changed = ExerciseIdentity.fallbackKey(
            "com.sec.android.app.shealth",
            start.plusSeconds(1),
            end,
            56,
        )

        assertNotEquals(first, changed)
    }
}
