package ai.orkk.shoelog.data.repository

import ai.orkk.shoelog.data.health.FakeHealthConnectDataSource
import ai.orkk.shoelog.data.health.HealthExercise
import ai.orkk.shoelog.data.local.ExerciseEntity
import ai.orkk.shoelog.data.preferences.AppSettings
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncRepositoryTest {
    private val now = Instant.parse("2026-09-01T00:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)

    @Test
    fun syncingSameRecordTwiceDoesNotDuplicateIt() = runTest {
        val health = FakeHealthConnectDataSource(records = listOf(run(id = "hc:1")))
        val store = InMemorySyncStore()
        val repository = SyncRepository(health, store, settings(), clock)

        val first = repository.sync()
        val second = repository.sync()

        assertEquals(1, store.records.size)
        assertEquals(1, (first as SyncResult.Success).newExerciseIds.size)
        assertEquals(0, (second as SyncResult.Success).newExerciseIds.size)
    }

    @Test
    fun changedDistanceUpdatesExistingRecord() = runTest {
        val health = FakeHealthConnectDataSource(records = listOf(run(id = "hc:1", distance = 5_000)))
        val store = InMemorySyncStore()
        val repository = SyncRepository(health, store, settings(), clock)
        repository.sync()

        health.records = listOf(run(id = "hc:1", distance = 8_000))
        repository.sync()

        assertEquals(8_000L, store.records.getValue("hc:1").distanceMeters)
    }

    @Test
    fun missingRecordIsSoftDeletedWithinCompletedWindow() = runTest {
        val health = FakeHealthConnectDataSource(records = listOf(run(id = "hc:1")))
        val store = InMemorySyncStore()
        val repository = SyncRepository(health, store, settings(), clock)
        repository.sync()

        health.records = emptyList()
        repository.sync()

        assertTrue(store.records.getValue("hc:1").sourceDeleted)
    }

    @Test
    fun defaultShoeAutoAssignsOnlyPositiveDistanceRuns() = runTest {
        val health = FakeHealthConnectDataSource(
            records = listOf(
                run(id = "hc:distance", distance = 5_000),
                run(id = "hc:no-distance", distance = null, startOffsetSeconds = 7_200),
            ),
        )
        val store = InMemorySyncStore(defaultShoeId = 7)
        val repository = SyncRepository(
            health,
            store,
            settings(autoAssign = true, defaultShoeId = 7),
            clock,
        )

        val result = repository.sync() as SyncResult.Success

        assertEquals(7L, store.assignments["hc:distance"])
        assertNull(store.assignments["hc:no-distance"])
        assertEquals(setOf("hc:distance"), result.autoAssignedIds)
        assertFalse(store.records.getValue("hc:no-distance").sourceDeleted)
    }

    @Test
    fun healthSyncNeverSoftDeletesSampleRecords() = runTest {
        val health = FakeHealthConnectDataSource(records = emptyList())
        val store = InMemorySyncStore().apply {
            records["sample:run-01"] = ExerciseEntity(
                id = "sample:run-01",
                fallbackKey = "fallback:sample:run-01",
                startEpochMillis = now.minusSeconds(3_600).toEpochMilli(),
                endEpochMillis = now.minusSeconds(1_800).toEpochMilli(),
                distanceMeters = 5_000,
                exerciseType = 56,
                sourcePackage = "ai.orkk.shoelog.sample",
                sourceName = "ShoeLog 샘플",
                sourceUpdatedAtEpochMillis = null,
                syncedAtEpochMillis = now.toEpochMilli(),
                sourceDeleted = false,
            )
        }

        SyncRepository(health, store, settings(), clock).sync()

        assertFalse(store.records.getValue("sample:run-01").sourceDeleted)
    }

    private fun run(
        id: String,
        distance: Long? = 5_000,
        startOffsetSeconds: Long = 3_600,
    ) = HealthExercise(
        id = id,
        startTime = now.minusSeconds(startOffsetSeconds),
        endTime = now.minusSeconds(startOffsetSeconds - 1_800),
        distanceMeters = distance,
        exerciseType = 56,
        sourcePackage = "com.sec.android.app.shealth",
        sourceName = "Samsung Health",
        sourceUpdatedAt = null,
    )

    private fun settings(
        autoAssign: Boolean = false,
        defaultShoeId: Long? = null,
    ) = StaticSyncSettings(
        AppSettings(
            autoAssignDefault = autoAssign,
            defaultShoeId = defaultShoeId,
        ),
    )
}

private class InMemorySyncStore(
    private val defaultShoeId: Long? = null,
) : ExerciseSyncStore {
    val records = linkedMapOf<String, ExerciseEntity>()
    val assignments = mutableMapOf<String, Long>()

    override suspend fun findById(id: String): ExerciseEntity? = records[id]

    override suspend fun findByFallbackKey(fallbackKey: String): ExerciseEntity? =
        records.values.firstOrNull { it.fallbackKey == fallbackKey }

    override suspend fun upsert(exercise: ExerciseEntity): Boolean {
        val inserted = exercise.id !in records
        records[exercise.id] = exercise
        return inserted
    }

    override suspend fun markMissingDeleted(
        startEpochMillis: Long,
        endEpochMillis: Long,
        presentIds: Set<String>,
        syncedAtEpochMillis: Long,
    ) {
        records.replaceAll { id, record ->
            if (!id.startsWith("sample:") && id !in presentIds && record.startEpochMillis >= startEpochMillis && record.endEpochMillis <= endEpochMillis) {
                record.copy(sourceDeleted = true, syncedAtEpochMillis = syncedAtEpochMillis)
            } else record
        }
    }

    override suspend fun activeDefaultShoeId(): Long? = defaultShoeId

    override suspend fun assign(exerciseId: String, shoeId: Long, automatic: Boolean, assignedAtEpochMillis: Long) {
        assignments[exerciseId] = shoeId
    }
}
