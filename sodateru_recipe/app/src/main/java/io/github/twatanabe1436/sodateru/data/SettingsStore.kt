package io.github.twatanabe1436.sodateru.data

import android.content.Context
import io.github.twatanabe1436.sodateru.core.DoughTemperature
import io.github.twatanabe1436.sodateru.core.claude.ClaudeRecipeReader
import io.github.twatanabe1436.sodateru.core.model.MixingMethod
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class OcrEngine(val label: String) {
    DEVICE("端末内（無料・オフライン）"),
    CLAUDE("Claude（高精度・手書き向き）"),
}

data class Settings(
    val ocrEngine: OcrEngine = OcrEngine.DEVICE,
    val claudeModel: String = ClaudeRecipeReader.DEFAULT_MODEL,
    val targetDoughTemp: Double = DoughTemperature.DEFAULT_TARGET,
    val mixingMethod: MixingMethod = MixingMethod.HAND,
    val lastBackupAt: Long = 0,
    val lastRoomTemp: Double? = null,
    val lastHumidity: Double? = null,
)

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<Settings> = _settings.asStateFlow()
    val current: Settings get() = _settings.value

    private fun read(): Settings {
        val d = Settings()
        fun double(key: String): Double? = if (prefs.contains(key)) prefs.getFloat(key, 0f).toDouble() else null
        return Settings(
            ocrEngine = OcrEngine.entries.firstOrNull { it.name == prefs.getString("ocrEngine", null) } ?: d.ocrEngine,
            claudeModel = prefs.getString("claudeModel", null) ?: d.claudeModel,
            targetDoughTemp = double("targetDoughTemp") ?: d.targetDoughTemp,
            mixingMethod = MixingMethod.entries.firstOrNull { it.name == prefs.getString("mixingMethod", null) } ?: d.mixingMethod,
            lastBackupAt = prefs.getLong("lastBackupAt", 0),
            lastRoomTemp = double("lastRoomTemp"),
            lastHumidity = double("lastHumidity"),
        )
    }

    fun update(transform: (Settings) -> Settings) {
        val s = transform(_settings.value)
        prefs.edit().apply {
            putString("ocrEngine", s.ocrEngine.name)
            putString("claudeModel", s.claudeModel)
            putFloat("targetDoughTemp", s.targetDoughTemp.toFloat())
            putString("mixingMethod", s.mixingMethod.name)
            putLong("lastBackupAt", s.lastBackupAt)
            if (s.lastRoomTemp != null) putFloat("lastRoomTemp", s.lastRoomTemp.toFloat()) else remove("lastRoomTemp")
            if (s.lastHumidity != null) putFloat("lastHumidity", s.lastHumidity.toFloat()) else remove("lastHumidity")
        }.apply()
        _settings.value = s
    }
}
