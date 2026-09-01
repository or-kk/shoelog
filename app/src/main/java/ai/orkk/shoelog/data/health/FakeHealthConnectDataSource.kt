package ai.orkk.shoelog.data.health

import java.time.Instant

class FakeHealthConnectDataSource(
    var records: List<HealthExercise> = emptyList(),
    var state: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    var permissions: Set<String> = setOf(READ_EXERCISE, READ_DISTANCE),
    var historyAvailable: Boolean = true,
) : HealthConnectDataSource {
    override val requiredPermissions: Set<String> = setOf(READ_EXERCISE, READ_DISTANCE)

    override fun availability(): HealthConnectAvailability = state

    override suspend fun grantedPermissions(): Set<String> = permissions

    override fun historyFeatureAvailable(): Boolean = historyAvailable

    override suspend fun readRunningExercises(start: Instant, end: Instant): List<HealthExercise> =
        records.filter { it.startTime >= start && it.endTime <= end }

    private companion object {
        const val READ_EXERCISE = "fake.permission.READ_EXERCISE"
        const val READ_DISTANCE = "fake.permission.READ_DISTANCE"
    }
}
