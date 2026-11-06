package org.voidcat.voidcanon.ai

import android.text.Html
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class WikiReference(val title: String, val url: String, val revision: String, val text: String, val truncated: Boolean)

/** One explicitly selected article, no crawl, credentials, redirects, or automatic retry. */
class WikiReferenceClient(private val http: OkHttpClient = OkHttpClient.Builder()
    .followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false)
    .callTimeout(30, TimeUnit.SECONDS).build()) {
    suspend fun fetch(url: String): WikiReference {
        val endpoint = endpoint(url)
        val body = read(Request.Builder().url(endpoint).header("User-Agent", "VoidCanon/0.5.0 (user-selected reference import)").get().build())
        return parse(body, url)
    }

    private suspend fun read(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(IOException("Wiki unavailable. Paste the reference text instead; your draft is safe."))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val text = response.use {
                        require(it.isSuccessful) { "Wiki access failed (HTTP ${it.code}). Paste the page text instead." }
                        val source = requireNotNull(it.body).source()
                        require(!source.request(1024L * 1024 + 1)) { "This article is too large. Paste a selected excerpt instead." }
                        source.readUtf8()
                    }
                    if (continuation.isActive) continuation.resume(text)
                } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
            }
        })
    }

    companion object {
        fun endpoint(url: String): HttpUrl {
            require(url.length <= 2000)
            val page = requireNotNull(url.trim().toHttpUrlOrNull()) { "Enter a Fandom article URL." }
            require(page.scheme == "https" && page.port == 443 && page.username.isEmpty() && page.password.isEmpty() &&
                (page.host.endsWith(".fandom.com") || page.host.endsWith(".wikia.com"))) { "Use an HTTPS Fandom/Wikia article URL, or paste reference text." }
            val segments = page.pathSegments
            val wiki = segments.indexOf("wiki")
            require(wiki >= 0 && wiki < segments.lastIndex && wiki <= 1) { "Choose an article URL containing /wiki/Page_title." }
            val title = segments.drop(wiki + 1).joinToString("/")
            require(title.isNotBlank())
            return page.newBuilder().encodedPath("/").query(null).fragment(null).apply {
                segments.take(wiki).forEach { addPathSegment(it) }
                addPathSegment("api.php")
                addQueryParameter("action", "parse"); addQueryParameter("page", title)
                addQueryParameter("prop", "text|revid"); addQueryParameter("format", "json"); addQueryParameter("formatversion", "2")
            }.build()
        }

        fun parse(body: String, url: String): WikiReference {
            val root = try { Json.parseToJsonElement(body).jsonObject } catch (_: Exception) { error("Wiki returned unreadable content. Paste the reference text instead.") }
            val page = root["parse"] as? JsonObject ?: error("This page could not be read. Paste the reference text instead.")
            val title = page["title"]?.jsonPrimitive?.contentOrNull.orEmpty().take(500)
            val revision = page["revid"]?.jsonPrimitive?.contentOrNull.orEmpty().take(200)
            val html = (page["text"] as? JsonPrimitive)?.contentOrNull
                ?: (page["text"] as? JsonObject)?.get("*")?.jsonPrimitive?.contentOrNull ?: error("No article text was returned.")
            val cleaned = html.replace(Regex("<(script|style)\\b[^>]*>.*?</\\1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)), "")
            val text = Html.fromHtml(cleaned, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            require(text.isNotBlank()) { "No article text was returned. Paste an excerpt instead." }
            return WikiReference(title, url.trim(), revision, text.take(12000), text.length > 12000)
        }
    }
}
