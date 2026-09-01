package ai.orkk.shoelog.data.repository

import ai.orkk.shoelog.data.health.HealthConnectAvailability
import ai.orkk.shoelog.data.health.HealthConnectDataSource
import ai.orkk.shoelog.data.local.ExerciseEntity
import ai.orkk.shoelog.data.local.ShoeLogDao
import ai.orkk.shoelog.data.preferences.AppSettings
import ai.orkk.shoelog.domain.ExerciseIdentity
import java.time.Clock
import java.time.Duration
import java.time.Instant

sealed interface SyncResult {
    data class Success(
        val newExerciseIds: Set<String>,
        val autoAssignedIds: Set<String>,
        val unassignedNewIds: Set<String>,
        val syncedAt: Instant,
    ) : SyncResult

    data class PermissionRequired(val missingPermissions: Set<String>) : SyncResult
    data object UpdateRequired : SyncResult
    data object Unsupported : SyncResult
    data class Failed(val userMessage: String) : SyncResult
}

interface SyncSettings {
    suspend fun current(): AppSettings
}

fun interface UnassignedRunNotifier {
    fun notifyUnassigned(exerciseIds: Set<String>)
}

class StaticSyncSettings(private val value: AppSettings) : SyncSettings {
    override suspend fun current(): AppSettings = value
}

interface ExerciseSyncStore {
    suspend fun findById(id: String): ExerciseEntity?
    suspend fun findByFallbackKey(fallbackKey: String): ExerciseEntity?
    suspend fun upsert(exercise: ExerciseEntity): Boolean
    suspend fun markMissingDeleted(
        startEpochMillis: Long,
        endEpochMillis: Long,
        presentIds: Set<String>,
        syncedAtEpochMillis: Long,
    )
    suspend fun activeDefaultShoeId(): Long?
    suspend fun assign(exerciseId: String, shoeId: Long, automatic: Boolean, assignedAtEpochMillis: Long)
}

class RoomExerciseSyncStore(private val dao: ShoeLogDao) : ExerciseSyncStore {
    override suspend fun findById(id: String): ExerciseEntity? = dao.exerciseById(id)

    override suspend fun findByFallbackKey(fallbackKey: String): ExerciseEntity? =
        dao.exerciseByFallbackKey(fallbackKey)

    override suspend fun upsert(exercise: ExerciseEntity): Boolean {
        val inserted = dao.insertExercise(exercise) != -1L
        if (!inserted) dao.updateExercise(exercise)
        return inserted
    }

    override suspend fun markMissingDeleted(
        startEpochMillis: Long,
        endEpochMillis: Long,
        presentIds: Set<String>,
        syncedAtEpochMillis: Long,
    ) {
        if (presentIds.isEmpty()) {
            dao.markAllExercisesDeleted(startEpochMillis, endEpochMillis, syncedAtEpochMillis)
        } else {
            dao.markMissingExercisesDeleted(
                startEpochMillis,
                endEpochMillis,
                presentIds.toList(),
                syncedAtEpochMillis,
            )
        }
    }

    override suspend fun activeDefaultShoeId(): Long? = dao.defaultShoe()?.id

    override suspend fun assign(
        exerciseId: String,
        shoeId: Long,
        automatic: Boolean,
        assignedAtEpochMillis: Long,
    ) {
        dao.assignShoe(exerciseId, shoeId, automatic, assignedAtEpochMillis)
    }
}

class SyncRepository(
    private val health: HealthConnectDataSource,
    private val store: ExerciseSyncStore,
    private val settings: SyncSettings,
    private val clock: Clock = Clock.systemUTC(),
    private val notifier: UnassignedRunNotifier = UnassignedRunNotifier { },
) {
    suspend fun sync(now: Instant = clock.instant()): SyncResult {
        return try {
            when (health.availability()) {
                HealthConnectAvailability.UPDATE_REQUIRED -> return SyncResult.UpdateRequired
                HealthConnectAvailability.UNSUPPORTED -> return SyncResult.Unsupported
                HealthConnectAvailability.AVAILABLE -> Unit
            }
            val granted = health.grantedPermissions()
            val missing = health.requiredPermissions - granted
            if (missing.isNotEmpty()) return SyncResult.PermissionRequired(missing)

            val appSettings = settings.current()
            val start = if (appSettings.historyRequested && health.historyFeatureAvailable()) {
                now.minus(Duration.ofDays(3_650))
            } else {
                now.minus(Duration.ofDays(30))
            }
            val healthExercises = health.readRunningExercises(start, now)
            val newIds = linkedSetOf<String>()
            val presentIds = linkedSetOf<String>()
            val newEntities = mutableListOf<ExerciseEntity>()

            for (record in healthExercises) {
                val fallbackKey = ExerciseIdentity.fallbackKey(
                    record.sourcePackage,
                    record.startTime,
                    record.endTime,
                    record.exerciseType,
                )
                val existing = store.findById(record.id) ?: store.findByFallbackKey(fallbackKey)
                val canonicalId = existing?.id ?: record.id.ifBlank { fallbackKey }
                val entity = ExerciseEntity(
                    id = canonicalId,
                    fallbackKey = fallbackKey,
                    startEpochMillis = record.startTime.toEpochMilli(),
                    endEpochMillis = record.endTime.toEpochMilli(),
                    distanceMeters = record.distanceMeters?.takeIf { it > 0 },
                    exerciseType = record.exerciseType,
                    sourcePackage = record.sourcePackage,
                    sourceName = record.sourceName,
                    sourceUpdatedAtEpochMillis = record.sourceUpdatedAt?.toEpochMilli(),
                    syncedAtEpochMillis = now.toEpochMilli(),
                    sourceDeleted = false,
                )
                val inserted = store.upsert(entity)
                presentIds += canonicalId
                if (inserted) {
                    newIds += canonicalId
                    newEntities += entity
                }
            }

            store.markMissingDeleted(start.toEpochMilli(), now.toEpochMilli(), presentIds, now.toEpochMilli())

            val autoAssigned = linkedSetOf<String>()
            val defaultShoeId = appSettings.defaultShoeId ?: store.activeDefaultShoeId()
            if (appSettings.autoAssignDefault && defaultShoeId != null) {
                newEntities.filter { (it.distanceMeters ?: 0) > 0 }.forEach { exercise ->
                    store.assign(exercise.id, defaultShoeId, automatic = true, assignedAtEpochMillis = now.toEpochMilli())
                    autoAssigned += exercise.id
                }
            }
            val result = SyncResult.Success(
                newExerciseIds = newIds,
                autoAssignedIds = autoAssigned,
                unassignedNewIds = newIds - autoAssigned,
                syncedAt = now,
            )
            if (result.unassignedNewIds.isNotEmpty()) notifier.notifyUnassigned(result.unassignedNewIds)
            result
        } catch (_: SecurityException) {
            SyncResult.PermissionRequired(health.requiredPermissions)
        } catch (_: Exception) {
            SyncResult.Failed("동기화하지 못했습니다. 잠시 후 다시 시도해 주세요.")
        }
    }
}
