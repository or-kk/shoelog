package ai.orkk.shoelog.notification

import ai.orkk.shoelog.domain.Exercise
import java.time.Instant
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationPolicyTest {
    @Test
    fun noNewRunsDoesNotNotify() {
        assertFalse(NotificationPolicy.shouldNotify(emptyList(), emptySet()))
    }

    @Test
    fun fullyAutoAssignedRunsDoNotNotify() {
        val exercise = exercise("run:1")

        assertFalse(NotificationPolicy.shouldNotify(listOf(exercise), setOf("run:1")))
    }

    @Test
    fun atLeastOneUnassignedNewRunNotifies() {
        val exercises = listOf(exercise("run:1"), exercise("run:2"))

        assertTrue(NotificationPolicy.shouldNotify(exercises, setOf("run:1")))
    }

    private fun exercise(id: String) = Exercise(
        id = id,
        fallbackKey = "fallback:$id",
        startTime = Instant.parse("2026-08-31T00:00:00Z"),
        endTime = Instant.parse("2026-08-31T00:30:00Z"),
        distanceMeters = 5_000,
        exerciseType = 56,
        sourcePackage = "ai.orkk.shoelog.sample",
        sourceName = "ShoeLog 샘플",
        sourceUpdatedAt = null,
        syncedAt = Instant.parse("2026-09-01T00:00:00Z"),
    )
}
