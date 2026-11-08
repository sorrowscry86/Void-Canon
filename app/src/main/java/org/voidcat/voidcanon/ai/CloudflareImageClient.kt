package org.voidcat.voidcanon.ai

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Base64
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Serializable
data class ImageConnection(val endpoint: String, val accessToken: String) {
    override fun toString() = "ImageConnection(endpoint=$endpoint, accessToken=<redacted>)"
    fun validate() {
        val url = endpoint.toHttpUrl()
        require(url.isHttps && url.username.isEmpty() && url.password.isEmpty() && url.query == null && url.fragment == null) { "Use an HTTPS image service URL without credentials or query parameters." }
        require(accessToken.length in 24..256 && accessToken.none { it.isWhitespace() }) { "Import a valid image connection file." }
    }
}

enum class ImageSize(val title: String, val wire: String, val width: Int, val height: Int) {
    Portrait("Portrait · 512 × 512", "portrait", 512, 512),
    Large("Large portrait · 1024 × 1024", "large", 1024, 1024),
    Background("Background · 1024 × 576", "background", 1024, 576),
}

data class GeneratedImage(val bytes: ByteArray, val model: String, val requestId: String, val reservedNeurons: Int)
data class ImageServiceStatus(val day: String, val remaining: Int)
class ImageServiceException(val status: Int, message: String) : IOException(message)

/** One explicit call. Never retries, follows redirects, or sends a Cloudflare account token. */
class CloudflareImageClient(
    private val http: OkHttpClient = OkHttpClient.Builder().retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).readTimeout(120, TimeUnit.SECONDS).callTimeout(120, TimeUnit.SECONDS).build(),
) {
    suspend fun status(connection: ImageConnection): ImageServiceStatus {
        connection.validate()
        val result = Json.parseToJsonElement(request(Request.Builder().url(connection.endpoint.trimEnd('/') + "/v1/status")
            .header("Authorization", "Bearer ${connection.accessToken}").build())).jsonObject
        val day = result.getValue("day").jsonPrimitive.content
        val limit = result.getValue("limit").jsonPrimitive.int
        val reserved = result.getValue("reserved").jsonPrimitive.int
        require(day.matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}")) && limit in 0..5000 && reserved >= 0 && result["model"]?.jsonPrimitive?.content == MODEL) { "Unrecognized image allowance response." }
        return ImageServiceStatus(day, (limit - reserved).coerceAtLeast(0))
    }

    suspend fun generate(connection: ImageConnection, requestId: String, prompt: String, size: ImageSize, reference: ByteArray?): GeneratedImage {
        connection.validate()
        require(prompt.isNotBlank() && prompt.length <= 2000) { "Use a prompt of 1–2,000 characters." }
        require(reference == null || reference.size <= 480000) { "Choose a smaller reference image." }
        val body = buildJsonObject {
            put("requestId", requestId); put("prompt", prompt.trim()); put("size", size.wire)
            reference?.let { put("reference", Base64.getEncoder().encodeToString(it)) }
        }
        val url = connection.endpoint.trimEnd('/') + "/v1/images"
        val text = request(Request.Builder().url(url).header("Authorization", "Bearer ${connection.accessToken}")
            .post(body.toString().toRequestBody("application/json".toMediaType())).build())
        val result = try { Json.parseToJsonElement(text).jsonObject } catch (_: Exception) { error("The image service returned an unreadable response.") }
        require(result["requestId"]?.jsonPrimitive?.content == requestId) { "The image response did not match this request." }
        val model = result["model"]?.jsonPrimitive?.content.orEmpty()
        require(model == MODEL) { "The image service returned an unexpected model." }
        val encoded = result["image"]?.jsonPrimitive?.content.orEmpty()
        require(encoded.length in 1..12000000) { "The image service returned no usable image." }
        val bytes = try { Base64.getDecoder().decode(encoded) } catch (_: IllegalArgumentException) { error("The generated image was unreadable.") }
        return GeneratedImage(bytes, model, requestId, result["reservedNeurons"]?.jsonPrimitive?.intOrNull ?: 0)
    }

    private suspend fun request(request: Request): String = suspendCancellableCoroutine { continuation ->
        val call = http.newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(IOException("Image generation is unavailable or timed out. No automatic retry was made; your local images are safe."))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val text = response.use {
                        if (!response.isSuccessful) throw ImageServiceException(response.code, when (response.code) {
                            401, 403 -> "Image access was rejected. Import your connection file again."
                            429 -> "The image service's daily development budget or Cloudflare limit was reached. Wait before generating again."
                            409 -> "This image request was already submitted. Check the preview before generating again."
                            400, 413 -> "The prompt or reference was rejected. Try a shorter prompt or a smaller reference."
                            else -> "Cloudflare could not finish this image (HTTP ${response.code}). No automatic retry was made."
                        })
                        val body = requireNotNull(response.body) { "The image service returned an empty response." }
                        require(body.contentLength() <= 12500000) { "The image response was too large." }
                        val source = body.source()
                        require(!source.request(12500001)) { "The image response was too large." }
                        source.readUtf8()
                    }
                    if (continuation.isActive) continuation.resume(text)
                } catch (e: Exception) { if (continuation.isActive) continuation.resumeWithException(e) }
            }
        })
    }

    companion object { const val MODEL = "@cf/black-forest-labs/flux-2-klein-4b" }
}
