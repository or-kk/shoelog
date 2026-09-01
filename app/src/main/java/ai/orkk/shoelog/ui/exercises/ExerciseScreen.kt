package ai.orkk.shoelog.ui.exercises

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ai.orkk.shoelog.data.repository.ExerciseRepository
import ai.orkk.shoelog.data.repository.ShoeRepository
import ai.orkk.shoelog.domain.Exercise
import ai.orkk.shoelog.domain.Shoe
import ai.orkk.shoelog.ui.EmptyState
import ai.orkk.shoelog.ui.ExerciseRow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun filterExercises(exercises: List<Exercise>, unassignedOnly: Boolean): List<Exercise> =
    if (unassignedOnly) exercises.filter { it.assignedShoeId == null } else exercises

data class ExerciseListUiState(
    val exercises: List<Exercise> = emptyList(),
    val shoes: List<Shoe> = emptyList(),
    val unassignedOnly: Boolean = false,
    val selectedExerciseId: String? = null,
    val message: String? = null,
)

class ExerciseListViewModel(
    private val exerciseRepository: ExerciseRepository,
    shoeRepository: ShoeRepository,
    initiallySelectedId: String?,
) : ViewModel() {
    private val controls = MutableStateFlow(Controls(selectedId = initiallySelectedId))

    val state = combine(
        exerciseRepository.observeExercises(),
        shoeRepository.observeShoes(),
        controls,
    ) { exercises, shoes, controls ->
        ExerciseListUiState(
            exercises = filterExercises(exercises, controls.unassignedOnly),
            shoes = shoes.filterNot { it.retired },
            unassignedOnly = controls.unassignedOnly,
            selectedExerciseId = controls.selectedId,
            message = controls.message,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExerciseListUiState())

    fun setUnassignedOnly(enabled: Boolean) {
        controls.value = controls.value.copy(unassignedOnly = enabled)
    }

    fun select(exerciseId: String?) {
        controls.value = controls.value.copy(selectedId = exerciseId, message = null)
    }

    fun assign(shoeId: Long?) {
        val exerciseId = controls.value.selectedId ?: return
        viewModelScope.launch {
            runCatching { exerciseRepository.assign(exerciseId, shoeId) }
                .onSuccess { controls.value = controls.value.copy(selectedId = null, message = "러닝화 배정을 변경했습니다.") }
                .onFailure { controls.value = controls.value.copy(message = it.message ?: "배정을 변경하지 못했습니다.") }
        }
    }

    private data class Controls(
        val unassignedOnly: Boolean = false,
        val selectedId: String? = null,
        val message: String? = null,
    )

    companion object {
        fun factory(
            exerciseRepository: ExerciseRepository,
            shoeRepository: ShoeRepository,
            selectedId: String?,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                ExerciseListViewModel(exerciseRepository, shoeRepository, selectedId) as T
        }
    }
}

@Composable
fun ExerciseListScreen(
    state: ExerciseListUiState,
    onUnassignedOnlyChange: (Boolean) -> Unit,
    onSelect: (String?) -> Unit,
    onAssign: (Long?) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("달리기 기록", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text("운동 하나에 러닝화 한 켤레를 배정합니다.")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !state.unassignedOnly,
                    onClick = { onUnassignedOnlyChange(false) },
                    label = { Text("전체") },
                )
                FilterChip(
                    selected = state.unassignedOnly,
                    onClick = { onUnassignedOnlyChange(true) },
                    label = { Text("미배정") },
                )
            }
            state.message?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
        }
        if (state.exercises.isEmpty()) {
            EmptyState(
                title = if (state.unassignedOnly) "미배정 달리기가 없습니다" else "달리기 기록이 없습니다",
                message = if (state.unassignedOnly) "모든 달리기에 러닝화가 배정되었습니다." else "Health Connect 동기화 후 기록이 여기에 표시됩니다.",
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
            ) {
                items(state.exercises, key = { it.id }) { exercise ->
                    ExerciseRow(exercise, onClick = { onSelect(exercise.id) })
                }
            }
        }
    }

    val selected = state.exercises.firstOrNull { it.id == state.selectedExerciseId }
        ?: state.selectedExerciseId?.let { id -> state.exercises.find { it.id == id } }
    if (state.selectedExerciseId != null) {
        AssignmentDialog(
            exercise = selected,
            shoes = state.shoes,
            onAssign = onAssign,
            onDismiss = { onSelect(null) },
        )
    }
}

@Composable
private fun AssignmentDialog(
    exercise: Exercise?,
    shoes: List<Shoe>,
    onAssign: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("러닝화 배정") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                exercise?.let { Text("${ai.orkk.shoelog.ui.formatRunDate(it)} · ${ai.orkk.shoelog.ui.formatDistance(it.distanceMeters)}") }
                if (shoes.isEmpty()) Text("활성 러닝화를 먼저 등록해 주세요.")
                shoes.forEach { shoe ->
                    TextButton(onClick = { onAssign(shoe.id) }, modifier = Modifier.fillMaxWidth()) {
                        Text(shoe.nickname.ifBlank { "${shoe.brand} ${shoe.model}" })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onAssign(null) }) { Text("배정 해제") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}
