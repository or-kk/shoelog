package ai.orkk.shoelog.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ai.orkk.shoelog.domain.Exercise
import ai.orkk.shoelog.domain.MileageStatus
import ai.orkk.shoelog.domain.Shoe
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

fun formatDistance(meters: Long?): String = meters
    ?.let { String.format(Locale.KOREA, "%.1f km", it.coerceAtLeast(0) / 1_000.0) }
    ?: "거리 정보 없음"

fun formatRunDate(exercise: Exercise): String = exercise.startTime
    .atZone(ZoneId.systemDefault())
    .format(DateTimeFormatter.ofPattern("M월 d일 HH:mm", Locale.KOREA))

@Composable
fun LoadingState(modifier: Modifier = Modifier, tag: String = "loading") {
    Box(modifier = modifier.fillMaxWidth().testTag(tag).padding(48.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = "불러오는 중" })
    }
}

@Composable
fun EmptyState(
    title: String,
    message: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    tag: String = "empty",
) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag(tag).padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (actionLabel != null && onAction != null) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAction, modifier = Modifier.height(48.dp)) { Text(actionLabel) }
        }
    }
}

@Composable
fun ShoeMileageCard(shoe: Shoe, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .semantics { contentDescription = "${shoe.brand} ${shoe.model} 신발 사진 자리" },
                contentAlignment = Alignment.Center,
            ) {
                val photo by rememberLocalPhoto(shoe.photoUri)
                if (photo == null) {
                    Text("👟", style = MaterialTheme.typography.headlineMedium)
                } else {
                    Image(
                        bitmap = requireNotNull(photo),
                        contentDescription = "${shoe.brand} ${shoe.model} 신발 사진",
                        modifier = Modifier.size(64.dp),
                        contentScale = ContentScale.Crop,
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(shoe.nickname.ifBlank { shoe.model }, fontWeight = FontWeight.Bold)
                        Text("${shoe.brand} · ${shoe.model}", style = MaterialTheme.typography.bodySmall)
                    }
                    if (shoe.defaultShoe) Text("기본", color = MaterialTheme.colorScheme.primary)
                }
                Text("${formatDistance(shoe.mileage.totalMeters)} / ${formatDistance(shoe.targetMeters)}")
                LinearProgressIndicator(
                    progress = { shoe.mileage.progress },
                    modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
                )
                val statusText = when (shoe.mileage.status) {
                    MileageStatus.NORMAL -> "남은 거리 ${formatDistance(shoe.mileage.remainingMeters)}"
                    MileageStatus.NEAR_TARGET -> "교체 목표에 가까워졌어요"
                    MileageStatus.TARGET_REACHED -> "교체 목표에 도달했어요"
                }
                Text(statusText, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun rememberLocalPhoto(uriString: String?): androidx.compose.runtime.State<ImageBitmap?> {
    val context = LocalContext.current
    return produceState<ImageBitmap?>(initialValue = null, key1 = uriString) {
        value = if (uriString == null) null else withContext(Dispatchers.IO) {
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(uriString)).use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }.getOrNull()
        }
    }
}

@Composable
fun ExerciseRow(exercise: Exercise, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(formatRunDate(exercise), fontWeight = FontWeight.SemiBold)
            Text(exercise.sourceName, style = MaterialTheme.typography.bodySmall)
            Text(exercise.assignedShoeLabel ?: "러닝화 미배정", style = MaterialTheme.typography.labelMedium)
        }
        Text(formatDistance(exercise.distanceMeters), fontWeight = FontWeight.Bold)
    }
}
