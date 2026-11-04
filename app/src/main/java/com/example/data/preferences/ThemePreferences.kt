package com.example.data.preferences

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppearanceMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class DesignTheme {
    MATERIAL_3,
    FROSTED_GLASS
}

data class AppPreferences(
    val appearanceMode: AppearanceMode = AppearanceMode.SYSTEM,
    val isAmoled: Boolean = false,
    val defaultCurrency: String = "৳",
    val customDevAvatarUri: String? = null,
    val designTheme: DesignTheme = DesignTheme.MATERIAL_3
)

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("mitmat_preferences", Context.MODE_PRIVATE)

    private val _preferencesFlow = MutableStateFlow(loadPreferences())
    val preferencesFlow: StateFlow<AppPreferences> = _preferencesFlow.asStateFlow()

    private fun loadPreferences(): AppPreferences {
        val appearanceStr = prefs.getString("appearance_mode", AppearanceMode.SYSTEM.name)
        val isAmoled = prefs.getBoolean("is_amoled", false)
        val currency = prefs.getString("default_currency", "৳") ?: "৳"
        val customAvatar = prefs.getString("custom_dev_avatar", null)
        val designThemeStr = prefs.getString("design_theme", DesignTheme.MATERIAL_3.name)

        val appearanceMode = try {
            AppearanceMode.valueOf(appearanceStr ?: AppearanceMode.SYSTEM.name)
        } catch (_: Exception) {
            AppearanceMode.SYSTEM
        }

        val designTheme = try {
            DesignTheme.valueOf(designThemeStr ?: DesignTheme.MATERIAL_3.name)
        } catch (_: Exception) {
            DesignTheme.MATERIAL_3
        }

        return AppPreferences(
            appearanceMode = appearanceMode,
            isAmoled = isAmoled,
            defaultCurrency = currency,
            customDevAvatarUri = customAvatar,
            designTheme = designTheme
        )
    }

    fun setAppearanceMode(mode: AppearanceMode) {
        prefs.edit().putString("appearance_mode", mode.name).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(appearanceMode = mode)
    }

    fun setIsAmoled(isAmoled: Boolean) {
        prefs.edit().putBoolean("is_amoled", isAmoled).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(isAmoled = isAmoled)
    }

    fun setDefaultCurrency(currency: String) {
        prefs.edit().putString("default_currency", currency).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(defaultCurrency = currency)
    }

    fun setCustomDevAvatar(uri: String?) {
        prefs.edit().putString("custom_dev_avatar", uri).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(customDevAvatarUri = uri)
    }

    fun setDesignTheme(theme: DesignTheme) {
        prefs.edit().putString("design_theme", theme.name).apply()
        _preferencesFlow.value = _preferencesFlow.value.copy(designTheme = theme)
    }
}
