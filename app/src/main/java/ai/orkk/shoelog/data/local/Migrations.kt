package ai.orkk.shoelog.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shoes ADD COLUMN purchasePriceWon INTEGER")
        db.execSQL("ALTER TABLE shoes ADD COLUMN listPriceWon INTEGER")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE shoes ADD COLUMN categoryCode TEXT")
        db.execSQL("ALTER TABLE shoes ADD COLUMN purposeCodes TEXT NOT NULL DEFAULT ''")
    }
}
