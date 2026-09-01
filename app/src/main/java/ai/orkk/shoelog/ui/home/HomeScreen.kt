package ai.orkk.shoelog.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.orkk.shoelog.AppContainer
import ai.orkk.shoelog.data.health.HealthConnectAvailability
import ai.orkk.shoelog.data.repository.SyncResult
import ai.orkk.shoelog.domain.Exercise
import ai.orkk.shoelog.domain.Shoe
import ai.orkk.shoelog.ui.EmptyState
import ai.orkk.shoelog.ui.ExerciseRow
import ai.orkk.shoelog.ui.LoadingState
import ai.orkk.shoelog.ui.ShoeMileageCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val isLoading: Boolean = true,
    val isSyncing: Boolean = false,
    val shoes: List<Shoe> = emptyList(),
    val recentExercises: List<Exercise> = emptyList(),
    val unassignedCount: Int = 0,
    val availability: HealthConnectAvailability = HealthConnectAvailability.AVAILABLE,
    val syncMessage: String? = null,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val syncStatus = MutableStateFlow<Pair<Boolean, String?>>(false to null)

    val state = combine(
        container.shoeRepository.observeShoes(),
        container.exerciseRepository.observeExercises(),
        container.exerciseRepository.observeUnassignedCount(),
        syncStatus,
    ) { shoes, exercises, unassignedCount, sync ->
        HomeUiState(
            isLoading = false,
            isSyncing = sync.first,
            shoes = shoes,
            recentExercises = exercises.take(3),
            unassignedCount = unassignedCount,
            availability = container.healthConnectDataSource.availability(),
            syncMessage = sync.second,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        refresh()
    }

    fun refresh() {
        if (syncStatus.value.first) return
        viewModelScope.launch {
            syncStatus.value = true to null
            val message = when (val result = container.syncRepository.sync()) {
                is SyncResult.Success -> if (result.newExerciseIds.isEmpty()) {
                    "새 달리기 기록이 없습니다."
                } else {
                    "새 달리기 ${result.newExerciseIds.size}개를 가져왔습니다."
                }
                is SyncResult.PermissionRequired -> "Health Connect 읽기 권한이 필요합니다."
                SyncResult.UpdateRequired -> "Health Connect 업데이트가 필요합니다."
                SyncResult.Unsupported -> "이 기기에서는 Health Connect를 사용할 수 없습니다."
                is SyncResult.Failed -> result.userMessage
            }
            syncStatus.value = false to message
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T = HomeViewModel(container) as T
            }
    }
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onRefresh: () -> Unit,
    onAddShoe: () -> Unit,
    onOpenShoe: (Long) -> Unit,
    onOpenExercises: () -> Unit,
) {
    if (state.isLoading) {
        LoadingState(modifier = Modifier.fillMaxSize(), tag = "home_loading")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().testTag("home_content"),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("ShoeLog", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                    Text("러닝화 마일리지를 한눈에")
                }
                OutlinedButton(onClick = onRefresh, enabled = !state.isSyncing) {
                    Text(if (state.isSyncing) "동기화 중" else "새로고침")
                }
            }
        }
        item { HealthConnectionCard(state.availability, state.syncMessage) }
        if (state.unassignedCount > 0) {
            item {
                Button(onClick = onOpenExercises, modifier = Modifier.fillMaxWidth()) {
                    Text("미배정 달리기 ${state.unassignedCount}개에 러닝화 배정")
                }
            }
        }
        if (state.shoes.isEmpty() && state.recentExercises.isEmpty()) {
            item {
                EmptyState(
                    title = "아직 러닝화가 없습니다",
                    message = "첫 러닝화를 등록하거나 설정에서 샘플 데이터를 살펴보세요.",
                    actionLabel = "러닝화 추가",
                    onAction = onAddShoe,
                    tag = "home_empty",
                )
            }
        } else {
            if (state.shoes.isNotEmpty()) {
                item { Text("내 러닝화", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(state.shoes, key = { it.id }) { shoe ->
                    ShoeMileageCard(shoe = shoe, onClick = { onOpenShoe(shoe.id) })
                }
            }
            if (state.recentExercises.isNotEmpty()) {
                item { Text("최근 달리기", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) }
                items(state.recentExercises, key = { it.id }) { exercise ->
                    ExerciseRow(exercise = exercise, onClick = onOpenExercises)
                }
            }
        }
    }
}

@Composable
private fun HealthConnectionCard(availability: HealthConnectAvailability, message: String?) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Health Connect", fontWeight = FontWeight.Bold)
            Text(
                when (availability) {
                    HealthConnectAvailability.AVAILABLE -> "연결 가능 · 운동과 거리만 읽습니다"
                    HealthConnectAvailability.UPDATE_REQUIRED -> "설치 또는 업데이트가 필요합니다"
                    HealthConnectAvailability.UNSUPPORTED -> "이 기기에서는 지원되지 않습니다"
                },
            )
            message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
    }
}
