package ai.orkk.shoelog.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ShoeEntity::class,
        ExerciseEntity::class,
        ExerciseShoeAssignmentEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class ShoeLogDatabase : RoomDatabase() {
    abstract fun shoeLogDao(): ShoeLogDao
}
