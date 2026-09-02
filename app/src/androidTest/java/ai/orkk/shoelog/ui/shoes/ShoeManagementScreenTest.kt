package ai.orkk.shoelog.ui.shoes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import ai.orkk.shoelog.domain.Shoe
import ai.orkk.shoelog.domain.ShoeCategory
import ai.orkk.shoelog.domain.ShoePurpose
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ShoeManagementScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun managementShowsMetadataSortingActionsAndDeleteConfirmation() {
        val shoe = shoe()
        var deleted: Shoe? = null
        composeRule.setContent {
            var state by remember { mutableStateOf(ShoeManagementUiState(shoes = listOf(shoe))) }
            ShoeListScreen(
                state = state,
                onIncludeRetiredChange = {},
                onSortKeyChange = {},
                onToggleSortDirection = {},
                onAdd = {},
                onOpen = {},
                onEdit = {},
                onDelete = { state = state.copy(pendingDelete = it) },
                onConfirmDelete = {
                    deleted = state.pendingDelete
                    state = state.copy(pendingDelete = null)
                },
                onCancelDelete = { state = state.copy(pendingDelete = null) },
            )
        }

        composeRule.onNodeWithText("추가").assertIsDisplayed()
        composeRule.onNodeWithText("데일리 · 맥스 쿠션화").assertIsDisplayed()
        composeRule.onNodeWithText("LSD").assertIsDisplayed()
        composeRule.onNodeWithText("템포런").assertIsDisplayed()
        composeRule.onNodeWithText("정렬: 최근 수정일").assertIsDisplayed()
        composeRule.onNodeWithText("내림차순").assertIsDisplayed()
        composeRule.onNodeWithTag("edit_shoe_7").assertIsDisplayed()
        composeRule.onNodeWithTag("delete_shoe_7").performClick()
        composeRule.onNodeWithText("테스트화를 삭제할까요?").assertIsDisplayed()
        composeRule.onNodeWithTag("confirm_delete_shoe").performClick()

        composeRule.runOnIdle { assertEquals(shoe, deleted) }
    }

    @Test
    fun blockedDeletionMessageExplainsRecovery() {
        composeRule.setContent {
            ShoeListScreen(
                state = ShoeManagementUiState(
                    message = "달리기 기록이 연결된 러닝화는 삭제할 수 없습니다. 은퇴 처리하거나 기록 배정을 해제한 뒤 다시 시도해 주세요.",
                ),
                onIncludeRetiredChange = {},
                onSortKeyChange = {},
                onToggleSortDirection = {},
                onAdd = {},
                onOpen = {},
                onEdit = {},
                onDelete = {},
                onConfirmDelete = {},
                onCancelDelete = {},
            )
        }

        composeRule.onNodeWithText(
            "달리기 기록이 연결된 러닝화는 삭제할 수 없습니다. 은퇴 처리하거나 기록 배정을 해제한 뒤 다시 시도해 주세요.",
        ).assertIsDisplayed()
    }

    private fun shoe() = Shoe(
        id = 7,
        brand = "ASICS",
        model = "Sample",
        nickname = "테스트화",
        purchaseDate = LocalDate.parse("2026-08-01"),
        purchasePriceWon = 129_000,
        targetMeters = 500_000,
        category = ShoeCategory.MAX_CUSHION,
        purposes = setOf(ShoePurpose.LSD, ShoePurpose.TEMPO),
        createdAt = Instant.parse("2026-09-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-09-02T00:00:00Z"),
    )
}
