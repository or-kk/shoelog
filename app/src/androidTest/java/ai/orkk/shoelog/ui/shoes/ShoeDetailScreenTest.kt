package ai.orkk.shoelog.ui.shoes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
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
            )
        }

        composeRule.onNodeWithText("구매가격 129,000원").assertIsDisplayed()
        composeRule.onNodeWithText("정가 169,000원").assertIsDisplayed()
    }
}
