package org.voidcat.voidcanon.ai

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.math.BigDecimal
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Serializable
data class RouterModel(
    val id: String, val name: String, val contextLength: Int,
    val vision: Boolean, val free: Boolean, val promptPrice: String, val completionPrice: String,
    val maxCompletionTokens: Int? = null,
)

@Serializable
data class PromptMessage(val role: String, val content: String)

data class RouterResult(val text: String, val model: String, val usageJson: String)

/** No automatic retries, redirects, alternate models, or credential-bearing logging. */
class OpenRouterClient(
    private val baseUrl: String = "https://openrouter.ai/api/v1/",
    private val http: OkHttpClient = OkHttpClient.Builder().retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).callTimeout(90, TimeUnit.SECONDS).build(),
) {
    suspend fun models(): List<RouterModel> {
        val body = request(Request.Builder().url(baseUrl + "models").get().build(), 8 * 1024 * 1024)
        return parseModels(body)
    }

    suspend fun complete(key: String, model: String, messages: List<PromptMessage>, freeOnly: Boolean,
                         imageDataUrl: String? = null, maxTokens: Int = 1000): RouterResult {
        require(key.isNotBlank() && !key.contains('\n') && !key.contains('\r')) { "Enter your OpenRouter key in AI settings." }
        require(model.isNotBlank()) { "Choose a model in AI settings." }
        val body = buildJsonObject {
            put("model", model); put("stream", false); put("max_tokens", maxTokens); put("temperature", 0.7)
            putJsonObject("provider") {
                put("allow_fallbacks", false)
                if (freeOnly) putJsonObject("max_price") {
                    put("prompt", 0); put("completion", 0); put("request", 0); put("image", 0)
                }
            }
            putJsonArray("messages") {
                messages.forEachIndexed { index, message -> addJsonObject {
                    put("role", message.role)
                    if (imageDataUrl != null && index == messages.lastIndex) putJsonArray("content") {
                        addJsonObject { put("type", "text"); put("text", message.content) }
                        addJsonObject { put("type", "image_url"); putJsonObject("image_url") { put("url", imageDataUrl) } }
                    } else put("content", message.content)
                } }
            }
        }
        val response = request(Request.Builder().url(baseUrl + "chat/completions")
            .header("Authorization", "Bearer $key").header("X-OpenRouter-Title", "Void Canon")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build(), 4 * 1024 * 1024)
        val root = try { Json.parseToJsonElement(response).jsonObject } catch (_: Exception) { error("OpenRouter returned an unreadable response.") }
        root["error"]?.let {
            val code = (it as? JsonObject)?.get("code")?.jsonPrimitive?.intOrNull
            error(providerError(code))
        }
        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
        val text = choice?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
        require(choice?.get("finish_reason")?.jsonPrimitive?.contentOrNull != "length") {
            "The model used its $maxTokens-token output budget before finishing. Reasoning models may spend this budget thinking. Choose another model in AI settings and retry; no incomplete result was saved."
        }
        require(!text.isNullOrBlank() && text.length <= 20000) { "The model returned no usable text. Try a different model or a shorter scene." }
        return RouterResult(text, root["model"]?.jsonPrimitive?.contentOrNull ?: model, root["usage"]?.toString() ?: "{}")
    }

    private suspend fun request(request: Request, limit: Int): String = suspendCancellableCoroutine { continuation ->
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(IOException("OpenRouter is unavailable. Check your connection; your local work is safe."))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val text = response.use {
                        check(response.isSuccessful) {
                            providerError(response.code)
                        }
                        val body = requireNotNull(response.body) { "OpenRouter returned an empty response." }
                        require(body.contentLength() <= limit) { "OpenRouter's response was too large." }
                        val source = body.source()
                        require(!source.request(limit.toLong() + 1)) { "OpenRouter's response was too large." }
                        source.readUtf8()
                    }
                    if (continuation.isActive) continuation.resume(text)
                } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
            }
        })
    }

    companion object {
        private fun providerError(code: Int?): String = when (code) {
            401, 403 -> "OpenRouter rejected the key or model access. Check AI settings."
            402 -> "OpenRouter reports insufficient credits. No paid fallback was attempted."
            429 -> "OpenRouter's rate limit or quota was reached. Free models can have per-minute or daily limits. Wait before retrying, or choose another current free model in AI settings. No automatic retry or paid fallback was attempted."
            400, 404 -> "The model or request is unavailable. Refresh models and check the selected model's capabilities."
            null -> "The selected model could not complete the request. No fallback was attempted."
            else -> "OpenRouter could not complete this request (HTTP $code). Retry explicitly when ready."
        }

        /** Price values, not a ':free' suffix, decide whether a model is currently free. */
        fun parseModels(body: String): List<RouterModel> {
            val root = Json.parseToJsonElement(body).jsonObject
            return root["data"]?.jsonArray.orEmpty().mapNotNull { element -> runCatching {
                val item = element.jsonObject
                val endpoints = item["endpoints"] as? JsonArray
                if (endpoints != null && endpoints.none { it.jsonPrimitive.content == "/api/v1/chat/completions" }) return@runCatching null
                val arch = item["architecture"]?.jsonObject ?: return@runCatching null
                val inputs = arch["input_modalities"]?.jsonArray.orEmpty().map { it.jsonPrimitive.content }
                val outputs = arch["output_modalities"]?.jsonArray.orEmpty().map { it.jsonPrimitive.content }
                if ("text" !in inputs || "text" !in outputs) return@runCatching null
                val prices = item["pricing"]?.jsonObject ?: JsonObject(emptyMap())
                fun zero(key: String, required: Boolean): Boolean {
                    val value = prices[key]?.jsonPrimitive?.contentOrNull ?: return !required
                    return value.toBigDecimalOrNull()?.compareTo(BigDecimal.ZERO) == 0
                }
                val free = zero("prompt", true) && zero("completion", true) && zero("request", false) && zero("image", false)
                RouterModel(item.getValue("id").jsonPrimitive.content,
                    item["name"]?.jsonPrimitive?.contentOrNull ?: item.getValue("id").jsonPrimitive.content,
                    item["context_length"]?.jsonPrimitive?.intOrNull ?: 4096, "image" in inputs, free,
                    prices["prompt"]?.jsonPrimitive?.contentOrNull ?: "unknown", prices["completion"]?.jsonPrimitive?.contentOrNull ?: "unknown",
                    item["top_provider"]?.jsonObject?.get("max_completion_tokens")?.jsonPrimitive?.intOrNull?.takeIf { it > 0 })
            }.getOrNull() }.distinctBy { it.id }.sortedWith(compareByDescending<RouterModel> { it.free }
                .thenByDescending { it.id.endsWith(":free") || it.id == "openrouter/free" }.thenBy { it.name })
        }
    }
}
