package ai.orkk.shoelog.ui.shoes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ai.orkk.shoelog.domain.ShoeCategory
import ai.orkk.shoelog.domain.ShoePurpose
import ai.orkk.shoelog.domain.Shoe
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class ShoeDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailDisplaysWonPrices() {
        composeRule.setContent {
            ShoeDetailScreen(
                shoe = Shoe(
                    id = 7,
                    brand = "ASICS",
                    model = "Sample",
                    purchasePriceWon = 129_000,
                    listPriceWon = 169_000,
                    targetMeters = 500_000,
                    createdAt = Instant.parse("2026-09-01T00:00:00Z"),
                    updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
                ),
                exercises = emptyList(),
                onBack = {},
                onEdit = {},
                onRetireToggle = {},
                onDelete = {},
            )
        }

        composeRule.onNodeWithText("구매가격 129,000원").assertIsDisplayed()
        composeRule.onNodeWithText("정가 169,000원").assertIsDisplayed()
    }

    @Test
    fun detailDisplaysMetadataAndConfirmsDeletion() {
        var deleteRequested = false
        composeRule.setContent {
            ShoeDetailScreen(
                shoe = Shoe(
                    id = 7,
                    brand = "ASICS",
                    model = "Sample",
                    nickname = "테스트화",
                    targetMeters = 500_000,
                    category = ShoeCategory.MAX_CUSHION,
                    purposes = setOf(ShoePurpose.LSD, ShoePurpose.TEMPO),
                    createdAt = Instant.parse("2026-09-01T00:00:00Z"),
                    updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
                ),
                exercises = emptyList(),
                onBack = {},
                onEdit = {},
                onRetireToggle = {},
                onDelete = { deleteRequested = true },
            )
        }

        composeRule.onNodeWithText("데일리 · 맥스 쿠션화").assertIsDisplayed()
        composeRule.onNodeWithText("LSD").assertIsDisplayed()
        composeRule.onNodeWithText("템포런").assertIsDisplayed()
        composeRule.onNodeWithText("삭제").performClick()
        composeRule.onNodeWithText("테스트화를 삭제할까요?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_delete_shoe").performClick()

        composeRule.runOnIdle { org.junit.Assert.assertTrue(deleteRequested) }
    }
}
