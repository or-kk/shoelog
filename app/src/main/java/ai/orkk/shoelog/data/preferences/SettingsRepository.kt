package ai.orkk.shoelog.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.shoeLogSettings by preferencesDataStore(name = "shoelog_settings")

data class AppSettings(
    val autoAssignDefault: Boolean = false,
    val defaultShoeId: Long? = null,
    val historyRequested: Boolean = false,
    val sampleMode: Boolean = false,
)

interface DefaultShoePreferenceStore {
    suspend fun clearIfMatches(shoeId: Long)
}

class SettingsRepository(private val context: Context) : DefaultShoePreferenceStore {
    private object Keys {
        val autoAssignDefault = booleanPreferencesKey("auto_assign_default")
        val defaultShoeId = longPreferencesKey("default_shoe_id")
        val historyRequested = booleanPreferencesKey("history_requested")
        val sampleMode = booleanPreferencesKey("sample_mode")
    }

    val settings: Flow<AppSettings> = context.shoeLogSettings.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences())
            else throw error
        }
        .map { preferences ->
            AppSettings(
                autoAssignDefault = preferences[Keys.autoAssignDefault] ?: false,
                defaultShoeId = preferences[Keys.defaultShoeId],
                historyRequested = preferences[Keys.historyRequested] ?: false,
                sampleMode = preferences[Keys.sampleMode] ?: false,
            )
        }

    suspend fun setAutoAssignDefault(enabled: Boolean) {
        context.shoeLogSettings.edit { it[Keys.autoAssignDefault] = enabled }
    }

    suspend fun setDefaultShoeId(shoeId: Long?) {
        context.shoeLogSettings.edit { preferences ->
            if (shoeId == null) preferences.remove(Keys.defaultShoeId)
            else preferences[Keys.defaultShoeId] = shoeId
        }
    }

    override suspend fun clearIfMatches(shoeId: Long) {
        if (settings.first().defaultShoeId == shoeId) setDefaultShoeId(null)
    }

    suspend fun setHistoryRequested(enabled: Boolean) {
        context.shoeLogSettings.edit { it[Keys.historyRequested] = enabled }
    }

    suspend fun setSampleMode(enabled: Boolean) {
        context.shoeLogSettings.edit { it[Keys.sampleMode] = enabled }
    }
}
