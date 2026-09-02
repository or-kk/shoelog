package ai.orkk.shoelog.ui.shoes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
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
        composeRule.onNodeWithTag("shoe_editor_list")
            .performScrollToNode(hasText("사진 선택"))
        composeRule.onNodeWithText("사진 선택").assertIsDisplayed()
    }

    @Test
    fun editorOffersPurchaseAndListPriceFields() {
        composeRule.setContent {
            ShoeEditorScreen(
                state = ShoeEditorUiState(),
                onFormChange = {},
                onPickPhoto = {},
                onSave = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("shoe_editor_list")
            .performScrollToNode(hasText("구매가격 (원)"))
        composeRule.onNodeWithText("구매가격 (원)").assertIsDisplayed()
        composeRule.onNodeWithTag("shoe_editor_list")
            .performScrollToNode(hasText("정가 (원)"))
        composeRule.onNodeWithText("정가 (원)").assertIsDisplayed()
    }

    @Test
    fun editorExplainsInvalidPurchasePrice() {
        composeRule.setContent {
            ShoeEditorScreen(
                state = ShoeEditorUiState(
                    errors = mapOf("purchasePriceWon" to "0 이상의 원화 정수를 입력해 주세요."),
                ),
                onFormChange = {},
                onPickPhoto = {},
                onSave = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("shoe_editor_list")
            .performScrollToNode(hasText("0 이상의 원화 정수를 입력해 주세요."))
        composeRule.onNodeWithText("0 이상의 원화 정수를 입력해 주세요.").assertIsDisplayed()
    }
}
