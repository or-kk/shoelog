package ai.orkk.shoelog.ui.shoes

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performClick
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ai.orkk.shoelog.domain.ShoeCategory
import ai.orkk.shoelog.domain.ShoePurpose
import org.junit.Assert.assertEquals
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

    @Test
    fun editorDoesNotOfferRetirementWhileSaving() {
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
            .performScrollToNode(hasText("저장"))
        composeRule.onAllNodesWithText("은퇴 상태").assertCountEquals(0)
    }

    @Test
    fun editorSelectsOneCategoryAndMultiplePurposes() {
        var latestForm = ShoeEditorForm()
        composeRule.setContent {
            var form by remember { mutableStateOf(ShoeEditorForm()) }
            ShoeEditorScreen(
                state = ShoeEditorUiState(form = form),
                onFormChange = {
                    form = it
                    latestForm = it
                },
                onPickPhoto = {},
                onSave = {},
                onBack = {},
            )
        }

        composeRule.onNodeWithTag("shoe_editor_list")
            .performScrollToNode(hasTestTag("category_max-cushion"))
        composeRule.onNodeWithTag("category_max-cushion").performClick()
        composeRule.onNodeWithTag("shoe_editor_list")
            .performScrollToNode(hasTestTag("purpose_lsd"))
        composeRule.onNodeWithTag("purpose_lsd").performClick()
        composeRule.onNodeWithTag("purpose_tempo").performClick()

        composeRule.runOnIdle {
            assertEquals(ShoeCategory.MAX_CUSHION, latestForm.category)
            assertEquals(setOf(ShoePurpose.LSD, ShoePurpose.TEMPO), latestForm.purposes)
        }

        composeRule.onNodeWithTag("shoe_editor_list")
            .performScrollToNode(hasTestTag("category_stability"))
        composeRule.onNodeWithTag("category_stability").performClick()

        composeRule.runOnIdle {
            assertEquals(ShoeCategory.STABILITY, latestForm.category)
            assertEquals(setOf(ShoePurpose.LSD, ShoePurpose.TEMPO), latestForm.purposes)
        }
    }
}
