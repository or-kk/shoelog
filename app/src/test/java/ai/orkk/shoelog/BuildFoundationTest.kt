package ai.orkk.shoelog

import org.junit.Assert.assertEquals
import org.junit.Test

class BuildFoundationTest {
    @Test
    fun appIdentityIsStable() {
        assertEquals("ai.orkk.shoelog", BuildConfig.APPLICATION_ID)
    }
}
