package ai.orkk.shoelog.ui.exercises

import ai.orkk.shoelog.domain.Exercise
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExerciseViewModelTest {
    @Test
    fun unassignedFilterKeepsDistanceLessRun() {
        val exercises = listOf(
            exercise("assigned", distance = 5_000, shoeId = 1),
            exercise("unassigned", distance = 8_000),
            exercise("no-distance", distance = null),
        )

        val result = filterExercises(exercises, unassignedOnly = true)

        assertEquals(listOf("unassigned", "no-distance"), result.map { it.id })
        assertNull(result.last().distanceMeters)
    }

    @Test
    fun allFilterKeepsEveryVisibleRun() {
        val exercises = listOf(exercise("one", 5_000), exercise("two", null, 2))

        assertEquals(exercises, filterExercises(exercises, unassignedOnly = false))
    }

    private fun exercise(id: String, distance: Long?, shoeId: Long? = null) = Exercise(
        id = id,
        fallbackKey = "fallback:$id",
        startTime = Instant.parse("2026-08-31T00:00:00Z"),
        endTime = Instant.parse("2026-08-31T00:30:00Z"),
        distanceMeters = distance,
        exerciseType = 56,
        sourcePackage = "ai.orkk.shoelog.sample",
        sourceName = "ShoeLog 샘플",
        sourceUpdatedAt = null,
        syncedAt = Instant.parse("2026-09-01T00:00:00Z"),
        assignedShoeId = shoeId,
    )
}
