package ai.orkk.shoelog

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildFoundationTest {
    @Test
    fun appIdentityIsStable() {
        assertEquals("ai.orkk.shoelog", BuildConfig.APPLICATION_ID)
    }

    @Test
    fun manifestRegistersAndroid14HealthPermissionRationale() {
        val manifest = File("src/main/AndroidManifest.xml").readText()

        assertTrue("android.intent.action.VIEW_PERMISSION_USAGE" in manifest)
        assertTrue("android.intent.category.HEALTH_PERMISSIONS" in manifest)
        assertTrue("android.permission.START_VIEW_PERMISSION_USAGE" in manifest)
        assertTrue("androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE" in manifest)
        assertTrue("android:targetActivity=\".PermissionsRationaleActivity\"" in manifest)
        assertFalse("android.health.connect.action.SHOW_PERMISSIONS_RATIONALE" in manifest)
    }
}
