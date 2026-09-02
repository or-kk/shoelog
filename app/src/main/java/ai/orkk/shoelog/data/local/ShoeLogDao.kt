package ai.orkk.shoelog.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoeLogDao {
    @Insert
    suspend fun insertShoe(shoe: ShoeEntity): Long

    @Update
    suspend fun updateShoe(shoe: ShoeEntity)

    @Query("SELECT * FROM shoes WHERE id = :shoeId")
    suspend fun shoeById(shoeId: Long): ShoeEntity?

    @Query("SELECT * FROM shoes WHERE id = :shoeId")
    fun observeShoe(shoeId: Long): Flow<ShoeEntity?>

    @Query(
        """
        SELECT s.*,
            s.initialMeters + COALESCE(SUM(
                CASE WHEN e.sourceDeleted = 0 AND e.distanceMeters > 0
                    THEN e.distanceMeters ELSE 0 END
            ), 0) AS totalMeters,
            COUNT(CASE WHEN e.sourceDeleted = 0 THEN 1 END) AS assignedExerciseCount
        FROM shoes s
        LEFT JOIN exercise_shoe_assignments a ON a.shoeId = s.id
        LEFT JOIN exercises e ON e.id = a.exerciseId
        WHERE (:includeRetired = 1 OR s.retired = 0)
        GROUP BY s.id
        ORDER BY s.retired ASC, s.defaultShoe DESC, s.updatedAtEpochMillis DESC
        """,
    )
    fun observeShoes(includeRetired: Boolean): Flow<List<ShoeMileageRow>>

    @Query(
        """
        SELECT s.*,
            s.initialMeters + COALESCE(SUM(
                CASE WHEN e.sourceDeleted = 0 AND e.distanceMeters > 0
                    THEN e.distanceMeters ELSE 0 END
            ), 0) AS totalMeters,
            COUNT(CASE WHEN e.sourceDeleted = 0 THEN 1 END) AS assignedExerciseCount
        FROM shoes s
        LEFT JOIN exercise_shoe_assignments a ON a.shoeId = s.id
        LEFT JOIN exercises e ON e.id = a.exerciseId
        WHERE s.id = :shoeId
        GROUP BY s.id
        """,
    )
    fun observeShoeMileage(shoeId: Long): Flow<ShoeMileageRow?>

    @Query("UPDATE shoes SET retired = :retired, updatedAtEpochMillis = :updatedAt WHERE id = :shoeId")
    suspend fun setShoeRetired(shoeId: Long, retired: Boolean, updatedAt: Long)

    @Query("UPDATE shoes SET defaultShoe = 0")
    suspend fun clearDefaultShoes()

    @Query("UPDATE shoes SET defaultShoe = 1, retired = 0, updatedAtEpochMillis = :updatedAt WHERE id = :shoeId")
    suspend fun markDefaultShoe(shoeId: Long, updatedAt: Long)

    @Transaction
    suspend fun setDefaultShoe(shoeId: Long?, updatedAt: Long) {
        clearDefaultShoes()
        if (shoeId != null) markDefaultShoe(shoeId, updatedAt)
    }

    @Query("SELECT * FROM shoes WHERE defaultShoe = 1 AND retired = 0 LIMIT 1")
    suspend fun defaultShoe(): ShoeEntity?

    @Query("DELETE FROM shoes WHERE id = :shoeId")
    suspend fun deleteShoeById(shoeId: Long)

    @Query("SELECT COUNT(*) FROM exercise_shoe_assignments WHERE shoeId = :shoeId")
    suspend fun assignmentCountForShoe(shoeId: Long): Int

    @Transaction
    suspend fun deleteShoeIfUnused(shoeId: Long): Boolean {
        if (assignmentCountForShoe(shoeId) > 0) return false
        deleteShoeById(shoeId)
        return true
    }

    @Transaction
    suspend fun deleteShoePreservingExercises(shoeId: Long) {
        deleteShoeById(shoeId)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity): Int

    @Transaction
    suspend fun upsertExercises(exercises: List<ExerciseEntity>): Int {
        var inserted = 0
        exercises.forEach { exercise ->
            if (insertExercise(exercise) == -1L) {
                updateExercise(exercise)
            } else {
                inserted += 1
            }
        }
        return inserted
    }

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    suspend fun exerciseById(exerciseId: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE fallbackKey = :fallbackKey LIMIT 1")
    suspend fun exerciseByFallbackKey(fallbackKey: String): ExerciseEntity?

    @Query("SELECT COUNT(*) FROM exercises")
    suspend fun exerciseCount(): Int

    @Query(
        """
        SELECT e.*, a.shoeId, s.brand AS shoeBrand, s.model AS shoeModel,
            s.nickname AS shoeNickname, a.automatic
        FROM exercises e
        LEFT JOIN exercise_shoe_assignments a ON a.exerciseId = e.id
        LEFT JOIN shoes s ON s.id = a.shoeId
        WHERE e.sourceDeleted = 0
            AND (:unassignedOnly = 0 OR a.exerciseId IS NULL)
        ORDER BY e.startEpochMillis DESC
        """,
    )
    fun observeExercises(unassignedOnly: Boolean): Flow<List<ExerciseWithShoeRow>>

    @Query(
        """
        SELECT e.*, a.shoeId, s.brand AS shoeBrand, s.model AS shoeModel,
            s.nickname AS shoeNickname, a.automatic
        FROM exercises e
        LEFT JOIN exercise_shoe_assignments a ON a.exerciseId = e.id
        LEFT JOIN shoes s ON s.id = a.shoeId
        WHERE e.id = :exerciseId
        """,
    )
    fun observeExercise(exerciseId: String): Flow<ExerciseWithShoeRow?>

    @Query("SELECT COUNT(*) FROM exercises e LEFT JOIN exercise_shoe_assignments a ON a.exerciseId = e.id WHERE e.sourceDeleted = 0 AND a.exerciseId IS NULL")
    fun observeUnassignedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAssignment(assignment: ExerciseShoeAssignmentEntity)

    @Query("DELETE FROM exercise_shoe_assignments WHERE exerciseId = :exerciseId")
    suspend fun deleteAssignment(exerciseId: String)

    @Transaction
    suspend fun assignShoe(
        exerciseId: String,
        shoeId: Long?,
        automatic: Boolean,
        assignedAtEpochMillis: Long,
    ) {
        if (shoeId == null) {
            deleteAssignment(exerciseId)
        } else {
            upsertAssignment(
                ExerciseShoeAssignmentEntity(
                    exerciseId = exerciseId,
                    shoeId = shoeId,
                    automatic = automatic,
                    assignedAtEpochMillis = assignedAtEpochMillis,
                ),
            )
        }
    }

    @Query("SELECT * FROM exercise_shoe_assignments WHERE exerciseId = :exerciseId")
    suspend fun assignmentFor(exerciseId: String): ExerciseShoeAssignmentEntity?

    @Query("UPDATE exercises SET sourceDeleted = 1, syncedAtEpochMillis = :syncedAt WHERE startEpochMillis >= :start AND endEpochMillis <= :end AND id NOT LIKE 'sample:%' AND id NOT IN (:presentIds)")
    suspend fun markMissingExercisesDeleted(
        start: Long,
        end: Long,
        presentIds: List<String>,
        syncedAt: Long,
    )

    @Query("UPDATE exercises SET sourceDeleted = 1, syncedAtEpochMillis = :syncedAt WHERE startEpochMillis >= :start AND endEpochMillis <= :end AND id NOT LIKE 'sample:%'")
    suspend fun markAllExercisesDeleted(
        start: Long,
        end: Long,
        syncedAt: Long,
    )

    @Query("DELETE FROM exercises WHERE id LIKE 'sample:%'")
    suspend fun clearSampleExercises()

    @Query("DELETE FROM shoes WHERE id < 0")
    suspend fun clearSampleShoes()
}
