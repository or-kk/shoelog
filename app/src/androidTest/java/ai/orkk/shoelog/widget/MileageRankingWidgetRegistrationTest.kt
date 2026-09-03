package ai.orkk.shoelog.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MileageRankingWidgetRegistrationTest {
    @Test
    fun installedAppOffersMileageRankingWidgetToTheLauncher() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val providerNames = AppWidgetManager.getInstance(context)
            .installedProviders
            .filter { it.provider.packageName == context.packageName }
            .map { it.provider.className }

        assertTrue(
            "ShoeLog should register its mileage ranking widget provider",
            providerNames.contains("ai.orkk.shoelog.widget.MileageRankingWidgetReceiver"),
        )
    }
}
