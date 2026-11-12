package com.example.data.preferences

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.data.model.ThemePreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private val Context.melloDataStore: DataStore<Preferences> by preferencesDataStore(name = "mello_preferences")

@Immutable
data class MelloUserPreferences(
    val themePreset: ThemePreset = ThemePreset.SOFT_LAVENDER,
    val notificationsEnabled: Boolean = true,
    val soundEffectsEnabled: Boolean = true,
    val reducedMotion: Boolean = false,
    val hasCompletedOnboarding: Boolean = false,
    val customDisplayName: String = "",
    val customCategories: List<String> = emptyList()
)

class MelloPreferencesRepository(private val context: Context) {

    private val appContext = context.applicationContext

    val preferencesFlow: Flow<MelloUserPreferences> = appContext.melloDataStore.data
        .map { prefs ->
            val rawCategories = prefs[KEY_CUSTOM_CATEGORIES].orEmpty()
            val parsedCategories = if (rawCategories.isEmpty()) {
                emptyList()
            } else {
                rawCategories
                    .split("|")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .distinct()
            }

            MelloUserPreferences(
                themePreset = ThemePreset.fromId(prefs[KEY_THEME] ?: ThemePreset.SOFT_LAVENDER.id),
                notificationsEnabled = prefs[KEY_NOTIFICATIONS] ?: true,
                soundEffectsEnabled = prefs[KEY_SOUNDS] ?: true,
                reducedMotion = prefs[KEY_REDUCED_MOTION] ?: false,
                hasCompletedOnboarding = prefs[KEY_ONBOARDING_DONE] ?: false,
                customDisplayName = prefs[KEY_CUSTOM_DISPLAY_NAME].orEmpty(),
                customCategories = parsedCategories
            )
        }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)

    suspend fun setThemePreset(preset: ThemePreset) {
        appContext.melloDataStore.edit { prefs ->
            prefs[KEY_THEME] = preset.id
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        appContext.melloDataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS] = enabled
        }
    }

    suspend fun setSoundEffectsEnabled(enabled: Boolean) {
        appContext.melloDataStore.edit { prefs ->
            prefs[KEY_SOUNDS] = enabled
        }
    }

    suspend fun setReducedMotion(enabled: Boolean) {
        appContext.melloDataStore.edit { prefs ->
            prefs[KEY_REDUCED_MOTION] = enabled
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        appContext.melloDataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_DONE] = completed
        }
    }

    suspend fun setCustomDisplayName(name: String) {
        appContext.melloDataStore.edit { prefs ->
            prefs[KEY_CUSTOM_DISPLAY_NAME] = name.trim().take(50)
        }
    }

    suspend fun addCustomCategory(category: String) {
        val clean = category.trim().replace("|", "").take(30)
        if (clean.isBlank()) return
        appContext.melloDataStore.edit { prefs ->
            val current = prefs[KEY_CUSTOM_CATEGORIES].orEmpty()
                .split("|")
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (current.none { it.equals(clean, ignoreCase = true) }) {
                val updated = (current + clean).takeLast(12)
                prefs[KEY_CUSTOM_CATEGORIES] = updated.joinToString("|")
            }
        }
    }

    private companion object {
        val KEY_THEME = stringPreferencesKey("theme_preset")
        val KEY_NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val KEY_SOUNDS = booleanPreferencesKey("sound_effects_enabled")
        val KEY_REDUCED_MOTION = booleanPreferencesKey("reduced_motion")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_completed")
        val KEY_CUSTOM_DISPLAY_NAME = stringPreferencesKey("custom_display_name")
        val KEY_CUSTOM_CATEGORIES = stringPreferencesKey("custom_categories")
    }
}
