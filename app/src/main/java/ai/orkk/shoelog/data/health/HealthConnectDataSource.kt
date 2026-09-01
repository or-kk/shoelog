package ai.orkk.shoelog.data.health

import java.time.Instant

enum class HealthConnectAvailability {
    AVAILABLE,
    UPDATE_REQUIRED,
    UNSUPPORTED,
}

data class HealthExercise(
    val id: String,
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Long?,
    val exerciseType: Int,
    val sourcePackage: String,
    val sourceName: String,
    val sourceUpdatedAt: Instant?,
)

interface HealthConnectDataSource {
    val requiredPermissions: Set<String>

    fun availability(): HealthConnectAvailability

    suspend fun grantedPermissions(): Set<String>

    fun historyFeatureAvailable(): Boolean

    suspend fun readRunningExercises(start: Instant, end: Instant): List<HealthExercise>
}
