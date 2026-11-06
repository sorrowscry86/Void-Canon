package org.voidcat.voidcanon.data

import android.graphics.BitmapFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import org.voidcat.voidcanon.ai.GeneratedImage
import org.voidcat.voidcanon.ai.ImageSize

@Serializable
data class GeneratedImageDraft(val requestId: String, val entityId: String, val entityName: String,
    val prompt: String, val size: String, val referenceId: String?, val referenceHash: String?,
    val model: String, val fileName: String, val createdAt: Long, val reservedNeurons: Int) {
    fun provenance() = "Generated with Cloudflare Workers AI · $model\nPrompt: $prompt\n" +
        (referenceId?.let { "Reference: $it · SHA-256: $referenceHash\n" } ?: "Text-only generation\n") +
        "Request: $requestId · $createdAt · $size"
}

class GeneratedImageDraftStore(files: File, private val scratch: File) {
    val directory = File(files, "generated-preview").apply { mkdirs() }
    private val manifest = File(directory, "preview.json")
    fun load(): GeneratedImageDraft? = runCatching {
        Json.decodeFromString<GeneratedImageDraft>(manifest.readText()).takeIf {
            it.fileName.matches(Regex("[a-zA-Z0-9-]+\\.png")) && File(directory, it.fileName).isFile
        }
    }.getOrNull()
    fun save(entityId: String, entityName: String, prompt: String, size: ImageSize,
             referenceId: String?, referenceHash: String?, result: GeneratedImage): GeneratedImageDraft {
        require(load() == null) { "Save or discard the previous preview first." }
        val (image, _) = ImageFiles.import(result.bytes.inputStream(), scratch, directory)
        val temporary = File(directory, "preview.pending")
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(image.path, bounds)
            require(bounds.outWidth == size.width && bounds.outHeight == size.height) { "Cloudflare returned unexpected image dimensions. The image was not saved to Gallery." }
            val draft = GeneratedImageDraft(result.requestId, entityId, entityName, prompt, size.wire, referenceId,
                referenceHash, result.model, image.name, System.currentTimeMillis(), result.reservedNeurons)
            temporary.writeText(Json.encodeToString(draft))
            check(temporary.renameTo(manifest)) { "Could not preserve the image preview." }
            return draft
        } catch (e: Exception) { image.delete(); temporary.delete(); throw e }
    }
    fun clear() {
        load()?.let { File(directory, it.fileName).delete() }
        manifest.delete()
    }
}
