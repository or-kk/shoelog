package ai.orkk.shoelog.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "shoes")
data class ShoeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val brand: String,
    val model: String,
    val nickname: String,
    val color: String,
    val purchaseDateEpochDay: Long?,
    val startDateEpochDay: Long?,
    val purchasePriceWon: Long? = null,
    val listPriceWon: Long? = null,
    val initialMeters: Long,
    val targetMeters: Long,
    val photoUri: String?,
    val retired: Boolean,
    val defaultShoe: Boolean,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long,
    val categoryCode: String? = null,
    val purposeCodes: String = "",
)

@Entity(
    tableName = "exercises",
    indices = [Index(value = ["fallbackKey"], unique = true)],
)
data class ExerciseEntity(
    @PrimaryKey val id: String,
    val fallbackKey: String,
    val startEpochMillis: Long,
    val endEpochMillis: Long,
    val distanceMeters: Long?,
    val exerciseType: Int,
    val sourcePackage: String,
    val sourceName: String,
    val sourceUpdatedAtEpochMillis: Long?,
    val syncedAtEpochMillis: Long,
    val sourceDeleted: Boolean,
)

@Entity(
    tableName = "exercise_shoe_assignments",
    foreignKeys = [
        ForeignKey(
            entity = ExerciseEntity::class,
            parentColumns = ["id"],
            childColumns = ["exerciseId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ShoeEntity::class,
            parentColumns = ["id"],
            childColumns = ["shoeId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("shoeId")],
)
data class ExerciseShoeAssignmentEntity(
    @PrimaryKey val exerciseId: String,
    val shoeId: Long,
    val automatic: Boolean,
    val assignedAtEpochMillis: Long,
)

data class ShoeMileageRow(
    @Embedded val shoe: ShoeEntity,
    val totalMeters: Long,
    val assignedExerciseCount: Int,
)

data class ExerciseWithShoeRow(
    @Embedded val exercise: ExerciseEntity,
    val shoeId: Long?,
    val shoeBrand: String?,
    val shoeModel: String?,
    val shoeNickname: String?,
    val automatic: Boolean?,
)
