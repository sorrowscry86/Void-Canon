package org.voidcat.voidcanon.ai

import android.content.Context
import android.annotation.SuppressLint
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class AiConfiguration(val rpModel: String = "", val visionModel: String = "", val freeOnly: Boolean = true, val consent: Boolean = false)

/** Credentials never enter Room, portable snapshots, logs, or the APK. */
// Explicit synchronous commits report credential-write failures; callers run them on Dispatchers.IO.
@SuppressLint("UseKtx", "ApplySharedPref")
class OpenRouterSettings(context: Context) {
    private val prefs = context.getSharedPreferences("openrouter-private", Context.MODE_PRIVATE)
    fun configuration() = AiConfiguration(prefs.getString("rpModel", "").orEmpty(), prefs.getString("visionModel", "").orEmpty(), prefs.getBoolean("freeOnly", true), prefs.getBoolean("consent", false))
    fun save(configuration: AiConfiguration, newKey: String) {
        val editor = prefs.edit().putString("rpModel", configuration.rpModel).putString("visionModel", configuration.visionModel)
            .putBoolean("freeOnly", configuration.freeOnly).putBoolean("consent", configuration.consent)
        if (newKey.isNotBlank()) {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.doFinal(newKey.trim().toByteArray())
            editor.putString("secret", Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        }
        check(editor.commit()) { "Could not save AI settings on this device." }
    }
    fun clearKey() { check(prefs.edit().remove("secret").remove("iv").commit()) { "Could not remove the key from this device." } }
    fun hasKey(): Boolean = prefs.contains("secret")
    fun readKey(): String { return try {
        val encoded = prefs.getString("secret", null) ?: return ""
        val iv = Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        String(cipher.doFinal(Base64.decode(encoded, Base64.NO_WRAP)))
    } catch (_: Exception) { "" } }
    fun models(): List<RouterModel> = runCatching { Json.decodeFromString<List<RouterModel>>(prefs.getString("catalog", "[]").orEmpty()) }.getOrDefault(emptyList())
    fun catalogTime(): Long = prefs.getLong("catalogTime", 0)
    fun saveModels(models: List<RouterModel>) {
        prefs.edit().putString("catalog", Json.encodeToString(models)).putLong("catalogTime", System.currentTimeMillis()).apply()
    }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("void-canon-openrouter", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("void-canon-openrouter", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
