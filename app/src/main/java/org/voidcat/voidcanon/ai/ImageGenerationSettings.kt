package org.voidcat.voidcanon.ai

import android.annotation.SuppressLint
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Only a scoped image-service access code is stored here; no Cloudflare account credential. */
@SuppressLint("ApplySharedPref", "UseKtx")
class ImageGenerationSettings(context: Context) {
    private val prefs = context.getSharedPreferences("image-service-private", Context.MODE_PRIVATE)
    fun endpoint() = prefs.getString("endpoint", "").orEmpty()
    fun connected() = prefs.contains("secret") && endpoint().isNotBlank()
    fun save(connection: ImageConnection) {
        connection.validate()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(connection.accessToken.toByteArray())
        check(prefs.edit().putString("endpoint", connection.endpoint.trimEnd('/'))
            .putString("secret", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP)).commit()) { "Could not save the image connection." }
    }
    fun read(): ImageConnection {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(prefs.getString("iv", ""), Base64.NO_WRAP)))
            }
            val secret = String(cipher.doFinal(Base64.decode(prefs.getString("secret", ""), Base64.NO_WRAP)))
            return ImageConnection(endpoint(), secret).also { it.validate() }
        } catch (_: Exception) { error("Import your image connection file on this device first.") }
    }
    fun clear() { check(prefs.edit().clear().commit()) { "Could not disconnect image generation." } }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("void-canon-image-service", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("void-canon-image-service", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
}
