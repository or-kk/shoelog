package ai.orkk.shoelog.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import ai.orkk.shoelog.domain.Exercise
import ai.orkk.shoelog.ui.home.HomeScreen
import ai.orkk.shoelog.ui.home.HomeUiState
import java.time.Instant
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun emptyStateIsExplicit() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(isLoading = false),
                onRefresh = {},
                onAddShoe = {},
                onOpenShoe = {},
                onOpenExercises = {},
            )
        }

        composeRule.onNodeWithTag("home_empty").assertIsDisplayed()
    }

    @Test
    fun loadingStateIsExplicit() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(isLoading = true),
                onRefresh = {},
                onAddShoe = {},
                onOpenShoe = {},
                onOpenExercises = {},
            )
        }

        composeRule.onNodeWithTag("home_loading").assertIsDisplayed()
    }

    @Test
    fun syncedRunsWithoutShoesStillOfferShoeRegistration() {
        composeRule.setContent {
            HomeScreen(
                state = HomeUiState(
                    isLoading = false,
                    recentExercises = listOf(
                        Exercise(
                            id = "run:1",
                            fallbackKey = "fallback:1",
                            startTime = Instant.parse("2026-09-01T00:00:00Z"),
                            endTime = Instant.parse("2026-09-01T00:30:00Z"),
                            distanceMeters = 5_000,
                            exerciseType = 56,
                            sourcePackage = "com.sec.android.app.shealth",
                            sourceName = "Samsung Health",
                            sourceUpdatedAt = Instant.parse("2026-09-01T00:31:00Z"),
                            syncedAt = Instant.parse("2026-09-01T00:31:00Z"),
                        ),
                    ),
                    unassignedCount = 1,
                ),
                onRefresh = {},
                onAddShoe = {},
                onOpenShoe = {},
                onOpenExercises = {},
            )
        }

        composeRule.onNodeWithText("러닝화 추가").assertIsDisplayed()
    }
}
