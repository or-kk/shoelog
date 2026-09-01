package ai.orkk.shoelog.data.repository

import ai.orkk.shoelog.data.local.ExerciseEntity
import ai.orkk.shoelog.data.local.ShoeEntity
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleDataRepositoryTest {
    private val clock = Clock.fixed(Instant.parse("2026-09-01T00:00:00Z"), ZoneOffset.UTC)

    @Test
    fun installingTwiceIsIdempotent() = runTest {
        val store = InMemorySampleDataStore()
        val repository = SampleDataRepository(store, clock)

        repository.install()
        val firstShoeIds = store.shoes.map { it.id }
        val firstExerciseIds = store.exercises.map { it.id }
        repository.install()

        assertEquals(firstShoeIds, store.shoes.map { it.id })
        assertEquals(firstExerciseIds, store.exercises.map { it.id })
        assertEquals(4, store.shoes.size)
        assertEquals(5, store.exercises.size)
        assertTrue(store.exercises.any { it.distanceMeters == null })
        assertTrue(store.shoes.any { it.retired })
    }

    @Test
    fun samplesContainNoPersonalIdentityOrPrivateSource() = runTest {
        val store = InMemorySampleDataStore()
        SampleDataRepository(store, clock).install()

        val allText = buildList {
            store.shoes.forEach { addAll(listOf(it.brand, it.model, it.nickname, it.color)) }
            store.exercises.forEach { addAll(listOf(it.id, it.sourcePackage, it.sourceName)) }
        }.joinToString(" ").lowercase()

        assertFalse("tmdrl1026" in allText)
        assertFalse("ralph" in allText)
        assertFalse("greenit" in allText)
        assertTrue(store.exercises.all { it.id.startsWith("sample:") })
    }
}

private class InMemorySampleDataStore : SampleDataStore {
    val shoes = mutableListOf<ShoeEntity>()
    val exercises = mutableListOf<ExerciseEntity>()
    val assignments = mutableMapOf<String, Long>()

    override suspend fun clear() {
        shoes.clear()
        exercises.clear()
        assignments.clear()
    }

    override suspend fun insertShoe(shoe: ShoeEntity) {
        shoes += shoe
    }

    override suspend fun upsertExercise(exercise: ExerciseEntity) {
        exercises.removeAll { it.id == exercise.id }
        exercises += exercise
    }

    override suspend fun assign(exerciseId: String, shoeId: Long) {
        assignments[exerciseId] = shoeId
    }
}
