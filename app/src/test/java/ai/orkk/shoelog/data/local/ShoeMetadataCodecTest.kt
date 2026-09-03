package ai.orkk.shoelog.data.local

import ai.orkk.shoelog.domain.ShoePurpose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShoeMetadataCodecTest {
    @Test
    fun purposesRoundTripInStableEnumOrderWithoutDuplicates() {
        val encoded = ShoeMetadataCodec.encodePurposes(
            linkedSetOf(ShoePurpose.RACE, ShoePurpose.DAILY, ShoePurpose.RACE),
        )

        assertEquals("daily,race", encoded)
        assertEquals(
            setOf(ShoePurpose.DAILY, ShoePurpose.RACE),
            ShoeMetadataCodec.decodePurposes(encoded),
        )
    }

    @Test
    fun unknownCodesAreIgnoredForForwardCompatibility() {
        assertEquals(
            setOf(ShoePurpose.LSD),
            ShoeMetadataCodec.decodePurposes(" future , lsd , future "),
        )
        assertNull(ShoeMetadataCodec.decodeCategory("future-category"))
    }

    @Test
    fun nonPlateUsesTheCorrectStableCodeAndLabel() {
        val category = ShoeMetadataCodec.decodeCategory("non-plate")

        assertEquals("논플레이트", category?.displayName)
        assertNull(ShoeMetadataCodec.decodeCategory("high-plate"))
    }
}
