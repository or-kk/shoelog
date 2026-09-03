package ai.orkk.shoelog.domain

enum class ShoeCategoryGroup(val displayName: String) {
    DAILY("데일리"),
    SUPER_TRAINER("슈퍼 트레이너"),
    RACING("레이싱"),
}

enum class ShoeCategory(
    val code: String,
    val group: ShoeCategoryGroup,
    val displayName: String,
) {
    ENTRY("entry", ShoeCategoryGroup.DAILY, "입문화"),
    MAX_CUSHION("max-cushion", ShoeCategoryGroup.DAILY, "맥스 쿠션화"),
    STABILITY("stability", ShoeCategoryGroup.DAILY, "안정화"),
    ALL_ROUNDER("all-rounder", ShoeCategoryGroup.DAILY, "올라운더"),
    LIGHTWEIGHT_TRAINER("lightweight-trainer", ShoeCategoryGroup.DAILY, "경량 트레이너"),
    NON_PLATE("non-plate", ShoeCategoryGroup.SUPER_TRAINER, "논플레이트"),
    LIGHT_PLATE("light-plate", ShoeCategoryGroup.SUPER_TRAINER, "라이트 플레이트"),
    CARBON_PLATE("carbon-plate", ShoeCategoryGroup.SUPER_TRAINER, "카본 플레이트"),
    MIDDLE_DISTANCE("middle-distance", ShoeCategoryGroup.RACING, "중거리"),
    LONG_DISTANCE("long-distance", ShoeCategoryGroup.RACING, "장거리"),
}

enum class ShoePurpose(val code: String, val displayName: String) {
    DAILY("daily", "데일리"),
    RECOVERY("recovery", "회복주"),
    LSD("lsd", "LSD"),
    TEMPO("tempo", "템포런"),
    INTERVAL("interval", "인터벌"),
    RACE("race", "대회"),
    TREADMILL("treadmill", "트레드밀"),
}
