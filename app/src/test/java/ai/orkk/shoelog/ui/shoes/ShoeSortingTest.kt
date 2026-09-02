package ai.orkk.shoelog.ui.shoes

import ai.orkk.shoelog.domain.MileageSummary
import ai.orkk.shoelog.domain.MileageStatus
import ai.orkk.shoelog.domain.Shoe
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class ShoeSortingTest {
    private val alpha = shoe(
        id = 1,
        brand = "ASICS",
        model = "Alpha",
        mileageMeters = 300_000,
        purchaseDate = LocalDate.parse("2026-01-01"),
        purchasePriceWon = 100_000,
        listPriceWon = 180_000,
        updatedAt = Instant.parse("2026-03-01T00:00:00Z"),
    )
    private val beta = shoe(
        id = 2,
        brand = "Brooks",
        model = "Beta",
        mileageMeters = 100_000,
        purchaseDate = LocalDate.parse("2026-02-01"),
        purchasePriceWon = 120_000,
        listPriceWon = 160_000,
        updatedAt = Instant.parse("2026-02-01T00:00:00Z"),
    )
    private val unknown = shoe(
        id = 3,
        brand = "Nike",
        model = "Unknown",
        mileageMeters = 200_000,
        purchaseDate = null,
        purchasePriceWon = null,
        listPriceWon = null,
        updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
    )

    @Test
    fun everySortKeyOrdersAscendingAndDescending() {
        val shoes = listOf(unknown, alpha, beta)
        val expected = mapOf(
            ShoeSortKey.MILEAGE to (listOf(2L, 3L, 1L) to listOf(1L, 3L, 2L)),
            ShoeSortKey.PURCHASE_DATE to (listOf(1L, 2L, 3L) to listOf(2L, 1L, 3L)),
            ShoeSortKey.PURCHASE_PRICE to (listOf(1L, 2L, 3L) to listOf(2L, 1L, 3L)),
            ShoeSortKey.LIST_PRICE to (listOf(2L, 1L, 3L) to listOf(1L, 2L, 3L)),
            ShoeSortKey.BRAND_MODEL to (listOf(1L, 2L, 3L) to listOf(3L, 2L, 1L)),
            ShoeSortKey.UPDATED_AT to (listOf(3L, 2L, 1L) to listOf(1L, 2L, 3L)),
        )

        expected.forEach { (key, orders) ->
            assertEquals(
                "$key ascending",
                orders.first,
                sortShoes(shoes, key, SortDirection.ASCENDING).map { it.id },
            )
            assertEquals(
                "$key descending",
                orders.second,
                sortShoes(shoes, key, SortDirection.DESCENDING).map { it.id },
            )
        }
    }

    @Test
    fun equalValuesUseBrandModelThenIdAsStableTieBreakers() {
        val second = shoe(id = 5, brand = "same", model = "Runner", mileageMeters = 10_000)
        val first = shoe(id = 4, brand = "Same", model = "Runner", mileageMeters = 10_000)

        assertEquals(
            listOf(4L, 5L),
            sortShoes(
                listOf(second, first),
                ShoeSortKey.MILEAGE,
                SortDirection.DESCENDING,
            ).map { it.id },
        )
    }

    private fun shoe(
        id: Long,
        brand: String,
        model: String,
        mileageMeters: Long,
        purchaseDate: LocalDate? = LocalDate.parse("2026-01-01"),
        purchasePriceWon: Long? = 100_000,
        listPriceWon: Long? = 150_000,
        updatedAt: Instant = Instant.parse("2026-01-01T00:00:00Z"),
    ) = Shoe(
        id = id,
        brand = brand,
        model = model,
        purchaseDate = purchaseDate,
        purchasePriceWon = purchasePriceWon,
        listPriceWon = listPriceWon,
        targetMeters = 500_000,
        createdAt = Instant.parse("2025-01-01T00:00:00Z"),
        updatedAt = updatedAt,
        mileage = MileageSummary(
            totalMeters = mileageMeters,
            remainingMeters = 500_000 - mileageMeters,
            progress = mileageMeters / 500_000f,
            usageRatio = mileageMeters / 500_000f,
            status = MileageStatus.NORMAL,
        ),
    )
}
