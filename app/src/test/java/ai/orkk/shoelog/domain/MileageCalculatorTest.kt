package ai.orkk.shoelog.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class MileageCalculatorTest {
    @Test
    fun includesInitialDistanceAndAssignedRuns() {
        val result = MileageCalculator.calculate(
            initialMeters = 25_000,
            assignedMeters = listOf(5_000L, 10_000L),
            targetMeters = 100_000,
        )

        assertEquals(40_000, result.totalMeters)
        assertEquals(60_000, result.remainingMeters)
        assertEquals(0.4f, result.progress)
    }

    @Test
    fun ignoresMissingZeroAndNegativeDistance() {
        val result = MileageCalculator.calculate(
            initialMeters = 1_000,
            assignedMeters = listOf(null, 0, -5, 2_000),
            targetMeters = 10_000,
        )

        assertEquals(3_000, result.totalMeters)
    }

    @Test
    fun distinguishesNearAndReachedTargets() {
        assertEquals(
            MileageStatus.NEAR_TARGET,
            MileageCalculator.calculate(90, emptyList(), 100).status,
        )
        assertEquals(
            MileageStatus.TARGET_REACHED,
            MileageCalculator.calculate(101, emptyList(), 100).status,
        )
    }

    @Test
    fun nonPositiveTargetHasSafeProgress() {
        val result = MileageCalculator.calculate(1_000, emptyList(), 0)

        assertEquals(0f, result.progress)
        assertEquals(0, result.remainingMeters)
        assertEquals(MileageStatus.NORMAL, result.status)
    }
}
