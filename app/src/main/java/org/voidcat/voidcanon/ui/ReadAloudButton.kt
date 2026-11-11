package org.voidcat.voidcanon.ui

import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Locale
import java.util.UUID

internal fun speechChunks(text: String, limit: Int): List<String> {
    require(limit >= 2)
    val chunks = mutableListOf<String>()
    var remaining = text
    while (remaining.isNotEmpty()) {
        var end = minOf(limit, remaining.length)
        if (end < remaining.length) {
            val space = remaining.lastIndexOf(' ', end - 1)
            if (space >= end / 2) end = space + 1
            if (end < remaining.length && Character.isHighSurrogate(remaining[end - 1])) end--
        }
        chunks += remaining.take(end); remaining = remaining.drop(end)
    }
    return chunks
}

/** Explicit playback only; requests an installed non-network voice from the Android engine. */
private class SceneReader(context: Context, private val report: (String) -> Unit) {
    private val app = context.applicationContext
    private var engine: TextToSpeech? = null
    private var ready = false
    private var closed = false
    private var pending: String? = null
    private var run: String? = null
    private var last: String? = null
    val speaking = MutableStateFlow(false)
    private val audio = app.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener { if (it < 0) stop() }.build()

    @Synchronized fun toggle(text: String) {
        if (closed) return
        if (speaking.value) { stop(); return }
        if (text.isBlank()) return
        pending = text; run = UUID.randomUUID().toString(); speaking.value = true
        if (ready) playPending() else if (engine == null) {
            try {
                engine = TextToSpeech(app) { status ->
                    // Some engines report initialization failure synchronously. Post until
                    // the constructor's engine assignment has completed before handling it.
                    android.os.Handler(android.os.Looper.getMainLooper()).post { initialized(status) }
                }
            } catch (_: Exception) { engine = null; fail("Read Aloud needs an installed Android text-to-speech engine and voice.") }
        }
    }
    @Synchronized private fun initialized(status: Int) {
        if (closed || engine == null) return
        if (status != TextToSpeech.SUCCESS) {
            engine?.shutdown(); engine = null; ready = false
            if (pending != null) fail("Read Aloud needs an installed Android text-to-speech engine and voice.")
            return
        }
        ready = true
        engine?.setAudioAttributes(attributes)
        engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) = Unit
            override fun onDone(id: String?) = completed(id)
            @Deprecated("Android callback") override fun onError(id: String?) = errored(id)
        })
        if (pending != null) playPending()
    }
    @Synchronized private fun completed(id: String?) { if (id != null && id == last) stop() }
    @Synchronized private fun errored(id: String?) { if (id != null && run?.let { id.startsWith(it) } == true) fail("Read Aloud could not finish. Check Android speech settings.") }
    @Synchronized private fun playPending() {
        val tts = engine ?: return
        val text = pending ?: return
        pending = null
        val voice = tts.voices?.filter { !it.isNetworkConnectionRequired && it.locale.language == Locale.getDefault().language }
            ?.maxByOrNull { it.quality }
        if (voice == null || tts.setVoice(voice) != TextToSpeech.SUCCESS) { fail("Install an offline voice for your device language in Android text-to-speech settings."); return }
        if (audio.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) { fail("Audio is busy. Try Read Aloud again."); return }
        val chunks = speechChunks(text, TextToSpeech.getMaxSpeechInputLength())
        val prefix = run ?: return
        last = "$prefix-${chunks.lastIndex}"
        chunks.forEachIndexed { index, chunk ->
            if (tts.speak(chunk, if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, null, "$prefix-$index") == TextToSpeech.ERROR) {
                fail("Read Aloud could not start. Check Android speech settings."); return
            }
        }
    }
    private fun fail(message: String) { stop(); report(message) }
    @Synchronized fun stop() { pending = null; run = null; last = null; speaking.value = false; engine?.stop(); audio.abandonAudioFocusRequest(focus) }
    @Synchronized fun close() { closed = true; stop(); ready = false; engine?.shutdown(); engine = null }
}

@Composable
fun ReadAloudButton(sceneId: String, messageId: String?, text: String, report: (String) -> Unit) {
    val context = LocalContext.current
    val latestReport by rememberUpdatedState(report)
    val reader = remember(sceneId) { SceneReader(context) { latestReport(it) } }
    val speaking by reader.speaking.collectAsState()
    val owner = LocalLifecycleOwner.current
    DisposableEffect(reader, owner) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_STOP) reader.stop() }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer); reader.close() }
    }
    LaunchedEffect(messageId, text) { reader.stop() }
    PlayTool(if (speaking) "Stop reading" else "Read aloud", if (speaking) CanonIcons.Stop else CanonIcons.Speaker,
        enabled = text.isNotBlank(), active = speaking) { reader.toggle(text) }
}
