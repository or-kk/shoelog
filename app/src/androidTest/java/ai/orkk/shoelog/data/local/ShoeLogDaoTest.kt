package ai.orkk.shoelog.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import ai.orkk.shoelog.data.repository.ShoeDraft
import ai.orkk.shoelog.data.repository.ShoeRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoeLogDaoTest {
    private lateinit var database: ShoeLogDatabase
    private lateinit var dao: ShoeLogDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, ShoeLogDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.shoeLogDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun mileageIncludesInitialDistanceAndAssignedRun() = runTest {
        val shoeId = dao.insertShoe(shoe(initialMeters = 10_000))
        dao.upsertExercises(listOf(exercise(id = "run:1", distanceMeters = 5_000)))
        dao.assignShoe("run:1", shoeId, automatic = false, assignedAtEpochMillis = 10)

        val row = dao.observeShoes(includeRetired = false).first().single()

        assertEquals(15_000, row.totalMeters)
    }

    @Test
    fun reassignmentMovesMileageBetweenShoes() = runTest {
        val firstShoe = dao.insertShoe(shoe(initialMeters = 10_000, model = "First"))
        val secondShoe = dao.insertShoe(shoe(initialMeters = 0, model = "Second"))
        dao.upsertExercises(listOf(exercise(id = "run:1", distanceMeters = 5_000)))
        dao.assignShoe("run:1", firstShoe, automatic = false, assignedAtEpochMillis = 10)

        dao.assignShoe("run:1", secondShoe, automatic = false, assignedAtEpochMillis = 20)

        val rows = dao.observeShoes(includeRetired = false).first().associateBy { it.shoe.model }
        assertEquals(10_000, rows.getValue("First").totalMeters)
        assertEquals(5_000, rows.getValue("Second").totalMeters)
    }

    @Test
    fun deletingShoePreservesExerciseAndClearsAssignment() = runTest {
        val shoeId = dao.insertShoe(shoe())
        dao.upsertExercises(listOf(exercise(id = "run:1")))
        dao.assignShoe("run:1", shoeId, automatic = false, assignedAtEpochMillis = 10)

        dao.deleteShoePreservingExercises(shoeId)

        assertEquals(1, dao.exerciseCount())
        assertNull(dao.assignmentFor("run:1"))
    }

    @Test
    fun sameHealthConnectIdIsStoredOnce() = runTest {
        dao.upsertExercises(listOf(exercise(id = "health-connect-id")))
        dao.upsertExercises(listOf(exercise(id = "health-connect-id", distanceMeters = 8_000)))

        assertEquals(1, dao.exerciseCount())
        assertEquals(8_000L, dao.exerciseById("health-connect-id")?.distanceMeters)
    }

    @Test
    fun repositorySavesWonPrices() = runTest {
        val repository = ShoeRepository(
            dao = dao,
            clock = Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC),
        )

        val shoeId = repository.save(
            shoeId = null,
            draft = ShoeDraft(
                brand = "Fictional",
                model = "Price Test",
                purchasePriceWon = 129_000,
                listPriceWon = 169_000,
            ),
        )

        val stored = requireNotNull(dao.shoeById(shoeId))
        assertEquals(129_000L, stored.purchasePriceWon)
        assertEquals(169_000L, stored.listPriceWon)
    }

    @Test
    fun newlyRegisteredShoeCannotStartRetired() = runTest {
        val repository = ShoeRepository(
            dao = dao,
            clock = Clock.fixed(Instant.parse("2026-09-02T00:00:00Z"), ZoneOffset.UTC),
        )

        val shoeId = repository.save(
            shoeId = null,
            draft = ShoeDraft(
                brand = "Fictional",
                model = "Visible Runner",
                retired = true,
            ),
        )

        val activeShoeIds = repository.observeShoes().first().map { it.id }
        assertEquals(listOf(shoeId), activeShoeIds)
    }

    private fun shoe(
        initialMeters: Long = 0,
        model: String = "Runner",
    ) = ShoeEntity(
        brand = "Fictional",
        model = model,
        nickname = "",
        color = "",
        purchaseDateEpochDay = null,
        startDateEpochDay = null,
        initialMeters = initialMeters,
        targetMeters = 500_000,
        photoUri = null,
        retired = false,
        defaultShoe = false,
        createdAtEpochMillis = 1,
        updatedAtEpochMillis = 1,
    )

    private fun exercise(
        id: String,
        distanceMeters: Long? = 5_000,
    ) = ExerciseEntity(
        id = id,
        fallbackKey = "fallback:$id",
        startEpochMillis = 100,
        endEpochMillis = 200,
        distanceMeters = distanceMeters,
        exerciseType = 56,
        sourcePackage = "com.example.fictional",
        sourceName = "샘플 건강 앱",
        sourceUpdatedAtEpochMillis = null,
        syncedAtEpochMillis = 300,
        sourceDeleted = false,
    )
}
