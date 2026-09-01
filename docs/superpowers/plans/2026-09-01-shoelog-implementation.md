# ShoeLog Android App Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a private-by-design Android phone app that imports Samsung Health running sessions through Health Connect, assigns one shoe to each run, and derives per-shoe mileage locally.

**Architecture:** Keep the existing single `app` module and separate domain rules, Room persistence, settings, Health Connect access, repositories, and Compose feature screens through focused packages and interfaces. The UI consumes immutable state from ViewModels; real and fake Health Connect implementations share one contract so the app remains testable without health data.

**Tech Stack:** Kotlin with AGP 9.2.1 built-in Kotlin, JDK 17, compileSdk 36.1, targetSdk 36, minSdk 28, Compose BOM 2026.06.01, Material 3, Navigation Compose 2.9.8, Room 2.8.4 with KSP 2.3.11, Lifecycle 2.11.0, Activity Compose 1.13.0, DataStore 1.2.1, Health Connect 1.1.0, Coroutines, Flow, JUnit, AndroidX test.

**Spec:** `docs/superpowers/specs/2026-09-01-shoelog-design.md`

## Global Constraints

- Phone app only; Galaxy Watch and Samsung Health remain responsible for recording runs.
- Read only `ExerciseSessionRecord` and `DistanceRecord`; never request route, location, heart rate, or unrelated health permissions.
- Store all exercise, shoe, assignment, and settings data on-device; declare no `INTERNET` permission.
- Keep `local.properties`, signing keys/config, health exports, private fixtures, DataStore files, databases, and personal settings out of Git.
- Use meter-valued `Long` fields internally and format kilometers only at the UI boundary.
- Never persist a mutable mileage total; derive it from initial distance plus current assignments.
- Build Korean-first UI, dark mode, explicit loading/empty/error/permission states, content descriptions, and 48dp touch targets.
- Keep fake sample records fictional and separate from personal health data.
- Preserve the existing package/application ID `ai.orkk.shoelog` and branch `main`.

---

## Planned File Map

```text
app/src/main/java/ai/orkk/shoelog/
├── ShoeLogApplication.kt                 dependency container
├── MainActivity.kt                       activity, permissions, app root
├── data/health/
│   ├── HealthConnectDataSource.kt         gateway contract and DTOs
│   ├── AndroidHealthConnectDataSource.kt  real SDK adapter
│   └── FakeHealthConnectDataSource.kt     deterministic sample/test adapter
├── data/local/
│   ├── Entities.kt                        Room tables
│   ├── ShoeLogDao.kt                      reads, writes, projections
│   └── ShoeLogDatabase.kt                 database and converters
├── data/preferences/
│   └── SettingsRepository.kt              Preferences DataStore
├── data/repository/
│   ├── ShoeRepository.kt                  shoe CRUD and mileage streams
│   ├── ExerciseRepository.kt              assignments and exercise streams
│   └── SyncRepository.kt                  Health Connect reconciliation
├── domain/
│   ├── Models.kt                          domain and UI-independent models
│   ├── MileageCalculator.kt               distance/progress rules
│   └── ExerciseIdentity.kt                fallback deduplication key
├── notification/RunNotificationManager.kt notification channel/deep link
└── ui/
    ├── ShoeLogApp.kt                      nav host and app scaffold
    ├── Components.kt                      shared cards/states/formatters
    ├── home/HomeScreen.kt                 dashboard and HomeViewModel
    ├── shoes/ShoeScreens.kt               list/detail/editor and ViewModels
    ├── exercises/ExerciseScreen.kt         list/filter/assignment
    ├── settings/SettingsScreen.kt          permissions/privacy/sample controls
    └── theme/{Color,Theme,Type}.kt          visual system
```

Tests mirror these responsibilities under `app/src/test` and `app/src/androidTest`.

---

### Task 1: Build, Compose, Privacy, and Test Foundation

**Files:**
- Modify: `.gitignore`
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts`
- Modify: `app/build.gradle.kts`
- Modify: `gradle.properties`
- Modify: `app/src/main/AndroidManifest.xml`
- Modify: `app/src/main/res/xml/backup_rules.xml`
- Modify: `app/src/main/res/xml/data_extraction_rules.xml`
- Create: `app/src/main/java/ai/orkk/shoelog/MainActivity.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/ui/theme/Color.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/ui/theme/Theme.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/ui/theme/Type.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/BuildFoundationTest.kt`

**Interfaces:**
- Produces: Compose-enabled application with `MainActivity : ComponentActivity` and `ShoeLogTheme(content: @Composable () -> Unit)`.

- [ ] **Step 1: Strengthen the ignore policy before staging Android files**

Add exact patterns for `*.jks`, `*.keystore`, `*.p12`, `keystore.properties`, `signing.properties`, `health-data/`, `private-fixtures/`, `*.db`, `*.db-*`, `*.preferences_pb`, APK/AAB files, `.kotlin/`, and generated Room schemas while retaining the existing Android Studio exclusions.

- [ ] **Step 2: Write a failing foundation test**

```kotlin
class BuildFoundationTest {
    @Test fun appIdentityIsStable() {
        assertEquals("ai.orkk.shoelog", BuildConfig.APPLICATION_ID)
    }
}
```

- [ ] **Step 3: Run the focused test and confirm the missing BuildConfig failure**

Run: `./gradlew :app:testDebugUnitTest --tests '*BuildFoundationTest*'`
Expected: FAIL until `buildFeatures.buildConfig = true` and the Compose source set compiles.

- [ ] **Step 4: Configure stable dependencies and Compose**

Use the versions named in the plan header. Apply `com.google.devtools.ksp` and `androidx.room`; enable Compose, BuildConfig, Room schema export, Java/Kotlin 17, minSdk 28, and test resources. Add Activity Compose, Compose BOM/material3/ui/tooling, lifecycle runtime/viewmodel Compose, Navigation Compose, Room runtime/ktx/compiler, DataStore preferences, Health Connect, coroutines test, AndroidX test core/runner/rules, and Compose UI tests.

- [ ] **Step 5: Replace the empty manifest with a private app manifest**

Declare `MainActivity`, `ShoeLogApplication`, Health Connect read permissions, optional history permission, `POST_NOTIFICATIONS`, and Health Connect permission rationale aliases. Do not declare `INTERNET`, location, route, heart-rate, storage, camera, or background health permissions.

- [ ] **Step 6: Implement the activity and theme shell**

Set edge-to-edge content, a black/charcoal palette, lime accent, system dark theme support, and Material typography. Render a temporary `Text("ShoeLog")` inside `ShoeLogTheme` so the module is independently runnable.

- [ ] **Step 7: Exclude Room/DataStore from device backup and verify**

Set `allowBackup="false"` as the primary safeguard and keep explicit exclusion rules for `database`, `sharedpref`, and `file` domains for OEM behavior. Run `./gradlew :app:testDebugUnitTest :app:assembleDebug` and expect PASS.

- [ ] **Step 8: Commit the build foundation**

```bash
git add .gitignore gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts gradle.properties app/src/main
git commit -m "build: configure private Compose app foundation"
```

### Task 2: Domain Mileage and Identity Rules

**Files:**
- Create: `app/src/main/java/ai/orkk/shoelog/domain/Models.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/domain/MileageCalculator.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/domain/ExerciseIdentity.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/domain/MileageCalculatorTest.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/domain/ExerciseIdentityTest.kt`

**Interfaces:**
- Produces: `MileageCalculator.calculate(initialMeters: Long, assignedMeters: Iterable<Long?>, targetMeters: Long): MileageSummary`.
- Produces: `ExerciseIdentity.fallbackKey(originPackage: String, start: Instant, end: Instant, type: Int): String`.
- Produces: `MileageStatus.NORMAL`, `NEAR_TARGET`, and `TARGET_REACHED`.

- [ ] **Step 1: Write failing calculation tests**

```kotlin
@Test fun includesInitialDistanceAndAssignedRuns() {
    val result = MileageCalculator.calculate(25_000, listOf(5_000L, 10_000L), 100_000)
    assertEquals(40_000, result.totalMeters)
    assertEquals(60_000, result.remainingMeters)
    assertEquals(0.4f, result.progress)
}

@Test fun ignoresMissingAndZeroDistance() {
    val result = MileageCalculator.calculate(1_000, listOf(null, 0, 2_000), 10_000)
    assertEquals(3_000, result.totalMeters)
}

@Test fun distinguishesNearAndReachedTargets() {
    assertEquals(MileageStatus.NEAR_TARGET, MileageCalculator.calculate(90, emptyList(), 100).status)
    assertEquals(MileageStatus.TARGET_REACHED, MileageCalculator.calculate(101, emptyList(), 100).status)
}
```

- [ ] **Step 2: Run tests and confirm missing domain types**

Run: `./gradlew :app:testDebugUnitTest --tests '*MileageCalculatorTest*' --tests '*ExerciseIdentityTest*'`
Expected: FAIL with unresolved `MileageCalculator` and `ExerciseIdentity`.

- [ ] **Step 3: Implement immutable models and calculations**

Clamp negative distances to zero, cap progress at `1f` for the progress bar, expose raw usage ratio separately if needed, and treat a non-positive target as zero progress with `NORMAL` status.

- [ ] **Step 4: Implement deterministic SHA-256 fallback identity**

Hash UTF-8 text in the exact order `originPackage|startEpochMillis|endEpochMillis|type`; return lowercase hex. Add a test proving the same input is stable and a changed start time changes the key.

- [ ] **Step 5: Run focused tests and commit**

Run: `./gradlew :app:testDebugUnitTest --tests '*domain*'`
Expected: PASS.

```bash
git add app/src/main/java/ai/orkk/shoelog/domain app/src/test/java/ai/orkk/shoelog/domain
git commit -m "feat: add mileage and exercise identity rules"
```

### Task 3: Room Data Model and Assignment Semantics

**Files:**
- Create: `app/src/main/java/ai/orkk/shoelog/data/local/Entities.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/data/local/ShoeLogDao.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/data/local/ShoeLogDatabase.kt`
- Test: `app/src/androidTest/java/ai/orkk/shoelog/data/local/ShoeLogDaoTest.kt`

**Interfaces:**
- Produces: `ShoeEntity`, `ExerciseEntity`, `ExerciseShoeAssignmentEntity`, `ShoeMileageRow`, and `ExerciseWithShoeRow`.
- Produces: `ShoeLogDao.observeShoes(includeRetired: Boolean): Flow<List<ShoeMileageRow>>`.
- Produces: transactional `assignShoe(exerciseId: String, shoeId: Long?, automatic: Boolean)` and `deleteShoePreservingExercises(shoeId: Long)`.

- [ ] **Step 1: Write failing in-memory Room tests**

Create fictional shoes and runs. Assert a 10 km initial distance plus 5 km assigned run yields 15 km, reassignment moves the 5 km from shoe A to B, deletion preserves the exercise and clears its assignment, and inserting the same Health Connect ID twice leaves one row.

- [ ] **Step 2: Run the instrumentation test compile**

Run: `./gradlew :app:compileDebugAndroidTestKotlin`
Expected: FAIL because entities and DAO do not exist.

- [ ] **Step 3: Implement entities, foreign keys, indices, and projections**

Use `onDelete = CASCADE` from exercise to assignment, `onDelete = CASCADE` from shoe to assignment only, unique `fallbackKey`, nullable `distanceMeters`, and `sourceDeleted`. Do not add a total-mileage column.

- [ ] **Step 4: Implement transactional DAO operations**

Use `INSERT OR REPLACE` for one-to-one assignments, `INSERT OR IGNORE` plus update for exercise reconciliation, one transaction to change the default shoe, and SQL `COALESCE(SUM(distanceMeters), 0)` constrained to non-deleted positive-distance exercises.

- [ ] **Step 5: Run database tests on an available emulator if present**

Run: `./gradlew :app:connectedDebugAndroidTest` when a device is listed; otherwise run `:app:compileDebugAndroidTestKotlin` and record device execution as a real-device follow-up.

- [ ] **Step 6: Commit persistence**

```bash
git add app/src/main/java/ai/orkk/shoelog/data/local app/src/androidTest/java/ai/orkk/shoelog/data/local
git commit -m "feat: persist shoes exercises and assignments"
```

### Task 4: Settings and Repository APIs

**Files:**
- Create: `app/src/main/java/ai/orkk/shoelog/data/preferences/SettingsRepository.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/data/repository/ShoeRepository.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/data/repository/ExerciseRepository.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/data/repository/RepositoryMappingTest.kt`

**Interfaces:**
- Produces: `data class AppSettings(autoAssignDefault: Boolean, defaultShoeId: Long?, historyRequested: Boolean, sampleMode: Boolean)`.
- Produces: `SettingsRepository.settings: Flow<AppSettings>` and suspend setters for every setting.
- Produces: `ShoeRepository` CRUD/retire/default APIs and `ExerciseRepository` list/filter/assign APIs.

- [ ] **Step 1: Write failing row-to-domain mapping tests**

Assert null photo/date values remain null, retired state is preserved, and distance-less exercises remain visible but contribute zero mileage.

- [ ] **Step 2: Run focused tests and confirm missing repositories**

Run: `./gradlew :app:testDebugUnitTest --tests '*RepositoryMappingTest*'`
Expected: FAIL with unresolved repositories.

- [ ] **Step 3: Implement DataStore settings**

Use application-scoped `preferencesDataStore(name = "shoelog_settings")`, catch `IOException` by emitting defaults, and never log preference contents.

- [ ] **Step 4: Implement repository mapping and validation**

Trim brand/model/alias, require brand and model, reject negative distances, require positive target distance, and expose `Flow` rather than cached mutable lists.

- [ ] **Step 5: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest --tests '*RepositoryMappingTest*'`
Expected: PASS.

```bash
git add app/src/main/java/ai/orkk/shoelog/data/preferences app/src/main/java/ai/orkk/shoelog/data/repository app/src/test/java/ai/orkk/shoelog/data/repository
git commit -m "feat: add settings and local repositories"
```

### Task 5: Health Connect Gateway and Synchronization

**Files:**
- Create: `app/src/main/java/ai/orkk/shoelog/data/health/HealthConnectDataSource.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/data/health/AndroidHealthConnectDataSource.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/data/health/FakeHealthConnectDataSource.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/data/repository/SyncRepository.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/data/repository/SyncRepositoryTest.kt`

**Interfaces:**
- Produces: `HealthConnectAvailability`, `HealthPermissionState`, `HealthExercise`, and `SyncResult` sealed/data types.
- Produces: `HealthConnectDataSource.availability()`, `grantedPermissions()`, `readRunningExercises(start: Instant, end: Instant)`, and `historyFeatureAvailable()`.
- Produces: `SyncRepository.sync(now: Instant): SyncResult`.

- [ ] **Step 1: Write synchronization contract tests with fakes**

Cover: new ID inserts once across two syncs; changed distance updates; a missing record in the completed sync window becomes `sourceDeleted`; auto-assign uses the active default shoe; missing distance stays unassigned and visible; gateway exception returns a Korean-safe error code without health values.

- [ ] **Step 2: Run tests and confirm missing gateway/sync types**

Run: `./gradlew :app:testDebugUnitTest --tests '*SyncRepositoryTest*'`
Expected: FAIL with unresolved `HealthConnectDataSource` and `SyncRepository`.

- [ ] **Step 3: Implement the fake and sync algorithm first**

Use fictional records only. Reconcile one completed time window in a Room transaction, preserve assignments on updates, and mark deletion only for IDs previously observed inside that exact window.

- [ ] **Step 4: Implement the Android adapter**

Check `HealthConnectClient.getSdkStatus`, request only `READ_EXERCISE`, `READ_DISTANCE`, and optionally history, page through running `ExerciseSessionRecord` records, and aggregate `DistanceRecord.DISTANCE_TOTAL` over each session with its `metadata.dataOrigin` filter. Map Samsung Health package data when exposed; otherwise show the package label without inference.

- [ ] **Step 5: Run unit tests and compile the SDK adapter**

Run: `./gradlew :app:testDebugUnitTest --tests '*SyncRepositoryTest*' :app:compileDebugKotlin`
Expected: PASS.

- [ ] **Step 6: Commit Health Connect integration**

```bash
git add app/src/main/java/ai/orkk/shoelog/data/health app/src/main/java/ai/orkk/shoelog/data/repository/SyncRepository.kt app/src/test/java/ai/orkk/shoelog/data/repository/SyncRepositoryTest.kt
git commit -m "feat: synchronize running records from Health Connect"
```

### Task 6: Application Container and Fictional Sample Data

**Files:**
- Create: `app/src/main/java/ai/orkk/shoelog/ShoeLogApplication.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/data/repository/SampleDataRepository.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/data/repository/SampleDataRepositoryTest.kt`

**Interfaces:**
- Produces: `AppContainer` with database, DAO, settings, health gateway, repositories, and notification manager.
- Produces: idempotent `SampleDataRepository.install()` and `clear()`.

- [ ] **Step 1: Write a failing idempotency/privacy test**

Install sample data twice and assert the same fictional shoe/run IDs and counts. Assert no strings contain the configured Git email, `Samsung Health` user identifiers, or attachment content.

- [ ] **Step 2: Implement a manual dependency container**

Construct one Room database and repositories from `Application`; avoid adding a DI framework for this single-module first release.

- [ ] **Step 3: Implement deterministic fictional samples**

Include multiple active shoes, one retired shoe, assigned and unassigned runs, one near-target shoe, and one distance-less run. Prefix stable sample IDs with `sample:` and clear only that namespace.

- [ ] **Step 4: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest --tests '*SampleDataRepositoryTest*'`
Expected: PASS.

```bash
git add app/src/main/java/ai/orkk/shoelog/ShoeLogApplication.kt app/src/main/java/ai/orkk/shoelog/data/repository/SampleDataRepository.kt app/src/test/java/ai/orkk/shoelog/data/repository/SampleDataRepositoryTest.kt
git commit -m "feat: add app container and fictional sample mode"
```

### Task 7: Compose Navigation, Home, and Shared UI States

**Files:**
- Modify: `app/src/main/java/ai/orkk/shoelog/MainActivity.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/ui/ShoeLogApp.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/ui/Components.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/ui/home/HomeScreen.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/ui/FormattersTest.kt`
- Test: `app/src/androidTest/java/ai/orkk/shoelog/ui/HomeScreenTest.kt`

**Interfaces:**
- Produces: routes `home`, `shoes`, `shoe/{shoeId}`, `shoe/edit?shoeId={shoeId}`, `exercises?exerciseId={exerciseId}`, and `settings`.
- Produces: `formatDistance(meters: Long?): String` using Korean locale-safe one-decimal km output.
- Produces: `HomeUiState` and `HomeViewModel`.

- [ ] **Step 1: Write failing formatter and Compose state tests**

Assert `12_340L` formats as `12.3 km`, null as `거리 정보 없음`, and the home empty/permission/error/content states expose unique test tags and recovery buttons.

- [ ] **Step 2: Implement shared components and navigation shell**

Add bottom navigation for 홈/달리기/설정, screen-level loading/empty/error components, semantic content descriptions, and no health values in exception text.

- [ ] **Step 3: Implement HomeViewModel and HomeScreen**

Combine shoe mileage, recent exercises, unassigned count, settings, and sync state. Show connection card, refresh button, active shoe cards, recent runs, target states, and navigation callbacks.

- [ ] **Step 4: Run UI compile/tests and commit**

Run: `./gradlew :app:testDebugUnitTest --tests '*FormattersTest*' :app:compileDebugAndroidTestKotlin`
Expected: PASS.

```bash
git add app/src/main/java/ai/orkk/shoelog/MainActivity.kt app/src/main/java/ai/orkk/shoelog/ui app/src/test/java/ai/orkk/shoelog/ui app/src/androidTest/java/ai/orkk/shoelog/ui/HomeScreenTest.kt
git commit -m "feat: add navigation and mileage dashboard"
```

### Task 8: Shoe Management Screens and Photo Picker

**Files:**
- Create: `app/src/main/java/ai/orkk/shoelog/ui/shoes/ShoeScreens.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/ui/shoes/ShoeEditorViewModelTest.kt`
- Test: `app/src/androidTest/java/ai/orkk/shoelog/ui/shoes/ShoeEditorScreenTest.kt`

**Interfaces:**
- Produces: `ShoeListScreen`, `ShoeDetailScreen`, `ShoeEditorScreen`, `ShoeEditorViewModel`, and validated `ShoeDraft`.

- [ ] **Step 1: Write failing editor validation tests**

Assert blank brand/model, negative initial distance, and non-positive target distance cannot save; a valid draft converts km text to exact meters; editing preserves ID and assignments.

- [ ] **Step 2: Implement list/detail/editor state flows**

Provide active/retired filtering, default badge, mileage progress, run history, edit, retire/restore, and guarded delete for unused shoes.

- [ ] **Step 3: Implement brand suggestions and Photo Picker**

Seed the required 13 brands, merge distinct local brands/models, allow arbitrary text, launch `PickVisualMedia(ImageOnly)`, call `takePersistableUriPermission` when supported, and surface URI failure without crashing or logging the URI.

- [ ] **Step 4: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest --tests '*ShoeEditorViewModelTest*' :app:compileDebugAndroidTestKotlin`
Expected: PASS.

```bash
git add app/src/main/java/ai/orkk/shoelog/ui/shoes app/src/test/java/ai/orkk/shoelog/ui/shoes app/src/androidTest/java/ai/orkk/shoelog/ui/shoes
git commit -m "feat: manage running shoes and photos"
```

### Task 9: Exercise Assignment and Settings/Permission Flows

**Files:**
- Create: `app/src/main/java/ai/orkk/shoelog/ui/exercises/ExerciseScreen.kt`
- Create: `app/src/main/java/ai/orkk/shoelog/ui/settings/SettingsScreen.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/MainActivity.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/ui/exercises/ExerciseViewModelTest.kt`
- Test: `app/src/androidTest/java/ai/orkk/shoelog/ui/settings/SettingsScreenTest.kt`

**Interfaces:**
- Produces: `ExerciseListViewModel.assign(exerciseId: String, shoeId: Long?)`.
- Produces: permission launch callbacks for base health, history health, and notifications.

- [ ] **Step 1: Write failing assignment/filter tests**

Assert the unassigned filter includes only active, source-present exercises without assignment; reassignment changes mileage streams immediately; distance-less runs remain assignable but add zero.

- [ ] **Step 2: Implement exercise list and assignment sheet**

Show date/time, distance, source, assignment, all/unassigned filter, active shoes, change/unassign actions, and deep-link selection by exercise ID.

- [ ] **Step 3: Implement settings and Health Connect guidance**

Show available/update-required/unsupported states, permission rationale before launcher use, Samsung Health → 설정 → Health Connect guidance, manual sync, default auto-assignment, history feature/permission, sample mode, and concise on-device privacy copy.

- [ ] **Step 4: Wire activity result contracts**

Use `PermissionController.createRequestPermissionResultContract()` for health permissions and `RequestPermission` for notifications. Request history separately only after feature availability and explicit user action.

- [ ] **Step 5: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest --tests '*ExerciseViewModelTest*' :app:compileDebugAndroidTestKotlin`
Expected: PASS.

```bash
git add app/src/main/java/ai/orkk/shoelog/MainActivity.kt app/src/main/java/ai/orkk/shoelog/ui/exercises app/src/main/java/ai/orkk/shoelog/ui/settings app/src/test/java/ai/orkk/shoelog/ui/exercises app/src/androidTest/java/ai/orkk/shoelog/ui/settings
git commit -m "feat: assign shoes and manage Health Connect settings"
```

### Task 10: Unassigned Run Notifications

**Files:**
- Create: `app/src/main/java/ai/orkk/shoelog/notification/RunNotificationManager.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/data/repository/SyncRepository.kt`
- Modify: `app/src/main/java/ai/orkk/shoelog/ShoeLogApplication.kt`
- Test: `app/src/test/java/ai/orkk/shoelog/notification/NotificationPolicyTest.kt`

**Interfaces:**
- Produces: `NotificationPolicy.shouldNotify(newExercises: List<Exercise>, autoAssignedIds: Set<String>): Boolean`.
- Produces: `RunNotificationManager.notifyUnassigned(exerciseId: String, count: Int)`.

- [ ] **Step 1: Write failing notification policy tests**

Assert no notification for zero new runs or runs auto-assigned to a default shoe; notify once for one or more genuinely unassigned new runs.

- [ ] **Step 2: Implement channel, permission guard, and deep link**

Create one low-importance channel, use immutable/update-current pending intent flags, open `exercises?exerciseId=<id>`, avoid showing time/distance on the lock screen, and skip silently when notification permission is absent.

- [ ] **Step 3: Trigger notification after successful foreground sync only**

Do not add WorkManager or a foreground service in version 1.0. Avoid repeating notifications for already-known unassigned exercises.

- [ ] **Step 4: Run tests and commit**

Run: `./gradlew :app:testDebugUnitTest --tests '*NotificationPolicyTest*'`
Expected: PASS.

```bash
git add app/src/main/java/ai/orkk/shoelog/notification app/src/main/java/ai/orkk/shoelog/data/repository/SyncRepository.kt app/src/main/java/ai/orkk/shoelog/ShoeLogApplication.kt app/src/test/java/ai/orkk/shoelog/notification
git commit -m "feat: notify about unassigned runs"
```

### Task 11: README, License, Privacy Audit, and Release Verification

**Files:**
- Create: `README.md`
- Create: `LICENSE`
- Create: `docs/PRIVACY.md`
- Create: `docs/DEVICE_TEST_CHECKLIST.md`
- Modify: `app/src/main/res/values/strings.xml`
- Modify: `app/src/main/res/values/themes.xml`
- Modify: `app/src/main/res/values-night/themes.xml`

**Interfaces:**
- Produces: complete contributor/user documentation and Apache-2.0 licensing.

- [ ] **Step 1: Write the documentation**

Document purpose, architecture, exact toolchain, build/run commands, Samsung Health and Health Connect setup, exact permissions and reasons, fictional sample mode, project structure, Galaxy device test steps, known limits, privacy/storage behavior, and roadmap. Include no personal email beyond the Apache copyright name `or-kk` unless explicitly desired.

- [ ] **Step 2: Audit tracked and ignored files before staging**

Run:

```bash
git status --short --ignored
git check-ignore -v local.properties test.jks health-data/example.json private-fixtures/settings.json
git ls-files | rg '(local\.properties|\.jks$|\.keystore$|\.p12$|health-data|private-fixtures|preferences_pb|\.db(-|$))'
```

Expected: the first four synthetic paths are ignored and `git ls-files` prints nothing.

- [ ] **Step 3: Run complete verification**

Run: `./gradlew --no-daemon clean :app:testDebugUnitTest :app:lintDebug :app:assembleDebug :app:compileDebugAndroidTestKotlin`.
Expected: BUILD SUCCESSFUL with unit tests and lint passing. If an emulator/device is available, also run `:app:connectedDebugAndroidTest`.

- [ ] **Step 4: Inspect the APK and manifest**

Confirm `app/build/outputs/apk/debug/app-debug.apk` exists. Use `apkanalyzer manifest permissions` or `aapt dump permissions` and verify there is no internet, location, route, heart-rate, storage, camera, or background health permission.

- [ ] **Step 5: Commit documentation and verified release state**

```bash
git add README.md LICENSE docs app/src/main/res
git commit -m "docs: document setup privacy and device verification"
```

- [ ] **Step 6: Prepare and push the public GitHub repository**

Verify `origin` equals `git@github.com:or-kk/shoelog.git`, re-run the tracked-file privacy scan, fetch the empty/known remote, and push `main` with upstream tracking only after every earlier command succeeds.

```bash
git remote -v
git push -u origin main
```

Expected: GitHub `or-kk/shoelog` receives the reviewed commit history without ignored or generated data.
