package ai.orkk.shoelog.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import ai.orkk.shoelog.ui.home.HomeScreen
import ai.orkk.shoelog.ui.home.HomeUiState
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
}
