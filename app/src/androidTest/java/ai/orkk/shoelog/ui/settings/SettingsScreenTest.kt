package ai.orkk.shoelog.ui.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import ai.orkk.shoelog.data.health.HealthConnectAvailability
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun settingsExplainsHealthPermissionsAndPrivacy() {
        composeRule.setContent {
            SettingsScreen(
                state = SettingsUiState(availability = HealthConnectAvailability.AVAILABLE),
                onRequestPermissions = {},
                onRequestHistory = {},
                onSync = {},
                onAutoAssignChange = {},
                onSampleModeChange = {},
            )
        }

        composeRule.onNodeWithText("Health Connect 권한").assertIsDisplayed()
        composeRule.onNodeWithTag("settings_list")
            .performScrollToNode(hasText("개인정보 보호"))
        composeRule.onNodeWithText("개인정보 보호").assertIsDisplayed()
    }
}
