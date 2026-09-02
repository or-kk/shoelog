package ai.orkk.shoelog.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ai.orkk.shoelog.MainActivity
import org.junit.Rule
import org.junit.Test

class ShoeLogNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun bottomNavigationOpensShoeManagement() {
        composeRule.onNodeWithText("러닝화").performClick()

        composeRule.onNodeWithText("은퇴한 신발 포함").assertIsDisplayed()
    }
}
