package ai.orkk.shoelog.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ShoeLogMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        ShoeLogDatabase::class.java,
    )

    @Test
    fun migrationFrom1To2PreservesShoesWithEmptyPrices() {
        helper.createDatabase(TEST_DATABASE, 1).apply {
            execSQL(
                """
                INSERT INTO shoes (
                    id, brand, model, nickname, color, purchaseDateEpochDay,
                    startDateEpochDay, initialMeters, targetMeters, photoUri,
                    retired, defaultShoe, createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (
                    7, 'Fictional', 'Migration Runner', '', '', NULL,
                    NULL, 3000, 500000, NULL, 0, 0, 1, 1
                )
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            2,
            true,
            MIGRATION_1_2,
        )

        migrated.query(
            "SELECT brand, model, purchasePriceWon, listPriceWon FROM shoes WHERE id = 7",
        ).use { cursor ->
            assertTrue(cursor.moveToFirst())
            assertEquals("Fictional", cursor.getString(0))
            assertEquals("Migration Runner", cursor.getString(1))
            assertTrue(cursor.isNull(2))
            assertTrue(cursor.isNull(3))
        }
    }

    @Test
    fun migrationFrom2To3PreservesExistingDataAndAddsEmptyMetadata() {
        helper.createDatabase(TEST_DATABASE, 2).apply {
            execSQL(
                """
                INSERT INTO shoes (
                    id, brand, model, nickname, color, purchaseDateEpochDay,
                    startDateEpochDay, purchasePriceWon, listPriceWon, initialMeters,
                    targetMeters, photoUri, retired, defaultShoe,
                    createdAtEpochMillis, updatedAtEpochMillis
                ) VALUES (
                    7, 'Fictional', 'Migration Runner', '', '', NULL,
                    NULL, 129000, 169000, 3000, 500000, NULL, 0, 1, 1, 2
                )
                """.trimIndent(),
            )
            close()
        }

        val migrated = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            3,
            true,
            MIGRATION_2_3,
        )

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

    private companion object {
        const val TEST_DATABASE = "shoelog-migration-test"
    }
}
