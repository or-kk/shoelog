package ai.orkk.shoelog.domain

import java.time.Instant
import java.time.LocalDate

enum class MileageStatus {
    NORMAL,
    NEAR_TARGET,
    TARGET_REACHED,
}

data class MileageSummary(
    val totalMeters: Long,
    val remainingMeters: Long,
    val progress: Float,
    val usageRatio: Float,
    val status: MileageStatus,
)

data class Shoe(
    val id: Long = 0,
    val brand: String,
    val model: String,
    val nickname: String = "",
    val color: String = "",
    val purchaseDate: LocalDate? = null,
    val startDate: LocalDate? = null,
    val purchasePriceWon: Long? = null,
    val listPriceWon: Long? = null,
    val initialMeters: Long = 0,
    val targetMeters: Long,
    val photoUri: String? = null,
    val retired: Boolean = false,
    val defaultShoe: Boolean = false,
    val createdAt: Instant,
    val updatedAt: Instant,
    val mileage: MileageSummary = MileageCalculator.calculate(initialMeters, emptyList(), targetMeters),
)

data class Exercise(
    val id: String,
    val fallbackKey: String,
    val startTime: Instant,
    val endTime: Instant,
    val distanceMeters: Long?,
    val exerciseType: Int,
    val sourcePackage: String,
    val sourceName: String,
    val sourceUpdatedAt: Instant?,
    val syncedAt: Instant,
    val sourceDeleted: Boolean = false,
    val assignedShoeId: Long? = null,
    val assignedShoeLabel: String? = null,
    val automaticallyAssigned: Boolean = false,
)
