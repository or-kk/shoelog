@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ai.orkk.shoelog.ui.shoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ai.orkk.shoelog.domain.Exercise
import ai.orkk.shoelog.domain.Shoe
import ai.orkk.shoelog.ui.ExerciseRow
import ai.orkk.shoelog.ui.LoadingState
import ai.orkk.shoelog.ui.ShoeMileageCard
import ai.orkk.shoelog.ui.formatDistance

@Composable
fun ShoeDetailScreen(
    shoe: Shoe?,
    exercises: List<Exercise>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRetireToggle: () -> Unit,
    onDelete: () -> Unit,
    deleteMessage: String? = null,
) {
    var showDeleteConfirmation by remember(shoe?.id) { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(shoe?.nickname?.ifBlank { shoe.model } ?: "러닝화 상세") },
                navigationIcon = { TextButton(onClick = onBack) { Text("뒤로") } },
                actions = { TextButton(onClick = onEdit, enabled = shoe != null) { Text("수정") } },
            )
        },
    ) { padding ->
        if (shoe == null) {
            LoadingState(modifier = Modifier.padding(padding))
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { ShoeMileageCard(shoe, onClick = {}) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("누적 ${formatDistance(shoe.mileage.totalMeters)}", style = MaterialTheme.typography.titleSmall)
                    Text("목표 ${formatDistance(shoe.targetMeters)}")
                    Text("남은 거리 ${formatDistance(shoe.mileage.remainingMeters)}")
                    Text("색상 ${shoe.color.ifBlank { "미입력" }}")
                    Text("구매일 ${shoe.purchaseDate ?: "미입력"}")
                    Text("사용 시작일 ${shoe.startDate ?: "미입력"}")
                    Text("구매가격 ${formatWon(shoe.purchasePriceWon)}")
                    Text("정가 ${formatWon(shoe.listPriceWon)}")
                }
            }
            item {
                OutlinedButton(onClick = onRetireToggle, modifier = Modifier.fillMaxWidth()) {
                    Text(if (shoe.retired) "다시 활성화" else "은퇴 처리")
                }
            }
            item {
                OutlinedButton(
                    onClick = { showDeleteConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("삭제") }
            }
            deleteMessage?.let { message ->
                item { Text(message, color = MaterialTheme.colorScheme.error) }
            }
            item { Text("이 신발을 착용한 달리기", style = MaterialTheme.typography.titleMedium) }
            if (exercises.isEmpty()) item { Text("아직 배정된 달리기가 없습니다.") }
            else items(exercises, key = { it.id }) { ExerciseRow(it, onClick = {}) }
        }
    }

    if (showDeleteConfirmation && shoe != null) {
        DeleteShoeDialog(
            shoe = shoe,
            onConfirm = {
                showDeleteConfirmation = false
                onDelete()
            },
            onDismiss = { showDeleteConfirmation = false },
        )
    }
}
