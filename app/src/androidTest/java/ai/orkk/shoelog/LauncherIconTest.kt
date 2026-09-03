package ai.orkk.shoelog

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherIconTest {
    @Test
    fun launcherIconUsesShoeLogLimeAndDarkPalette() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val icon = context.packageManager.getApplicationIcon(context.packageName)
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)

        icon.setBounds(0, 0, bitmap.width, bitmap.height)
        icon.draw(Canvas(bitmap))

        var limePixels = 0
        var darkPixels = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val color = bitmap.getPixel(x, y)
                if (color.red in 160..210 && color.green in 220..255 && color.blue in 40..110) {
                    limePixels++
                }
                if (color.red < 60 && color.green < 70 && color.blue < 60) {
                    darkPixels++
                }
            }
        }

        val pixelCount = bitmap.width * bitmap.height
        assertTrue("launcher icon should visibly use ShoeLog lime", limePixels > pixelCount * 0.15)
        assertTrue("launcher icon should visibly use ShoeLog dark", darkPixels > pixelCount * 0.03)
    }
}
