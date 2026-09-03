# ShoeLog Shoe Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add one running-shoe category, multiple purpose chips, a sortable management tab, editing, and assignment-safe deletion without losing existing local data.

**Architecture:** Keep the existing domain/data/Compose separation. Persist stable category and purpose codes in two added Room columns, expose decoded enums through `Shoe`, and move management state into a dedicated ViewModel that owns filtering, sorting, and deletion feedback. Keep SQL focused on persistence and assignment-safe deletion while performing the small personal shoe-list sort in Kotlin.

**Tech Stack:** Kotlin, Coroutines Flow, Jetpack Compose Material 3, Navigation Compose, Room 2.x, Preferences DataStore, JUnit 4, AndroidX Room migration testing, Compose UI testing, Gradle 9.4.1, JDK 17

**Spec:** `docs/superpowers/specs/2026-09-03-shoe-management-design.md`

## Global Constraints

- Room must migrate from version 2 to version 3 without deleting shoes, exercises, assignments, prices, or settings.
- A shoe has zero or one category and zero or more purposes from the fixed approved lists.
- Purpose selection is multi-select; category selection is single-select.
- Assigned shoes cannot be permanently deleted; the user is directed to retire the shoe or unassign its runs first.
- Nullable sort values always appear last for both ascending and descending order.
- Instrumentation tests run only on an Android emulator. Never run `connectedDebugAndroidTest` against the physical phone.
- Physical-phone verification uses only ordinary `adb install -r` and manual launch checks.
- Do not add real health data, personal settings, signing keys, `local.properties`, databases, APKs, or AABs to Git.

---

## Planned File Structure

- Create `app/src/main/java/ai/orkk/shoelog/domain/ShoeMetadata.kt`: approved category groups, category codes, and purpose codes.
- Create `app/src/main/java/ai/orkk/shoelog/data/local/ShoeMetadataCodec.kt`: deterministic Room string encoding and defensive decoding.
- Modify `app/src/main/java/ai/orkk/shoelog/domain/Models.kt`: add decoded category and purpose values to `Shoe`.
- Modify `app/src/main/java/ai/orkk/shoelog/data/local/Entities.kt`: add `categoryCode` and `purposeCodes` columns.
- Modify `app/src/main/java/ai/orkk/shoelog/data/local/Migrations.kt`: add `MIGRATION_2_3`.
- Modify `app/src/main/java/ai/orkk/shoelog/data/local/ShoeLogDatabase.kt`: raise the schema version to 3.
- Modify `app/src/main/java/ai/orkk/shoelog/data/local/ShoeLogDao.kt`: provide transactional assignment-safe deletion.
- Modify `app/src/main/java/ai/orkk/shoelog/data/repository/ShoeRepository.kt`: map metadata, return explicit deletion results, and clear a matching default preference.
- Modify `app/src/main/java/ai/orkk/shoelog/data/preferences/SettingsRepository.kt`: implement the narrow default-shoe preference clearing contract.
- Modify `app/src/main/java/ai/orkk/shoelog/ShoeLogApplication.kt`: register migration 2→3 and inject settings cleanup into the shoe repository.
- Create `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeEditor.kt`: editor form, ViewModel, category selector, purpose chips, and editor screen extracted from the existing combined file.
- Create `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeManagement.kt`: sort model, comparator, management ViewModel, list screen, menus, and delete dialogs.
- Create `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeDetail.kt`: detail display, metadata chips, retirement action, and deletion entry point.
- Modify `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeScreens.kt`: remove moved declarations; delete the file if no shared declarations remain.
- Modify `app/src/main/java/ai/orkk/shoelog/ui/Components.kt`: show compact category and purpose metadata on reusable shoe cards.
- Modify `app/src/main/java/ai/orkk/shoelog/ui/ShoeLogApp.kt`: add the bottom navigation destination and wire management/detail deletion state.
- Add focused unit, migration, DAO, and Compose tests alongside the existing test suites.
- Modify `README.md`: document metadata, management, sorting, and the safe-delete rule.

---

### Task 1: Domain Metadata and Deterministic Storage

**Files:**
- Create: `app/src/main/java/ai/orkk/shoelog/domain/ShoeMetadata.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/data/local/ShoeMetadataCodec.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/domain/Models.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/domain/ShoeMetadataTest.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/data/local/ShoeMetadataCodecTest.kt`

**Interfaces:**
- Produces: `ShoeCategoryGroup(displayName: String)`.
- Produces: `ShoeCategory(code: String, group: ShoeCategoryGroup, displayName: String)` with ten approved categories.
- Produces: `ShoePurpose(code: String, displayName: String)` with seven approved purposes.
- Produces: `ShoeMetadataCodec.encodePurposes(Set<ShoePurpose>): String`.
- Produces: `ShoeMetadataCodec.decodePurposes(String): Set<ShoePurpose>`.
- Produces: `ShoeMetadataCodec.decodeCategory(String?): ShoeCategory?`.

- [ ] **Step 1: Write failing taxonomy and codec tests**

```kotlin
class ShoeMetadataTest {
    @Test fun everyCategoryBelongsToTheApprovedGroup() {
        assertEquals(
            setOf("입문화", "맥스 쿠션화", "안정화", "올라운더", "경량 트레이너"),
            ShoeCategory.entries.filter { it.group == ShoeCategoryGroup.DAILY }.map { it.displayName }.toSet(),
        )
        assertEquals(10, ShoeCategory.entries.size)
        assertEquals(7, ShoePurpose.entries.size)
    }
}

class ShoeMetadataCodecTest {
    @Test fun purposesRoundTripInStableEnumOrderWithoutDuplicates() {
        val encoded = ShoeMetadataCodec.encodePurposes(
            linkedSetOf(ShoePurpose.RACE, ShoePurpose.DAILY, ShoePurpose.RACE),
        )
        assertEquals("daily,race", encoded)
        assertEquals(setOf(ShoePurpose.DAILY, ShoePurpose.RACE), ShoeMetadataCodec.decodePurposes(encoded))
    }

    @Test fun unknownCodesAreIgnoredForForwardCompatibility() {
        assertEquals(setOf(ShoePurpose.LSD), ShoeMetadataCodec.decodePurposes("future,lsd"))
        assertNull(ShoeMetadataCodec.decodeCategory("future-category"))
    }
}
```

- [ ] **Step 2: Run the focused tests and confirm they fail because the metadata types do not exist**

Run: `./gradlew testDebugUnitTest --tests '*ShoeMetadataTest' --tests '*ShoeMetadataCodecTest'`

Expected: compilation failure naming `ShoeCategory`, `ShoePurpose`, or `ShoeMetadataCodec`.

- [ ] **Step 3: Implement the approved enums and codec**

```kotlin
enum class ShoeCategoryGroup(val displayName: String) {
    DAILY("데일리"), SUPER_TRAINER("슈퍼 트레이너"), RACING("레이싱")
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
    DAILY("daily", "데일리"), RECOVERY("recovery", "회복주"), LSD("lsd", "LSD"),
    TEMPO("tempo", "템포런"), INTERVAL("interval", "인터벌"), RACE("race", "대회"),
    TREADMILL("treadmill", "트레드밀"),
}
```

Implement codec lookup maps by `code`, encode in `ShoePurpose.entries` order, trim tokens, discard duplicates, and ignore unknown values.

- [ ] **Step 4: Add nullable category and an empty purpose set to `Shoe`**

```kotlin
data class Shoe(
    // existing properties
    val category: ShoeCategory? = null,
    val purposes: Set<ShoePurpose> = emptySet(),
    // existing timestamps and mileage
)
```

- [ ] **Step 5: Run the focused tests**

Run: `./gradlew testDebugUnitTest --tests '*ShoeMetadataTest' --tests '*ShoeMetadataCodecTest'`

Expected: PASS.

- [ ] **Step 6: Commit the domain and codec slice**

```bash
git add app/src/main/java/ai/orkk/shoelog/domain app/src/main/java/ai/orkk/shoelog/data/local/ShoeMetadataCodec.kt app/src/test/java/ai/orkk/shoelog/domain app/src/test/java/ai/orkk/shoelog/data/local
git commit -m "feat: define shoe categories and purposes"
```

---

### Task 2: Room v3 Migration and Repository Mapping

**Files:**
- Modify: `app/src/main/java/ai/orkk/shoelog/data/local/Entities.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/data/local/Migrations.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/data/local/ShoeLogDatabase.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/data/repository/ShoeRepository.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/data/repository/SampleDataRepository.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/ShoeLogApplication.kt`
- Modify: `app/src/androidTest/java/ai/orkk/shoelog/data/local/ShoeLogMigrationTest.kt`
- Modify: `app/src/test/java/ai/orkk/shoelog/data/repository/RepositoryMappingTest.kt`
- Generated: `app/schemas/ai.orkk.shoelog.data.local.ShoeLogDatabase/3.json`

**Interfaces:**
- Consumes: metadata types and codec from Task 1.
- Produces: `MIGRATION_2_3: Migration`.
- Extends: `ShoeDraft.category: ShoeCategory?` and `ShoeDraft.purposes: Set<ShoePurpose>`.
- Persists: `ShoeEntity.categoryCode: String?` and `ShoeEntity.purposeCodes: String`.

- [ ] **Step 1: Add a failing v2→v3 migration test**

```kotlin
@Test
fun migrationFrom2To3PreservesExistingDataAndAddsEmptyMetadata() {
    helper.createDatabase(TEST_DATABASE, 2).apply {
        execSQL(
            "INSERT INTO shoes (id, brand, model, nickname, color, purchaseDateEpochDay, " +
                "startDateEpochDay, purchasePriceWon, listPriceWon, initialMeters, targetMeters, " +
                "photoUri, retired, defaultShoe, createdAtEpochMillis, updatedAtEpochMillis) " +
                "VALUES (7, 'Fictional', 'Runner', '', '', NULL, NULL, 129000, 169000, " +
                "3000, 500000, NULL, 0, 1, 1, 2)",
        )
        close()
    }
    val migrated = helper.runMigrationsAndValidate(TEST_DATABASE, 3, true, MIGRATION_2_3)
    migrated.query(
        "SELECT brand, purchasePriceWon, categoryCode, purposeCodes FROM shoes WHERE id = 7",
    ).use { cursor ->
        assertTrue(cursor.moveToFirst())
        assertEquals("Fictional", cursor.getString(0))
        assertEquals(129_000L, cursor.getLong(1))
        assertTrue(cursor.isNull(2))
        assertEquals("", cursor.getString(3))
    }
}
```

- [ ] **Step 2: Extend the repository mapping test with category and multiple purposes**

Create a `ShoeEntity` with `categoryCode = "max-cushion"` and `purposeCodes = "daily,lsd"`, map it through `toDomainShoe()`, and assert `MAX_CUSHION`, `DAILY`, and `LSD`. Create a `ShoeDraft` with the same metadata, expose its entity mapping as `internal`, and assert the exact stored codes.

- [ ] **Step 3: Run the migration and mapping tests to confirm failure**

Run: `./gradlew testDebugUnitTest --tests '*RepositoryMappingTest' compileDebugAndroidTestKotlin`

Expected: compilation failure for missing entity fields and migration.

- [ ] **Step 4: Implement schema version 3 and register the migration**

```kotlin
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shoes ADD COLUMN categoryCode TEXT")
        db.execSQL("ALTER TABLE shoes ADD COLUMN purposeCodes TEXT NOT NULL DEFAULT ''")
    }
}
```

Set `ShoeLogDatabase.version = 3`, add entity defaults `categoryCode: String? = null` and `purposeCodes: String = ""`, and register both `MIGRATION_1_2` and `MIGRATION_2_3` in `AppContainer`.

- [ ] **Step 5: Map the new fields through `ShoeDraft`, entity, and domain models**

Use `category?.code`, `ShoeMetadataCodec.encodePurposes(purposes)`, `decodeCategory`, and `decodePurposes`. Keep the category and purposes unchanged when opening and saving an existing shoe.

- [ ] **Step 6: Run unit compilation and generate the Room schema**

Run: `./gradlew testDebugUnitTest assembleDebug`

Expected: PASS and a generated version 3 Room schema containing nullable `categoryCode` and non-null `purposeCodes` with default `''`.

- [ ] **Step 7: Run the migration test on the emulator only**

Run: `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ai.orkk.shoelog.data.local.ShoeLogMigrationTest`

Expected: both 1→2 and 2→3 migration tests PASS. Confirm `adb -s emulator-5554 get-state` reports `device` before running.

- [ ] **Step 8: Commit the persistence slice**

```bash
git add app/src/main/java/ai/orkk/shoelog/data app/src/main/java/ai/orkk/shoelog/domain/Models.kt app/src/main/java/ai/orkk/shoelog/ShoeLogApplication.kt app/src/androidTest/java/ai/orkk/shoelog/data/local/ShoeLogMigrationTest.kt app/src/test/java/ai/orkk/shoelog/data/repository/RepositoryMappingTest.kt app/schemas
git commit -m "feat: persist shoe categories and purposes"
```

---

### Task 3: Category and Multi-Purpose Editor

**Files:**
- Create: `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeEditor.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeScreens.kt`
- Modify: `app/src/test/java/ai/orkk/shoelog/ui/shoes/ShoeEditorViewModelTest.kt`
- Modify: `app/src/androidTest/java/ai/orkk/shoelog/ui/shoes/ShoeEditorScreenTest.kt`

**Interfaces:**
- Consumes: `ShoeCategory` and `ShoePurpose` from Task 1.
- Extends: `ShoeEditorForm.category: ShoeCategory?` and `ShoeEditorForm.purposes: Set<ShoePurpose>`.
- Keeps: `ShoeEditorScreen(state, onFormChange, onPickPhoto, onSave, onBack)` so navigation call sites remain stable.

- [ ] **Step 1: Write failing form round-trip tests**

```kotlin
@Test fun metadataConvertsToDraftAndBackToEditableForm() {
    val form = ShoeEditorForm(
        brand = "ASICS",
        model = "Sample",
        category = ShoeCategory.MAX_CUSHION,
        purposes = setOf(ShoePurpose.DAILY, ShoePurpose.LSD),
    )
    val draft = form.toDraft()
    assertEquals(ShoeCategory.MAX_CUSHION, draft.category)
    assertEquals(setOf(ShoePurpose.DAILY, ShoePurpose.LSD), draft.purposes)

    val existing = Shoe(
        id = 7,
        brand = "ASICS",
        model = "Sample",
        targetMeters = 500_000,
        category = draft.category,
        purposes = draft.purposes,
        createdAt = Instant.parse("2026-09-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-09-01T00:00:00Z"),
    )
    assertEquals(form.category, ShoeEditorForm.from(existing).category)
    assertEquals(form.purposes, ShoeEditorForm.from(existing).purposes)
}
```

- [ ] **Step 2: Write failing Compose tests for single category and multiple purpose chips**

Render `ShoeEditorScreen` with a mutable captured form callback. Click `맥스 쿠션화`, `LSD`, and `템포런`. Assert the last emitted form contains one category and both purposes. Then click `안정화` and assert it replaced the category without clearing either purpose.

- [ ] **Step 3: Run focused editor tests and confirm failure**

Run: `./gradlew testDebugUnitTest --tests '*ShoeEditorViewModelTest' compileDebugAndroidTestKotlin`

Expected: compilation failure because editor metadata properties and UI chips are missing.

- [ ] **Step 4: Extract the editor declarations into `ShoeEditor.kt` and add metadata fields**

Move `SuggestedBrands`, `ShoeEditorForm`, `ShoeEditorUiState`, `ShoeEditorViewModel`, `ShoeEditorScreen`, editor-only helpers, and `formatWon` without changing their external package names. Extend form conversion in both directions.

- [ ] **Step 5: Add grouped single-select category chips and multi-select purpose chips**

For every `ShoeCategoryGroup`, show the group label and its categories as wrapping `FilterChip`s. Implement category click as:

```kotlin
onFormChange(form.copy(category = category.takeUnless { it == form.category }))
```

Implement purpose click as:

```kotlin
val purposes = form.purposes.toMutableSet().apply {
    if (!add(purpose)) remove(purpose)
}
onFormChange(form.copy(purposes = purposes))
```

Add stable test tags `category_<code>` and `purpose_<code>` and preserve the current always-active behavior for newly created shoes.

- [ ] **Step 6: Run unit and emulator editor tests**

Run: `./gradlew testDebugUnitTest --tests '*ShoeEditorViewModelTest'`

Run: `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ai.orkk.shoelog.ui.shoes.ShoeEditorScreenTest`

Expected: PASS on the emulator.

- [ ] **Step 7: Commit the editor slice**

```bash
git add app/src/main/java/ai/orkk/shoelog/ui/shoes app/src/test/java/ai/orkk/shoelog/ui/shoes/ShoeEditorViewModelTest.kt app/src/androidTest/java/ai/orkk/shoelog/ui/shoes/ShoeEditorScreenTest.kt
git commit -m "feat: edit shoe categories and purposes"
```

---

### Task 4: Safe Deletion and Sortable Management State

**Files:**
- Modify: `app/src/main/java/ai/orkk/shoelog/data/local/ShoeLogDao.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/data/preferences/SettingsRepository.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/data/repository/ShoeRepository.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/ShoeLogApplication.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeManagement.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/ui/shoes/ShoeSortingTest.kt`
- Test: `app/src/androidTest/java/ai/orkk/shoelog/data/local/ShoeLogDaoTest.kt`
- Test: `app/src/androidTest/java/ai/orkk/shoelog/data/repository/ShoeRepositoryDeletionTest.kt`

**Interfaces:**
- Produces: `enum class DeleteShoeResult { DELETED, BLOCKED_BY_ASSIGNMENTS, NOT_FOUND }`.
- Produces: `DefaultShoePreferenceStore.clearIfMatches(shoeId: Long)` implemented by `SettingsRepository`.
- Produces: `enum class ShoeSortKey { MILEAGE, PURCHASE_DATE, PURCHASE_PRICE, LIST_PRICE, BRAND_MODEL, UPDATED_AT }`.
- Produces: `enum class SortDirection { ASCENDING, DESCENDING }`.
- Produces: `sortShoes(shoes: List<Shoe>, key: ShoeSortKey, direction: SortDirection): List<Shoe>`.
- Produces: `ShoeManagementViewModel` and immutable `ShoeManagementUiState`.

- [ ] **Step 1: Add a failing DAO test for assignment-safe deletion**

```kotlin
@Test fun assignedShoeCannotBeDeletedButUnusedShoeCan() = runTest {
    val assignedId = dao.insertShoe(shoe())
    dao.upsertExercises(listOf(exercise(id = "run:1")))
    dao.assignShoe("run:1", assignedId, automatic = false, assignedAtEpochMillis = 10)
    assertFalse(dao.deleteShoeIfUnused(assignedId))
    assertNotNull(dao.shoeById(assignedId))

    val unusedId = dao.insertShoe(shoe(model = "Unused"))
    assertTrue(dao.deleteShoeIfUnused(unusedId))
    assertNull(dao.shoeById(unusedId))
}
```

- [ ] **Step 2: Add failing comparator tests for every sort key**

Build three shoes with different mileage, dates, prices, brands, and update timestamps. For each `ShoeSortKey`, assert ascending and descending order. For purchase date and both prices, include a null value and assert its ID remains last in both directions. Assert equal primary values use case-insensitive brand/model and then ID as deterministic tie-breakers.

Also add a repository integration test backed by an in-memory Room database and a recording `DefaultShoePreferenceStore`. Assert that an unused default shoe returns `DELETED` and records its ID for preference cleanup, an assigned shoe returns `BLOCKED_BY_ASSIGNMENTS` without cleanup, and a missing ID returns `NOT_FOUND`.

- [ ] **Step 3: Run the focused tests and confirm failure**

Run: `./gradlew testDebugUnitTest --tests '*ShoeSortingTest' compileDebugAndroidTestKotlin`

Expected: compilation failure for `sortShoes` and `deleteShoeIfUnused`.

- [ ] **Step 4: Implement transactional assignment-safe deletion in the DAO**

```kotlin
@Query("SELECT COUNT(*) FROM exercise_shoe_assignments WHERE shoeId = :shoeId")
suspend fun assignmentCountForShoe(shoeId: Long): Int

@Transaction
suspend fun deleteShoeIfUnused(shoeId: Long): Boolean {
    if (assignmentCountForShoe(shoeId) > 0) return false
    deleteShoeById(shoeId)
    return true
}
```

Keep exercise rows untouched. Do not call the existing cascade-based deletion path for an assigned shoe.

- [ ] **Step 5: Implement explicit repository deletion results and preference cleanup**

Define a narrow `DefaultShoePreferenceStore` interface in the preferences package. `SettingsRepository.clearIfMatches` reads `settings.first()` and removes only a matching ID. Inject it into `ShoeRepository` from `AppContainer`. `deleteIfUnused` checks existence, delegates the final assignment check to the DAO transaction, clears the matching preference only after deletion succeeds, and returns the explicit result.

- [ ] **Step 6: Implement stable sorting and management state**

`ShoeManagementUiState` contains `shoes`, `includeRetired = false`, `sortKey = UPDATED_AT`, `sortDirection = DESCENDING`, `pendingDelete: Shoe?`, and `message: String?`. The ViewModel uses `flatMapLatest` to switch repository observation when `includeRetired` changes, then combines it with sort settings. Expose `setIncludeRetired`, `setSortKey`, `toggleSortDirection`, `requestDelete`, `cancelDelete`, `confirmDelete`, and `consumeMessage`. `confirmDelete` maps every `DeleteShoeResult` to the approved user feedback, catches repository exceptions as `러닝화를 삭제하지 못했습니다.`, and always closes the confirmation dialog after the operation.

For nullable keys, partition into non-null and null values, sort only the non-null partition in the selected direction, and append nulls. Always apply brand/model and ID tie-breakers.

- [ ] **Step 7: Run unit and emulator DAO tests**

Run: `./gradlew testDebugUnitTest --tests '*ShoeSortingTest'`

Run: `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=ai.orkk.shoelog.data.local.ShoeLogDaoTest,ai.orkk.shoelog.data.repository.ShoeRepositoryDeletionTest`

Expected: PASS; assigned shoes remain present and unused shoes are removed.

- [ ] **Step 8: Commit the management logic slice**

```bash
git add app/src/main/java/ai/orkk/shoelog/data app/src/main/java/ai/orkk/shoelog/ShoeLogApplication.kt app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeManagement.kt app/src/test/java/ai/orkk/shoelog/ui/shoes/ShoeSortingTest.kt app/src/androidTest/java/ai/orkk/shoelog/data/local/ShoeLogDaoTest.kt app/src/androidTest/java/ai/orkk/shoelog/data/repository/ShoeRepositoryDeletionTest.kt
git commit -m "feat: add safe shoe deletion and sorting"
```

---

### Task 5: Management UI, Detail Metadata, and Navigation

**Files:**
- Modify: `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeManagement.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeDetail.kt`
- Modify/Delete: `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeScreens.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/ui/Components.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/ui/ShoeLogApp.kt`
- Create: `app/src/androidTest/java/ai/orkk/shoelog/ui/shoes/ShoeManagementScreenTest.kt`
- Modify: `app/src/androidTest/java/ai/orkk/shoelog/ui/shoes/ShoeDetailScreenTest.kt`
- Modify: `app/src/androidTest/java/ai/orkk/shoelog/ui/HomeScreenTest.kt`

**Interfaces:**
- Consumes: `ShoeManagementViewModel` state and actions from Task 4.
- Extends: `ShoeListScreen` to accept full management state plus sort, edit, open, and delete callbacks.
- Extends: `ShoeDetailScreen` with `onDelete: () -> Unit` while retaining back, edit, and retirement callbacks.
- Adds: `MainDestination(Routes.SHOES, Routes.SHOES, "러닝화", "👟")`.

- [ ] **Step 1: Write failing management-screen UI tests**

Render two shoes with category and purpose metadata. Verify `러닝화 추가`, category text, purpose chips, sort selector, sort-direction control, and edit/delete actions are visible. Click delete and assert a confirmation dialog contains the shoe name. Render a blocked deletion message and assert the text directs the user to retire or unassign runs.

- [ ] **Step 2: Extend detail tests for category, purposes, and delete action**

Render a `MAX_CUSHION` shoe with `LSD` and `TEMPO`. Assert `데일리 · 맥스 쿠션화`, `LSD`, `템포런`, and `삭제` are displayed. Trigger the deletion callback and assert the captured flag changes.

- [ ] **Step 3: Add a navigation regression assertion**

Extract `mainDestinations` to internal visibility or add a root-app Compose test, then assert the labels are exactly `홈`, `달리기`, `러닝화`, `설정` and that selecting `러닝화` navigates to `Routes.SHOES`.

- [ ] **Step 4: Run UI compilation and confirm failure**

Run: `./gradlew compileDebugAndroidTestKotlin`

Expected: compilation failure for new management and detail callbacks or assertions.

- [ ] **Step 5: Build the management list UI**

Keep the top-bar add action always visible. Add the active/retired switch, a single-choice sort menu, an ascending/descending button, and cards that show optional metadata plus mileage, purchase date, and purchase price. Use a per-card overflow menu with `상세`, `수정`, and `삭제`. Use stable semantics/test tags for each action.

Show an `AlertDialog` for confirmation. On a blocked result, show: `달리기 기록이 연결된 러닝화는 삭제할 수 없습니다. 은퇴 처리하거나 기록 배정을 해제한 뒤 다시 시도해 주세요.`

- [ ] **Step 6: Extract and extend the detail screen**

Move detail declarations into `ShoeDetail.kt`. Display group/category and each selected purpose as read-only chips. Add a delete button after the retirement button. Reuse the same confirmation wording and navigate back only when deletion returns `DELETED`.

- [ ] **Step 7: Wire the bottom navigation and ViewModels**

Add the `러닝화` destination between runs and settings. Create the management ViewModel with `ShoeManagementViewModel.factory(container.shoeRepository)`. Use the same repository deletion API for list and detail flows. Keep editor navigation and Home’s always-visible add button behavior unchanged.

- [ ] **Step 8: Run focused UI tests on the emulator only**

Run: `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.package=ai.orkk.shoelog.ui.shoes`

Expected: editor, management, and detail tests PASS.

- [ ] **Step 9: Commit the UI and navigation slice**

```bash
git add app/src/main/java/ai/orkk/shoelog/ui app/src/androidTest/java/ai/orkk/shoelog/ui
git commit -m "feat: add running shoe management tab"
```

---

### Task 6: Documentation, Full Verification, APK, and Push

**Files:**
- Modify: `README.md`
- Verify: `.gitignore`
- Verify: `app/build/outputs/apk/debug/app-debug.apk`

**Interfaces:**
- Consumes: all implemented behavior from Tasks 1–5.
- Produces: public documentation and a verified debug APK; no tracked health or personal data.

- [ ] **Step 1: Update README behavior and safety wording**

Add category, multiple-purpose chips, the bottom management tab, sorting keys, edit/delete behavior, and v2→v3 preservation to the feature list. Replace the current statement that shoe deletion unassigns runs with the approved rule that assigned shoes cannot be deleted. State that users may retire them or unassign runs first.

- [ ] **Step 2: Run the repository privacy check before staging**

Run: `git status --short --ignored`

Expected: `local.properties`, signing keys, databases, DataStore files, health-data folders, APKs, and build output are absent from tracked changes or shown only as ignored.

Run: `git ls-files | rg '(^|/)(local\.properties|.*\.(jks|keystore|p12)|health-data|private-fixtures|.*\.(db|sqlite|apk|aab))$'`

Expected: no output.

- [ ] **Step 3: Run the full local verification suite**

Run: `./gradlew testDebugUnitTest lintDebug assembleDebug compileDebugAndroidTestKotlin`

Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Run all instrumentation tests on the emulator only**

First run: `adb -s emulator-5554 get-state`

Expected: `device`.

Then run: `ANDROID_SERIAL=emulator-5554 ./gradlew connectedDebugAndroidTest`

Expected: all instrumentation and Compose tests PASS. Do not substitute the physical-phone serial.

- [ ] **Step 5: Inspect final changes and commit documentation**

Run: `git diff --check`

Expected: no output.

```bash
git add README.md
git commit -m "docs: document shoe management features"
```

- [ ] **Step 6: Install the verified APK on the physical phone only when it is connected and unlocked**

Run: `adb devices -l`

Select the physical `SM-F976N` serial, not `emulator-5554`, then run ordinary replacement install:

```bash
adb -s RFKL70ADYML install -r app/build/outputs/apk/debug/app-debug.apk
adb -s RFKL70ADYML shell monkey -p ai.orkk.shoelog -c android.intent.category.LAUNCHER 1
```

Expected: `Success`; ShoeLog launches. Do not run instrumentation tests, `pm clear`, or uninstall on this phone.

- [ ] **Step 7: Push the reviewed commits to the personal public repository**

Run: `git status --short`

Expected: no tracked changes.

Run: `git push origin main`

Expected: `main` on `git@github.com:or-kk/shoelog.git` advances to the final local commit.
