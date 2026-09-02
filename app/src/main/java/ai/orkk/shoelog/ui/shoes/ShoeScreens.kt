@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package ai.orkk.shoelog.ui.shoes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.orkk.shoelog.domain.Exercise
import ai.orkk.shoelog.domain.Shoe
import ai.orkk.shoelog.ui.EmptyState
import ai.orkk.shoelog.ui.ExerciseRow
import ai.orkk.shoelog.ui.LoadingState
import ai.orkk.shoelog.ui.ShoeMileageCard
import ai.orkk.shoelog.ui.formatDistance

@Composable
fun ShoeListScreen(
    shoes: List<Shoe>,
    includeRetired: Boolean,
    onIncludeRetiredChange: (Boolean) -> Unit,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("러닝화") }, actions = { TextButton(onClick = onAdd) { Text("추가") } }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("은퇴한 신발 포함")
                    Switch(checked = includeRetired, onCheckedChange = onIncludeRetiredChange)
                }
            }
            if (shoes.isEmpty()) {
                item { EmptyState("등록된 러닝화가 없습니다", "러닝화를 추가해 마일리지를 관리하세요.", "러닝화 추가", onAdd) }
            } else {
                items(shoes, key = { it.id }) { ShoeMileageCard(it, onClick = { onOpen(it.id) }) }
            }
        }
    }
}

@Composable
fun ShoeDetailScreen(
    shoe: Shoe?,
    exercises: List<Exercise>,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onRetireToggle: () -> Unit,
) {
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
                    Text("누적 ${formatDistance(shoe.mileage.totalMeters)}", fontWeight = FontWeight.Bold)
                    Text("목표 ${formatDistance(shoe.targetMeters)}")
                    Text("남은 거리 ${formatDistance(shoe.mileage.remainingMeters)}")
                    Text("색상 ${shoe.color.ifBlank { "미입력" }}")
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
            item { Text("이 신발을 착용한 달리기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            if (exercises.isEmpty()) item { Text("아직 배정된 달리기가 없습니다.") }
            else items(exercises, key = { it.id }) { ExerciseRow(it, onClick = {}) }
        }
    }
}
