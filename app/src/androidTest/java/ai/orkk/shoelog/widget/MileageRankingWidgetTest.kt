package ai.orkk.shoelog.widget

import android.widget.FrameLayout
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.ExperimentalGlanceRemoteViewsApi
import androidx.glance.appwidget.GlanceRemoteViews
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.assertHasText
import androidx.glance.testing.unit.hasTestTag
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Test

class MileageRankingWidgetTest {
    @Test
    fun contentShowsOnlyTheTopThreeMileageEntries() = runGlanceAppWidgetUnitTest {
        provideComposable {
            MileageRankingContent(
                entries = listOf(
                    entry(1, "Pegasus 41", 321_000),
                    entry(2, "Novablast 5", 210_500),
                    entry(3, "Deviate Nitro 3", 99_900),
                    entry(4, "Adizero SL2", 81_000),
                ),
            )
        }

        onNode(hasTestTag("rank-1")).assertHasText("1")
        onNode(hasTestTag("shoe-1")).assertHasText("Pegasus 41")
        onNode(hasTestTag("distance-1")).assertHasText("321.0 km")
        onNode(hasTestTag("shoe-3")).assertHasText("Deviate Nitro 3")
        onAllNodes(hasTestTag("ranking-row")).assertCountEquals(3)
    }

    @Test
    fun contentExplainsWhenNoShoesAreRegistered() = runGlanceAppWidgetUnitTest {
        provideComposable {
            MileageRankingContent(entries = emptyList())
        }

        onNode(hasTestTag("empty-ranking"))
            .assertHasText("러닝화를 등록하면 마일리지 순위가 표시됩니다")
    }

    @OptIn(ExperimentalGlanceRemoteViewsApi::class)
    @Test
    fun generatedRemoteViewsCanBeInflatedByAWidgetHost() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val result = GlanceRemoteViews().compose(
            context = context,
            size = DpSize(250.dp, 110.dp),
        ) {
            MileageRankingContent(entries = emptyList())
        }

        result.remoteViews.apply(context, FrameLayout(context))
        Unit
    }

    private fun entry(rank: Int, name: String, meters: Long) = MileageRankingEntry(
        rank = rank,
        shoeId = rank.toLong(),
        name = name,
        totalMeters = meters,
        progress = meters / 500_000f,
    )
}
