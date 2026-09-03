package ai.orkk.shoelog

import android.app.Application
import androidx.room.Room
import ai.orkk.shoelog.data.health.AndroidHealthConnectDataSource
import ai.orkk.shoelog.data.health.HealthConnectDataSource
import ai.orkk.shoelog.data.local.ShoeLogDatabase
import ai.orkk.shoelog.data.local.MIGRATION_1_2
import ai.orkk.shoelog.data.local.MIGRATION_2_3
import ai.orkk.shoelog.data.preferences.SettingsRepository
import ai.orkk.shoelog.data.repository.ExerciseRepository
import ai.orkk.shoelog.data.repository.RoomExerciseSyncStore
import ai.orkk.shoelog.data.repository.RoomSampleDataStore
import ai.orkk.shoelog.data.repository.SampleDataRepository
import ai.orkk.shoelog.data.repository.ShoeRepository
import ai.orkk.shoelog.data.repository.SyncRepository
import ai.orkk.shoelog.data.repository.SyncSettings
import ai.orkk.shoelog.notification.RunNotificationManager
import ai.orkk.shoelog.widget.MileageRankingWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ShoeLogApplication : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            container.shoeRepository.observeShoes()
                .map { shoes ->
                    shoes.map { shoe ->
                        MileageWidgetSnapshot(
                            id = shoe.id,
                            nickname = shoe.nickname,
                            brand = shoe.brand,
                            model = shoe.model,
                            totalMeters = shoe.mileage.totalMeters,
                            targetMeters = shoe.targetMeters,
                        )
                    }
                }
                .distinctUntilChanged()
                .catch { /* Periodic widget refresh remains available if Room cannot be read. */ }
                .collect { MileageRankingWidget().updateAll(this@ShoeLogApplication) }
        }
    }
}

private data class MileageWidgetSnapshot(
    val id: Long,
    val nickname: String,
    val brand: String,
    val model: String,
    val totalMeters: Long,
    val targetMeters: Long,
)

class AppContainer(application: Application) {
    private val database = Room.databaseBuilder(
        application,
        ShoeLogDatabase::class.java,
        "shoelog.db",
    ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
    private val dao = database.shoeLogDao()

    val settingsRepository = SettingsRepository(application)
    val shoeRepository = ShoeRepository(dao, defaultShoePreferences = settingsRepository)
    val exerciseRepository = ExerciseRepository(dao)
    val healthConnectDataSource: HealthConnectDataSource = AndroidHealthConnectDataSource(application)
    val notificationManager = RunNotificationManager(application)
    val syncRepository = SyncRepository(
        health = healthConnectDataSource,
        store = RoomExerciseSyncStore(dao),
        settings = object : SyncSettings {
            override suspend fun current() = settingsRepository.settings.first()
        },
        notifier = notificationManager,
    )
    val sampleDataRepository = SampleDataRepository(RoomSampleDataStore(dao))
}
