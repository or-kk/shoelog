@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package ai.orkk.shoelog.ui.shoes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.orkk.shoelog.data.repository.DeleteShoeResult
import ai.orkk.shoelog.data.repository.ShoeRepository
import ai.orkk.shoelog.domain.Shoe
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
