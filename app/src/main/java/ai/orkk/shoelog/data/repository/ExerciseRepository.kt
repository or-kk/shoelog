package ai.orkk.shoelog.data.repository

import ai.orkk.shoelog.data.local.ExerciseWithShoeRow
import ai.orkk.shoelog.data.local.ShoeLogDao
import ai.orkk.shoelog.domain.Exercise
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ExerciseRepository(
    private val dao: ShoeLogDao,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun observeExercises(unassignedOnly: Boolean = false): Flow<List<Exercise>> =
        dao.observeExercises(unassignedOnly).map { rows -> rows.map(ExerciseWithShoeRow::toDomainExercise) }

    fun observeExercise(exerciseId: String): Flow<Exercise?> =
        dao.observeExercise(exerciseId).map { it?.toDomainExercise() }

    fun observeUnassignedCount(): Flow<Int> = dao.observeUnassignedCount()

    suspend fun assign(exerciseId: String, shoeId: Long?, automatic: Boolean = false) {
        requireNotNull(dao.exerciseById(exerciseId)) { "달리기 기록을 찾을 수 없습니다." }
        if (shoeId != null) requireNotNull(dao.shoeById(shoeId)) { "러닝화를 찾을 수 없습니다." }
        dao.assignShoe(exerciseId, shoeId, automatic, clock.instant().toEpochMilli())
    }
}

internal fun ExerciseWithShoeRow.toDomainExercise(): Exercise {
    val label = if (shoeId == null) null else {
        shoeNickname?.takeIf(String::isNotBlank)
            ?: listOfNotNull(shoeBrand, shoeModel).joinToString(" ").takeIf(String::isNotBlank)
    }
    return Exercise(
        id = exercise.id,
        fallbackKey = exercise.fallbackKey,
        startTime = Instant.ofEpochMilli(exercise.startEpochMillis),
        endTime = Instant.ofEpochMilli(exercise.endEpochMillis),
        distanceMeters = exercise.distanceMeters,
        exerciseType = exercise.exerciseType,
        sourcePackage = exercise.sourcePackage,
        sourceName = exercise.sourceName,
        sourceUpdatedAt = exercise.sourceUpdatedAtEpochMillis?.let(Instant::ofEpochMilli),
        syncedAt = Instant.ofEpochMilli(exercise.syncedAtEpochMillis),
        sourceDeleted = exercise.sourceDeleted,
        assignedShoeId = shoeId,
        assignedShoeLabel = label,
        automaticallyAssigned = automatic ?: false,
    )
}
