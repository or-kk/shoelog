package ai.orkk.shoelog.data.repository

import ai.orkk.shoelog.data.local.ExerciseEntity
import ai.orkk.shoelog.data.local.ShoeEntity
import ai.orkk.shoelog.data.local.ShoeLogDao
import java.time.Clock
import java.time.Duration

interface SampleDataStore {
    suspend fun clear()
    suspend fun insertShoe(shoe: ShoeEntity)
    suspend fun upsertExercise(exercise: ExerciseEntity)
    suspend fun assign(exerciseId: String, shoeId: Long)
}

class RoomSampleDataStore(private val dao: ShoeLogDao) : SampleDataStore {
    override suspend fun clear() {
        dao.clearSampleExercises()
        dao.clearSampleShoes()
    }

    override suspend fun insertShoe(shoe: ShoeEntity) {
        dao.insertShoe(shoe)
    }

    override suspend fun upsertExercise(exercise: ExerciseEntity) {
        dao.upsertExercises(listOf(exercise))
    }

    override suspend fun assign(exerciseId: String, shoeId: Long) {
        dao.assignShoe(exerciseId, shoeId, automatic = false, assignedAtEpochMillis = exerciseTime)
    }

    private val exerciseTime: Long
        get() = System.currentTimeMillis()
}

class SampleDataRepository(
    private val store: SampleDataStore,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun install() {
        store.clear()
        val now = clock.instant()
        shoes(now.toEpochMilli()).forEach { store.insertShoe(it) }
        val exercises = exercises(now.toEpochMilli())
        exercises.forEach { store.upsertExercise(it) }
        store.assign("sample:run-01", SAMPLE_SHOE_DAILY)
        store.assign("sample:run-02", SAMPLE_SHOE_NEAR_TARGET)
        store.assign("sample:run-03", SAMPLE_SHOE_DAILY)
    }

    suspend fun clear() = store.clear()

    private fun shoes(now: Long): List<ShoeEntity> = listOf(
        sampleShoe(
            id = SAMPLE_SHOE_DAILY,
            brand = "Nike",
            model = "Velocity Sample",
            nickname = "데일리 러너",
            color = "검정 · 라임",
            initialMeters = 50_000,
            targetMeters = 500_000,
            defaultShoe = true,
            now = now,
        ),
        sampleShoe(
            id = SAMPLE_SHOE_NEAR_TARGET,
            brand = "ASICS",
            model = "Cloud Sample",
            nickname = "장거리 훈련화",
            color = "회색",
            initialMeters = 430_000,
            targetMeters = 500_000,
            now = now,
        ),
        sampleShoe(
            id = SAMPLE_SHOE_SPEED,
            brand = "HOKA",
            model = "Tempo Sample",
            nickname = "스피드화",
            color = "흰색 · 주황",
            initialMeters = 12_000,
            targetMeters = 400_000,
            now = now,
        ),
        sampleShoe(
            id = SAMPLE_SHOE_RETIRED,
            brand = "New Balance",
            model = "Archive Sample",
            nickname = "첫 러닝화",
            color = "파랑",
            initialMeters = 610_000,
            targetMeters = 600_000,
            retired = true,
            now = now,
        ),
    )

    private fun exercises(now: Long): List<ExerciseEntity> = listOf(
        sampleExercise("sample:run-01", now - Duration.ofDays(1).toMillis(), 10_200),
        sampleExercise("sample:run-02", now - Duration.ofDays(3).toMillis(), 25_400),
        sampleExercise("sample:run-03", now - Duration.ofDays(5).toMillis(), 5_100),
        sampleExercise("sample:run-04", now - Duration.ofDays(7).toMillis(), 12_300),
        sampleExercise("sample:run-05", now - Duration.ofDays(9).toMillis(), null),
    )

    private fun sampleShoe(
        id: Long,
        brand: String,
        model: String,
        nickname: String,
        color: String,
        initialMeters: Long,
        targetMeters: Long,
        retired: Boolean = false,
        defaultShoe: Boolean = false,
        now: Long,
    ) = ShoeEntity(
        id = id,
        brand = brand,
        model = model,
        nickname = nickname,
        color = color,
        purchaseDateEpochDay = null,
        startDateEpochDay = null,
        initialMeters = initialMeters,
        targetMeters = targetMeters,
        photoUri = null,
        retired = retired,
        defaultShoe = defaultShoe,
        createdAtEpochMillis = now,
        updatedAtEpochMillis = now,
    )

    private fun sampleExercise(id: String, start: Long, distance: Long?) = ExerciseEntity(
        id = id,
        fallbackKey = "fallback:$id",
        startEpochMillis = start,
        endEpochMillis = start + Duration.ofMinutes(42).toMillis(),
        distanceMeters = distance,
        exerciseType = SAMPLE_RUNNING_TYPE,
        sourcePackage = "ai.orkk.shoelog.sample",
        sourceName = "ShoeLog 샘플",
        sourceUpdatedAtEpochMillis = null,
        syncedAtEpochMillis = clock.instant().toEpochMilli(),
        sourceDeleted = false,
    )

    private companion object {
        const val SAMPLE_SHOE_DAILY = -101L
        const val SAMPLE_SHOE_NEAR_TARGET = -102L
        const val SAMPLE_SHOE_SPEED = -103L
        const val SAMPLE_SHOE_RETIRED = -104L
        const val SAMPLE_RUNNING_TYPE = 56
    }
}
