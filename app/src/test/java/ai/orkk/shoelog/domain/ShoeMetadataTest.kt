package ai.orkk.shoelog.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ShoeMetadataTest {
    @Test
    fun everyCategoryBelongsToTheApprovedGroup() {
        assertEquals(
            setOf("입문화", "맥스 쿠션화", "안정화", "올라운더", "경량 트레이너"),
            ShoeCategory.entries
                .filter { it.group == ShoeCategoryGroup.DAILY }
                .map { it.displayName }
                .toSet(),
        )
        assertEquals(
            setOf("논플레이트", "라이트 플레이트", "카본 플레이트"),
            ShoeCategory.entries
                .filter { it.group == ShoeCategoryGroup.SUPER_TRAINER }
                .map { it.displayName }
                .toSet(),
        )
        assertEquals(
            setOf("중거리", "장거리"),
            ShoeCategory.entries
                .filter { it.group == ShoeCategoryGroup.RACING }
                .map { it.displayName }
                .toSet(),
        )
        assertEquals(10, ShoeCategory.entries.size)
        assertEquals(7, ShoePurpose.entries.size)
    }
}
