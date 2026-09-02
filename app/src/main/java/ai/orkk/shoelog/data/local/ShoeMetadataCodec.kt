package ai.orkk.shoelog.data.local

import ai.orkk.shoelog.domain.ShoeCategory
import ai.orkk.shoelog.domain.ShoePurpose

object ShoeMetadataCodec {
    private val categoriesByCode = ShoeCategory.entries.associateBy(ShoeCategory::code)
    private val purposesByCode = ShoePurpose.entries.associateBy(ShoePurpose::code)

    fun decodeCategory(code: String?): ShoeCategory? = code
        ?.trim()
        ?.takeIf(String::isNotEmpty)
        ?.let(categoriesByCode::get)

    fun encodePurposes(purposes: Set<ShoePurpose>): String = ShoePurpose.entries
        .filter(purposes::contains)
        .joinToString(",", transform = ShoePurpose::code)

    fun decodePurposes(codes: String): Set<ShoePurpose> = codes
        .split(',')
        .asSequence()
        .map(String::trim)
        .mapNotNull(purposesByCode::get)
        .toSet()
}
