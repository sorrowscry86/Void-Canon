package org.voidcat.voidcanon.ai

import android.content.Context

/** Device-local text provider. Never participates in automatic fallback or image requests. */
@android.annotation.SuppressLint("UseKtx", "ApplySharedPref")
class DeepSeekBackup(context: Context) {
    val settings = OpenRouterSettings(context, "deepseek-private", "void-canon-deepseek")
    private val prefs = context.getSharedPreferences("deepseek-private", Context.MODE_PRIVATE)
    fun active() = prefs.getBoolean("active", false)
    fun save(model: String, key: String, consent: Boolean, active: Boolean) {
        require(!key.trim().contains('\n') && !key.trim().contains('\r')) { "Enter a single-line DeepSeek API key." }
        require(!active || consent) { "Allow DeepSeek API charges and story sharing before activating the backup." }
        require(!active || model.trim().matches(Regex("[A-Za-z0-9/_.:-]{1,200}"))) { "Enter a model ID from your DeepSeek account." }
        require(!active || key.isNotBlank() || settings.hasKey()) { "Enter your separate DeepSeek API key." }
        settings.save(AiConfiguration(rpModel = model.trim(), freeOnly = false, consent = consent), key)
        check(prefs.edit().putBoolean("active", active).commit()) { "Could not save the provider selection." }
    }
    fun model(): RouterModel {
        val id = settings.configuration().rpModel
        // Application limits, not advertised provider capabilities/prices.
        return RouterModel(id, id, 32768, false, false, "unknown", "unknown", 4096)
    }
}
