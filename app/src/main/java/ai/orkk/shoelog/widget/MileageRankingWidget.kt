package ai.orkk.shoelog.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import ai.orkk.shoelog.MainActivity
import ai.orkk.shoelog.ShoeLogApplication
import ai.orkk.shoelog.ui.shoes.ShoeSortKey
import ai.orkk.shoelog.ui.shoes.SortDirection
import ai.orkk.shoelog.ui.shoes.sortShoes
import java.util.Locale
import kotlinx.coroutines.flow.first

data class MileageRankingEntry(
    val rank: Int,
    val shoeId: Long,
    val name: String,
    val totalMeters: Long,
    val progress: Float,
)

class MileageRankingWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entries = runCatching {
            val repository = (context.applicationContext as ShoeLogApplication).container.shoeRepository
            sortShoes(
                shoes = repository.observeShoes().first(),
                key = ShoeSortKey.MILEAGE,
                direction = SortDirection.DESCENDING,
            ).take(MAX_RANKING_SIZE).mapIndexed { index, shoe ->
                MileageRankingEntry(
                    rank = index + 1,
                    shoeId = shoe.id,
                    name = shoe.nickname.ifBlank { "${shoe.brand} ${shoe.model}".trim() },
                    totalMeters = shoe.mileage.totalMeters,
                    progress = shoe.mileage.progress.coerceIn(0f, 1f),
                )
            }
        }.getOrDefault(emptyList())

        provideContent {
            MileageRankingContent(
                entries = entries,
                openAction = actionStartActivity(
                    Intent(context, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_OPEN_SHOE_MANAGEMENT, true),
                ),
            )
        }
    }

    private companion object {
        const val MAX_RANKING_SIZE = 3
    }
}

class MileageRankingWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = MileageRankingWidget()
}

@Composable
internal fun MileageRankingContent(
    entries: List<MileageRankingEntry>,
    openAction: Action? = null,
) {
    val widgetModifier = GlanceModifier
        .fillMaxSize()
        .background(ColorProvider(WIDGET_BACKGROUND))
        .appWidgetBackground()
        .padding(horizontal = 16.dp, vertical = 12.dp)
        .let { modifier -> openAction?.let(modifier::clickable) ?: modifier }
    Column(
        modifier = widgetModifier,
    ) {
        Text(
            text = "마일리지 랭킹",
            style = TextStyle(
                color = ColorProvider(WIDGET_FOREGROUND),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
            ),
        )
        Spacer(GlanceModifier.height(8.dp))

        if (entries.isEmpty()) {
            Text(
                text = "러닝화를 등록하면 마일리지 순위가 표시됩니다",
                modifier = GlanceModifier.semantics { testTag = "empty-ranking" },
                style = TextStyle(
                    color = ColorProvider(WIDGET_FOREGROUND),
                    fontSize = 13.sp,
                ),
            )
        } else {
            entries.take(3).forEach { entry ->
                RankingRow(entry)
            }
        }
    }
}

@Composable
private fun RankingRow(entry: MileageRankingEntry) {
    Column(
        modifier = GlanceModifier
            .fillMaxWidth()
            .semantics { testTag = "ranking-row" },
    ) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.rank.toString(),
                modifier = GlanceModifier
                    .width(22.dp)
                    .semantics { testTag = "rank-${entry.rank}" },
                style = TextStyle(
                    color = ColorProvider(WIDGET_FOREGROUND),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                text = entry.name,
                modifier = GlanceModifier
                    .defaultWeight()
                    .semantics { testTag = "shoe-${entry.rank}" },
                maxLines = 1,
                style = TextStyle(
                    color = ColorProvider(WIDGET_FOREGROUND),
                    fontSize = 13.sp,
                ),
            )
            Spacer(GlanceModifier.width(8.dp))
            Text(
                text = formatDistance(entry.totalMeters),
                modifier = GlanceModifier.semantics { testTag = "distance-${entry.rank}" },
                style = TextStyle(
                    color = ColorProvider(WIDGET_FOREGROUND),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
        LinearProgressIndicator(
            progress = entry.progress,
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(3.dp),
            color = ColorProvider(WIDGET_FOREGROUND),
            backgroundColor = ColorProvider(WIDGET_PROGRESS_BACKGROUND),
        )
        Spacer(GlanceModifier.height(6.dp))
    }
}

private fun formatDistance(totalMeters: Long): String =
    String.format(Locale.KOREA, "%.1f km", totalMeters / 1_000.0)

private val WIDGET_BACKGROUND = Color(0xFFB8F34A)
private val WIDGET_FOREGROUND = Color(0xFF11130F)
private val WIDGET_PROGRESS_BACKGROUND = Color(0xFF96C73B)
