@file:OptIn(
    kotlinx.coroutines.ExperimentalCoroutinesApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)

package ai.orkk.shoelog.ui.shoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.orkk.shoelog.data.repository.DeleteShoeResult
import ai.orkk.shoelog.data.repository.ShoeRepository
import ai.orkk.shoelog.domain.Shoe
import ai.orkk.shoelog.ui.EmptyState
import ai.orkk.shoelog.ui.ShoeMileageCard
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ShoeSortKey(val displayName: String) {
    MILEAGE("달린 거리"),
    PURCHASE_DATE("구매 날짜"),
    PURCHASE_PRICE("구매 가격"),
    LIST_PRICE("정가"),
    BRAND_MODEL("브랜드·모델"),
    UPDATED_AT("최근 수정일"),
}

enum class SortDirection {
    ASCENDING,
    DESCENDING,
}

fun sortShoes(
    shoes: List<Shoe>,
    key: ShoeSortKey,
    direction: SortDirection,
): List<Shoe> {
    val fallback = compareBy<Shoe>(
        { it.brand.lowercase(Locale.KOREA) },
        { it.model.lowercase(Locale.KOREA) },
        Shoe::id,
    )
    val primary = when (key) {
        ShoeSortKey.MILEAGE -> compareBy<Shoe> { it.mileage.totalMeters }
        ShoeSortKey.PURCHASE_DATE -> compareBy<Shoe> { it.purchaseDate }
        ShoeSortKey.PURCHASE_PRICE -> compareBy<Shoe> { it.purchasePriceWon }
        ShoeSortKey.LIST_PRICE -> compareBy<Shoe> { it.listPriceWon }
        ShoeSortKey.BRAND_MODEL -> compareBy<Shoe>(
            { it.brand.lowercase(Locale.KOREA) },
            { it.model.lowercase(Locale.KOREA) },
        )
        ShoeSortKey.UPDATED_AT -> compareBy<Shoe> { it.updatedAt }
    }
    val comparator = Comparator<Shoe> { first, second ->
        val primaryResult = if (direction == SortDirection.ASCENDING) {
            primary.compare(first, second)
        } else {
            primary.compare(second, first)
        }
        primaryResult.takeIf { it != 0 } ?: fallback.compare(first, second)
    }
    val (withValue, withoutValue) = shoes.partition { it.hasSortValue(key) }
    return withValue.sortedWith(comparator) + withoutValue.sortedWith(fallback)
}

private fun Shoe.hasSortValue(key: ShoeSortKey): Boolean = when (key) {
    ShoeSortKey.PURCHASE_DATE -> purchaseDate != null
    ShoeSortKey.PURCHASE_PRICE -> purchasePriceWon != null
    ShoeSortKey.LIST_PRICE -> listPriceWon != null
    else -> true
}

data class ShoeManagementUiState(
    val shoes: List<Shoe> = emptyList(),
    val includeRetired: Boolean = false,
    val sortKey: ShoeSortKey = ShoeSortKey.UPDATED_AT,
    val sortDirection: SortDirection = SortDirection.DESCENDING,
    val pendingDelete: Shoe? = null,
    val message: String? = null,
)

private data class ManagementFeedback(
    val pendingDelete: Shoe? = null,
    val message: String? = null,
)

class ShoeManagementViewModel(private val repository: ShoeRepository) : ViewModel() {
    private val includeRetired = MutableStateFlow(false)
    private val sortKey = MutableStateFlow(ShoeSortKey.UPDATED_AT)
    private val sortDirection = MutableStateFlow(SortDirection.DESCENDING)
    private val feedback = MutableStateFlow(ManagementFeedback())

    private val shoes = includeRetired.flatMapLatest(repository::observeShoes)

    val state: StateFlow<ShoeManagementUiState> = combine(
        shoes,
        includeRetired,
        sortKey,
        sortDirection,
        feedback,
    ) { shoeList, retired, key, direction, currentFeedback ->
        ShoeManagementUiState(
            shoes = sortShoes(shoeList, key, direction),
            includeRetired = retired,
            sortKey = key,
            sortDirection = direction,
            pendingDelete = currentFeedback.pendingDelete,
            message = currentFeedback.message,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShoeManagementUiState(),
    )

    fun setIncludeRetired(include: Boolean) {
        includeRetired.value = include
    }

    fun setSortKey(key: ShoeSortKey) {
        sortKey.value = key
    }

    fun toggleSortDirection() {
        sortDirection.value = if (sortDirection.value == SortDirection.ASCENDING) {
            SortDirection.DESCENDING
        } else {
            SortDirection.ASCENDING
        }
    }

    fun requestDelete(shoe: Shoe) {
        feedback.value = ManagementFeedback(pendingDelete = shoe)
    }

    fun cancelDelete() {
        feedback.value = feedback.value.copy(pendingDelete = null)
    }

    fun consumeMessage() {
        feedback.value = feedback.value.copy(message = null)
    }

    fun confirmDelete() {
        val shoe = feedback.value.pendingDelete ?: return
        viewModelScope.launch {
            val message = runCatching { repository.deleteIfUnused(shoe.id) }
                .fold(
                    onSuccess = {
                        when (it) {
                            DeleteShoeResult.DELETED -> "러닝화를 삭제했습니다."
                            DeleteShoeResult.BLOCKED_BY_ASSIGNMENTS ->
                                "달리기 기록이 연결된 러닝화는 삭제할 수 없습니다. 은퇴 처리하거나 기록 배정을 해제한 뒤 다시 시도해 주세요."
                            DeleteShoeResult.NOT_FOUND -> "러닝화를 찾을 수 없습니다."
                        }
                    },
                    onFailure = { "러닝화를 삭제하지 못했습니다." },
                )
            feedback.value = ManagementFeedback(message = message)
        }
    }

    companion object {
        fun factory(repository: ShoeRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    ShoeManagementViewModel(repository) as T
            }
    }
}

@Composable
fun ShoeListScreen(
    state: ShoeManagementUiState,
    onIncludeRetiredChange: (Boolean) -> Unit,
    onSortKeyChange: (ShoeSortKey) -> Unit,
    onToggleSortDirection: () -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Shoe) -> Unit,
    onConfirmDelete: () -> Unit,
    onCancelDelete: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("러닝화 관리") },
                actions = { TextButton(onClick = onAdd) { Text("추가") } },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("은퇴한 신발 포함")
                    Switch(checked = state.includeRetired, onCheckedChange = onIncludeRetiredChange)
                }
            }
            item {
                SortControls(
                    sortKey = state.sortKey,
                    direction = state.sortDirection,
                    onSortKeyChange = onSortKeyChange,
                    onToggleDirection = onToggleSortDirection,
                )
            }
            state.message?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.primary) }
            }
            if (state.shoes.isEmpty()) {
                item {
                    EmptyState(
                        title = "등록된 러닝화가 없습니다",
                        message = "러닝화를 추가해 마일리지를 관리하세요.",
                        actionLabel = "러닝화 추가",
                        onAction = onAdd,
                    )
                }
            } else {
                items(state.shoes, key = Shoe::id) { shoe ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        ShoeMileageCard(shoe, onClick = { onOpen(shoe.id) })
                        Text(
                            "구매일 ${shoe.purchaseDate ?: "미입력"} · 구매가격 ${formatWon(shoe.purchasePriceWon)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = { onEdit(shoe.id) },
                                modifier = Modifier.testTag("edit_shoe_${shoe.id}"),
                            ) { Text("수정") }
                            TextButton(
                                onClick = { onDelete(shoe) },
                                modifier = Modifier.testTag("delete_shoe_${shoe.id}"),
                            ) { Text("삭제") }
                        }
                    }
                }
            }
        }
    }

    state.pendingDelete?.let { shoe ->
        DeleteShoeDialog(
            shoe = shoe,
            onConfirm = onConfirmDelete,
            onDismiss = onCancelDelete,
        )
    }
}

@Composable
private fun SortControls(
    sortKey: ShoeSortKey,
    direction: SortDirection,
    onSortKeyChange: (ShoeSortKey) -> Unit,
    onToggleDirection: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text("정렬: ${sortKey.displayName}")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                ShoeSortKey.entries.forEach { key ->
                    DropdownMenuItem(
                        text = { Text(key.displayName) },
                        onClick = {
                            onSortKeyChange(key)
                            expanded = false
                        },
                    )
                }
            }
        }
        OutlinedButton(onClick = onToggleDirection) {
            Text(if (direction == SortDirection.ASCENDING) "오름차순" else "내림차순")
        }
    }
}

@Composable
internal fun DeleteShoeDialog(
    shoe: Shoe,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val name = shoe.nickname.ifBlank { shoe.model }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${name}를 삭제할까요?") },
        text = { Text("삭제한 러닝화는 복구할 수 없습니다.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("confirm_delete_shoe"),
            ) { Text("삭제") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
