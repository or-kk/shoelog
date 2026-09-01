package ai.orkk.shoelog.ui.shoes

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
}
