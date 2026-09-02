package ai.orkk.shoelog.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.orkk.shoelog.data.local.ExerciseEntity
import ai.orkk.shoelog.data.local.ShoeLogDatabase
import ai.orkk.shoelog.data.preferences.SettingsRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoeRepositoryDeletionTest {
    private lateinit var database: ShoeLogDatabase
    private lateinit var repository: ShoeRepository
    private lateinit var settings: SettingsRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ShoeLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        settings = SettingsRepository(context)
        runBlocking { settings.setDefaultShoeId(null) }
        repository = ShoeRepository(
            dao = database.shoeLogDao(),
            defaultShoePreferences = settings,
            clock = Clock.fixed(Instant.parse("2026-09-03T00:00:00Z"), ZoneOffset.UTC),
        )
    }

    @After
    fun tearDown() {
        runBlocking { settings.setDefaultShoeId(null) }
        database.close()
    }

    @Test
    fun unusedDefaultShoeIsDeletedAndPreferenceIsCleared() = runTest {
        val shoeId = repository.save(null, ShoeDraft("Fictional", "Unused", defaultShoe = true))
        settings.setDefaultShoeId(shoeId)

        val result = repository.deleteIfUnused(shoeId)

        assertEquals(DeleteShoeResult.DELETED, result)
        assertNull(database.shoeLogDao().shoeById(shoeId))
        assertNull(settings.settings.first().defaultShoeId)
    }

    @Test
    fun assignedShoeIsBlockedWithoutClearingPreference() = runTest {
        val shoeId = repository.save(null, ShoeDraft("Fictional", "Assigned", defaultShoe = true))
        settings.setDefaultShoeId(shoeId)
        val dao = database.shoeLogDao()
        dao.upsertExercises(listOf(exercise("run:assigned")))
        dao.assignShoe("run:assigned", shoeId, automatic = false, assignedAtEpochMillis = 10)

        val result = repository.deleteIfUnused(shoeId)

        assertEquals(DeleteShoeResult.BLOCKED_BY_ASSIGNMENTS, result)
        assertEquals(shoeId, settings.settings.first().defaultShoeId)
    }

    @Test
    fun missingShoeReturnsNotFound() = runTest {
        assertEquals(DeleteShoeResult.NOT_FOUND, repository.deleteIfUnused(999))
    }

    private fun exercise(id: String) = ExerciseEntity(
        id = id,
        fallbackKey = "fallback:$id",
        startEpochMillis = 100,
        endEpochMillis = 200,
        distanceMeters = 5_000,
        exerciseType = 56,
        sourcePackage = "com.example.fictional",
        sourceName = "샘플 건강 앱",
        sourceUpdatedAtEpochMillis = null,
        syncedAtEpochMillis = 300,
        sourceDeleted = false,
    )
}
