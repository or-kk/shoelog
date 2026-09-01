package ai.orkk.shoelog.ui.shoes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class ShoeEditorScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editorShowsRequiredFieldsAndPhotoAction() {
        composeRule.setContent {
            ShoeEditorScreen(
                state = ShoeEditorUiState(),
                onFormChange = {},
                onPickPhoto = {},
                onSave = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithText("브랜드").assertIsDisplayed()
        composeRule.onNodeWithText("모델명").assertIsDisplayed()
        composeRule.onNodeWithText("사진 선택").assertIsDisplayed()
    }
}
