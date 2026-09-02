package ai.orkk.shoelog.ui.shoes

import ai.orkk.shoelog.domain.Shoe
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShoeEditorViewModelTest {
    @Test
    fun blankBrandAndModelCannotSave() {
        val errors = ShoeEditorForm(brand = " ", model = "").validate()

        assertTrue("brand" in errors)
        assertTrue("model" in errors)
    }

    @Test
    fun negativeInitialAndZeroTargetCannotSave() {
        val errors = ShoeEditorForm(
            brand = "ASICS",
            model = "Sample",
            initialKm = "-1",
            targetKm = "0",
        ).validate()

        assertTrue("initialKm" in errors)
        assertTrue("targetKm" in errors)
    }

    @Test
    fun validKilometerInputConvertsToExactMeters() {
        val draft = ShoeEditorForm(
            brand = " ASICS ",
            model = " Sample Runner ",
            initialKm = "12.3",
            targetKm = "500",
        ).toDraft()

        assertEquals("ASICS", draft.brand)
        assertEquals("Sample Runner", draft.model)
        assertEquals(12_300L, draft.initialMeters)
        assertEquals(500_000L, draft.targetMeters)
    }

    @Test
    fun decimalPurchasePriceCannotSave() {
        val errors = ShoeEditorForm(
            brand = "ASICS",
            model = "Sample",
            purchasePriceWon = "129000.5",
        ).validate()

        assertTrue("purchasePriceWon" in errors)
    }

    @Test
    fun negativeListPriceCannotSave() {
        val errors = ShoeEditorForm(
            brand = "ASICS",
            model = "Sample",
            listPriceWon = "-1",
        ).validate()

        assertTrue("listPriceWon" in errors)
    }

    @Test
    fun validWonInputsConvertToExactPrices() {
        val draft = ShoeEditorForm(
            brand = "ASICS",
            model = "Sample",
            purchasePriceWon = "129000",
            listPriceWon = "169000",
        ).toDraft()

        assertEquals(129_000L, draft.purchasePriceWon)
        assertEquals(169_000L, draft.listPriceWon)
    }

    @Test
    fun existingShoePricesPopulateEditableForm() {
        val shoe = Shoe(
            id = 7,
            brand = "ASICS",
            model = "Sample",
            purchasePriceWon = 129_000,
            listPriceWon = 169_000,
            targetMeters = 500_000,
            createdAt = Instant.parse("2026-09-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
        )

        val form = ShoeEditorForm.from(shoe)

        assertEquals("129000", form.purchasePriceWon)
        assertEquals("169000", form.listPriceWon)
    }
}
