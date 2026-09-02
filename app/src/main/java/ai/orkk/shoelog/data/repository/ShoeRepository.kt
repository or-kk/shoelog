package ai.orkk.shoelog.data.repository

import ai.orkk.shoelog.data.local.ShoeEntity
import ai.orkk.shoelog.data.local.ShoeLogDao
import ai.orkk.shoelog.data.local.ShoeMileageRow
import ai.orkk.shoelog.domain.MileageCalculator
import ai.orkk.shoelog.domain.Shoe
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

data class ShoeDraft(
    val brand: String,
    val model: String,
    val nickname: String = "",
    val color: String = "",
    val purchaseDate: LocalDate? = null,
    val startDate: LocalDate? = null,
    val purchasePriceWon: Long? = null,
    val listPriceWon: Long? = null,
    val initialMeters: Long = 0,
    val targetMeters: Long = 500_000,
    val photoUri: String? = null,
    val retired: Boolean = false,
    val defaultShoe: Boolean = false,
)

class ShoeRepository(
    private val dao: ShoeLogDao,
    private val clock: Clock = Clock.systemUTC(),
) {
    fun observeShoes(includeRetired: Boolean = false): Flow<List<Shoe>> =
        dao.observeShoes(includeRetired).map { rows -> rows.map(ShoeMileageRow::toDomainShoe) }

    fun observeShoe(shoeId: Long): Flow<Shoe?> =
        dao.observeShoeMileage(shoeId).map { it?.toDomainShoe() }

    suspend fun save(shoeId: Long?, draft: ShoeDraft): Long {
        val normalized = draft.normalizedAndValidated()
        val now = clock.instant().toEpochMilli()
        val savedId = if (shoeId == null) {
            dao.insertShoe(normalized.copy(retired = false).toEntity(createdAt = now, updatedAt = now))
        } else {
            val existing = requireNotNull(dao.shoeById(shoeId)) { "러닝화를 찾을 수 없습니다." }
            dao.updateShoe(normalized.toEntity(id = shoeId, createdAt = existing.createdAtEpochMillis, updatedAt = now))
            shoeId
        }
        if (normalized.defaultShoe) dao.setDefaultShoe(savedId, now)
        return savedId
    }

    suspend fun setRetired(shoeId: Long, retired: Boolean) {
        dao.setShoeRetired(shoeId, retired, clock.instant().toEpochMilli())
        if (retired && dao.defaultShoe()?.id == shoeId) {
            dao.setDefaultShoe(null, clock.instant().toEpochMilli())
        }
    }

    suspend fun setDefault(shoeId: Long?) {
        dao.setDefaultShoe(shoeId, clock.instant().toEpochMilli())
    }

    suspend fun deleteIfUnused(shoeId: Long): Boolean {
        val row = dao.observeShoeMileage(shoeId).first() ?: return true
        if (row.assignedExerciseCount > 0) return false
        dao.deleteShoePreservingExercises(shoeId)
        return true
    }

    fun observeBrandAndModelSuggestions(): Flow<Pair<List<String>, List<String>>> =
        observeShoes(includeRetired = true).map { shoes ->
            shoes.map { it.brand }.distinct().sorted() to
                shoes.map { it.model }.distinct().sorted()
        }
}

internal fun ShoeMileageRow.toDomainShoe(): Shoe = Shoe(
    id = shoe.id,
    brand = shoe.brand,
    model = shoe.model,
    nickname = shoe.nickname,
    color = shoe.color,
    purchaseDate = shoe.purchaseDateEpochDay?.let(LocalDate::ofEpochDay),
    startDate = shoe.startDateEpochDay?.let(LocalDate::ofEpochDay),
    purchasePriceWon = shoe.purchasePriceWon,
    listPriceWon = shoe.listPriceWon,
    initialMeters = shoe.initialMeters,
    targetMeters = shoe.targetMeters,
    photoUri = shoe.photoUri,
    retired = shoe.retired,
    defaultShoe = shoe.defaultShoe,
    createdAt = Instant.ofEpochMilli(shoe.createdAtEpochMillis),
    updatedAt = Instant.ofEpochMilli(shoe.updatedAtEpochMillis),
    mileage = MileageCalculator.calculate(totalMeters, emptyList(), shoe.targetMeters),
)

private fun ShoeDraft.normalizedAndValidated(): ShoeDraft {
    val normalized = copy(
        brand = brand.trim(),
        model = model.trim(),
        nickname = nickname.trim(),
        color = color.trim(),
        photoUri = photoUri?.trim()?.takeIf(String::isNotEmpty),
    )
    require(normalized.brand.isNotEmpty()) { "브랜드를 입력해 주세요." }
    require(normalized.model.isNotEmpty()) { "모델명을 입력해 주세요." }
    require(normalized.initialMeters >= 0) { "초기거리는 0 이상이어야 합니다." }
    require(normalized.targetMeters > 0) { "목표거리는 0보다 커야 합니다." }
    return normalized
}

private fun ShoeDraft.toEntity(
    id: Long = 0,
    createdAt: Long,
    updatedAt: Long,
) = ShoeEntity(
    id = id,
    brand = brand,
    model = model,
    nickname = nickname,
    color = color,
    purchaseDateEpochDay = purchaseDate?.toEpochDay(),
    startDateEpochDay = startDate?.toEpochDay(),
    purchasePriceWon = purchasePriceWon,
    listPriceWon = listPriceWon,
    initialMeters = initialMeters,
    targetMeters = targetMeters,
    photoUri = photoUri,
    retired = retired,
    defaultShoe = defaultShoe,
    createdAtEpochMillis = createdAt,
    updatedAtEpochMillis = updatedAt,
)
