package ai.orkk.shoelog.data.repository

import ai.orkk.shoelog.data.local.ExerciseEntity
import ai.orkk.shoelog.data.local.ExerciseWithShoeRow
import ai.orkk.shoelog.data.local.ShoeEntity
import ai.orkk.shoelog.data.local.ShoeMileageRow
import ai.orkk.shoelog.domain.ShoeCategory
import ai.orkk.shoelog.domain.ShoePurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RepositoryMappingTest {
    @Test
    fun shoeMappingPreservesOptionalAndRetiredValues() {
        val row = ShoeMileageRow(
            shoe = shoeEntity(photoUri = null, retired = true),
            totalMeters = 123_000,
            assignedExerciseCount = 4,
        )

        val shoe = row.toDomainShoe()

        assertNull(shoe.photoUri)
        assertNull(shoe.purchaseDate)
        assertTrue(shoe.retired)
        assertEquals(123_000, shoe.mileage.totalMeters)
    }

    @Test
    fun shoeMappingPreservesWonPrices() {
        val row = ShoeMileageRow(
            shoe = shoeEntity(
                photoUri = null,
                retired = false,
                purchasePriceWon = 129_000,
                listPriceWon = 169_000,
            ),
            totalMeters = 0,
            assignedExerciseCount = 0,
        )

        val shoe = row.toDomainShoe()

        assertEquals(129_000L, shoe.purchasePriceWon)
        assertEquals(169_000L, shoe.listPriceWon)
    }

    @Test
    fun shoeMetadataMapsBetweenStorageAndDomain() {
        val row = ShoeMileageRow(
            shoe = shoeEntity(
                photoUri = null,
                retired = false,
                categoryCode = "max-cushion",
                purposeCodes = "daily,lsd",
            ),
            totalMeters = 0,
            assignedExerciseCount = 0,
        )

        val shoe = row.toDomainShoe()

        assertEquals(ShoeCategory.MAX_CUSHION, shoe.category)
        assertEquals(setOf(ShoePurpose.DAILY, ShoePurpose.LSD), shoe.purposes)

        val entity = ShoeDraft(
            brand = "Fictional",
            model = "Cloud Test",
            category = shoe.category,
            purposes = shoe.purposes,
        ).toEntity(createdAt = 1, updatedAt = 2)

        assertEquals("max-cushion", entity.categoryCode)
        assertEquals("daily,lsd", entity.purposeCodes)
    }

    @Test
    fun distanceLessExerciseRemainsVisibleAndUnassigned() {
        val row = ExerciseWithShoeRow(
            exercise = exerciseEntity(distanceMeters = null),
            shoeId = null,
            shoeBrand = null,
            shoeModel = null,
            shoeNickname = null,
            automatic = null,
        )

        val exercise = row.toDomainExercise()

        assertNull(exercise.distanceMeters)
        assertNull(exercise.assignedShoeId)
        assertFalse(exercise.sourceDeleted)
    }

    private fun shoeEntity(
        photoUri: String?,
        retired: Boolean,
        purchasePriceWon: Long? = null,
        listPriceWon: Long? = null,
        categoryCode: String? = null,
        purposeCodes: String = "",
    ) = ShoeEntity(
        id = 7,
        brand = "Fictional",
        model = "Cloud Test",
        nickname = "테스트화",
        color = "검정",
        purchaseDateEpochDay = null,
        startDateEpochDay = null,
        purchasePriceWon = purchasePriceWon,
        listPriceWon = listPriceWon,
        initialMeters = 3_000,
        targetMeters = 500_000,
        photoUri = photoUri,
        retired = retired,
        defaultShoe = false,
        categoryCode = categoryCode,
        purposeCodes = purposeCodes,
        createdAtEpochMillis = 1_700_000_000_000,
        updatedAtEpochMillis = 1_700_000_100_000,
    )

    private fun exerciseEntity(distanceMeters: Long?) = ExerciseEntity(
        id = "run:test",
        fallbackKey = "fallback:test",
        startEpochMillis = 1_700_000_000_000,
        endEpochMillis = 1_700_003_600_000,
        distanceMeters = distanceMeters,
        exerciseType = 56,
        sourcePackage = "com.example.fictional",
        sourceName = "샘플 건강 앱",
        sourceUpdatedAtEpochMillis = null,
        syncedAtEpochMillis = 1_700_004_000_000,
        sourceDeleted = false,
    )
}
