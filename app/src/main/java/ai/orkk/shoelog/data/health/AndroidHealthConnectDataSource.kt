package ai.orkk.shoelog.data.health

import android.content.Context
import android.content.pm.PackageManager
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import kotlin.math.roundToLong

class AndroidHealthConnectDataSource(
    private val context: Context,
) : HealthConnectDataSource {
    private val client: HealthConnectClient by lazy { HealthConnectClient.getOrCreate(context) }

    override val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    override fun availability(): HealthConnectAvailability = when (
        HealthConnectClient.getSdkStatus(context, HEALTH_CONNECT_PACKAGE)
    ) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
        else -> HealthConnectAvailability.UNSUPPORTED
    }

    override suspend fun grantedPermissions(): Set<String> =
        if (availability() == HealthConnectAvailability.AVAILABLE) {
            client.permissionController.getGrantedPermissions()
        } else {
            emptySet()
        }

    override fun historyFeatureAvailable(): Boolean =
        availability() == HealthConnectAvailability.AVAILABLE &&
            client.features.getFeatureStatus(HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_HISTORY) ==
            HealthConnectFeatures.FEATURE_STATUS_AVAILABLE

    override suspend fun readRunningExercises(start: Instant, end: Instant): List<HealthExercise> {
        val sessions = mutableListOf<ExerciseSessionRecord>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = ExerciseSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end),
                    pageToken = pageToken,
                ),
            )
            sessions += response.records.filter { it.exerciseType in RUNNING_TYPES }
            pageToken = response.pageToken
        } while (pageToken != null)

        return sessions.map { session ->
            val distance = client.aggregate(
                AggregateRequest(
                    metrics = setOf(DistanceRecord.DISTANCE_TOTAL),
                    timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime),
                    dataOriginFilter = setOf(session.metadata.dataOrigin),
                ),
            )[DistanceRecord.DISTANCE_TOTAL]?.inMeters?.roundToLong()?.takeIf { it > 0 }
            val sourcePackage = session.metadata.dataOrigin.packageName
            HealthExercise(
                id = session.metadata.id,
                startTime = session.startTime,
                endTime = session.endTime,
                distanceMeters = distance,
                exerciseType = session.exerciseType,
                sourcePackage = sourcePackage,
                sourceName = appLabel(sourcePackage),
                sourceUpdatedAt = session.metadata.lastModifiedTime,
            )
        }
    }

    private fun appLabel(packageName: String): String = try {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    } catch (_: PackageManager.NameNotFoundException) {
        packageName
    }

    private companion object {
        const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
        val RUNNING_TYPES = setOf(
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING,
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL,
        )
    }
}
