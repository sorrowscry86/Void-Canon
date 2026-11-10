package org.voidcat.voidcanon.ai

import android.content.Context

/** Device permissions and rendering preferences, deliberately absent from portable archives. */
@android.annotation.SuppressLint("ApplySharedPref", "UseKtx")
class ArtworkPreferences(context: Context) {
    private val prefs = context.getSharedPreferences("automatic-artwork", Context.MODE_PRIVATE)
    fun enabled(sceneId: String) = prefs.getBoolean("enabled:$sceneId", false)
    fun authorized(sceneId: String) = prefs.getBoolean("authorized:$sceneId", false)
    fun illustrated(sceneId: String) = prefs.getBoolean("illustrated:$sceneId", true)
    fun brief(sceneId: String) = prefs.getString("brief:$sceneId", "").orEmpty()
    fun style(worldId: String) = prefs.getString("style:$worldId", "").orEmpty()
    fun blockedDay() = prefs.getString("blocked-day", "").orEmpty()
    fun block(day: String) { check(prefs.edit().putString("blocked-day", day).commit()) }
    fun configure(sceneId: String, worldId: String, enabled: Boolean, brief: String, style: String) {
        require(brief.length <= 800 && style.length <= 300)
        check(prefs.edit().putBoolean("authorized:$sceneId", enabled || authorized(sceneId)).putBoolean("enabled:$sceneId", enabled).putString("brief:$sceneId", brief.trim())
            .putString("style:$worldId", style.trim()).commit()) { "Could not save artwork settings." }
    }
    fun view(sceneId: String, illustrated: Boolean) { check(prefs.edit().putBoolean("illustrated:$sceneId", illustrated).commit()) }
    fun pause(sceneId: String) { check(prefs.edit().putBoolean("enabled:$sceneId", false).commit()) }
    fun resetPermissions() { check(prefs.edit().clear().commit()) }
}
