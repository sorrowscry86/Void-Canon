package org.voidcat.voidcanon.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.voidcat.voidcanon.ai.imageHash
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.time.Instant
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

internal fun copyLimited(input: InputStream, output: OutputStream, limit: Long): Long {
    var count = 0L
    val buffer = ByteArray(8192)
    while (true) {
        val n = input.read(buffer)
        if (n < 0) break
        count += n
        require(count <= limit) { "File exceeds the supported size limit." }
        output.write(buffer, 0, n)
    }
    return count
}

/** Strict portable archive; extraction never follows a filename supplied by an archive. */
object BackupArchive {
    val json = Json { prettyPrint = true; encodeDefaults = true }
    private val safeId = Regex("[a-zA-Z0-9_-]{1,100}")
    private val safeFile = Regex("[a-zA-Z0-9_-]{1,100}\\.png")
    private const val MAX_TOTAL_BYTES = 128L * 1024 * 1024
    private const val MAX_RECORDS = 10000

    fun write(snapshot: WorldSnapshot, images: File, output: OutputStream) {
        validateGraph(snapshot)
        val manifest = json.encodeToString(snapshot.copy(formatVersion = 4, exportedAt = Instant.now().toString())).toByteArray()
        require(manifest.size <= 16 * 1024 * 1024) { "World is too large for a portable backup." }
        var total = manifest.size.toLong()
        snapshot.assets.forEach {
            val file = File(images, it.fileName)
            require(file.isFile && file.length() <= ImageFiles.MAX_FILE_BYTES) { "An image is missing or too large. Backup was not created." }
            total += file.length()
        }
        require(total <= MAX_TOTAL_BYTES) { "World is too large for a portable backup." }
        ZipOutputStream(output.buffered()).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json")); zip.write(manifest); zip.closeEntry()
            snapshot.assets.forEach { asset ->
                zip.putNextEntry(ZipEntry("images/${asset.fileName}"))
                File(images, asset.fileName).inputStream().use { copyLimited(it, zip, ImageFiles.MAX_FILE_BYTES) }
                zip.closeEntry()
            }
        }
    }

    data class Staged(val snapshot: WorldSnapshot, val directory: File) : AutoCloseable {
        override fun close() { directory.deleteRecursively() }
    }

    fun read(input: InputStream, cache: File): Staged {
        val directory = File(cache, "restore-${UUID.randomUUID()}").apply { check(mkdirs()) }
        try {
            var manifest: String? = null
            var total = 0L
            val seen = mutableSetOf<String>()
            ZipInputStream(input.buffered()).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(seen.add(entry.name) && seen.size <= MAX_RECORDS) { "Backup contains duplicate or too many files." }
                    require(!entry.isDirectory) { "Backup contains an unsupported directory entry." }
                    if (entry.name == "manifest.json") {
                        val bytes = java.io.ByteArrayOutputStream()
                        total += copyLimited(zip, bytes, 16L * 1024 * 1024)
                        manifest = bytes.toString(Charsets.UTF_8.name())
                    } else {
                        require(entry.name.startsWith("images/") && safeFile.matches(entry.name.removePrefix("images/"))) {
                            "Backup contains an unsafe or unsupported file path."
                        }
                        val file = File(directory, entry.name.removePrefix("images/"))
                        file.outputStream().use { total += copyLimited(zip, it, ImageFiles.MAX_FILE_BYTES) }
                    }
                    require(total <= MAX_TOTAL_BYTES) { "Backup exceeds the supported size limit." }
                    zip.closeEntry()
                }
            }
            val snapshot = try {
                json.decodeFromString<WorldSnapshot>(requireNotNull(manifest) { "This is not a Void Canon backup." })
            } catch (e: SerializationException) {
                throw IllegalArgumentException("The backup manifest is unreadable. Choose a valid Void Canon backup.", e)
            }
            validateGraph(snapshot)
            val expected = snapshot.assets.map { "images/${it.fileName}" }.toSet() + "manifest.json"
            require(seen == expected) { "Backup has missing or unreferenced images." }
            snapshot.assets.forEach { asset ->
                val transparent = ImageFiles.validate(File(directory, asset.fileName))
                require(transparent == asset.transparent) { "Backup image transparency does not match its metadata." }
            }
            val normalized = snapshot.copy(formatVersion = 4,
                assets = snapshot.assets.map { asset ->
                    val hash = imageHash(File(directory, asset.fileName))
                    require(asset.contentHash.isBlank() || asset.contentHash == hash) { "Backup image does not match its source hash." }
                    asset.copy(contentHash = hash)
                },
                generations = snapshot.generations.map { if (it.status == "pending") it.copy(status = "failed", error = "Restored request was not resumed. Retry explicitly.") else it })
            return Staged(normalized, directory)
        } catch (e: Exception) { directory.deleteRecursively(); throw e }
    }

    fun validateGraph(s: WorldSnapshot) {
        require(s.formatVersion in 1..4) { "Unsupported backup version. This app supports versions 1, 2, 3, and 4." }
        if (s.formatVersion < 4) require(s.continuityEvents.isEmpty() && s.scenes.all { it.continuityJson.isBlank() }) { "Legacy backups cannot contain tracked state." }
        require(s.worlds.isNotEmpty()) { "A backup must contain at least one world." }
        if (s.formatVersion < 3) require(s.worlds.size == 1 && s.scenes.size == 1) { "Legacy backups must contain one world and one scene." }
        require(listOf(s.worlds.size, s.entries.size, s.assets.size, s.scenes.size, s.messages.size, s.descriptions.size, s.chapters.size, s.generations.size, s.continuityEvents.size).sum() <= MAX_RECORDS) { "Too many records." }
        val allIds = s.worlds.map { it.id } + s.entries.map { it.id } + s.assets.map { it.id } + s.scenes.map { it.id } + s.messages.map { it.id } + s.descriptions.map { it.id } + s.chapters.map { it.id } + s.generations.map { it.id } + s.continuityEvents.map { it.id }
        require(allIds.all { safeId.matches(it) } && allIds.toSet().size == allIds.size) { "Invalid or duplicate record IDs." }
        val worlds = s.worlds.associateBy { it.id }
        val entries = s.entries.associateBy { it.id }
        val assets = s.assets.associateBy { it.id }
        val scenes = s.scenes.associateBy { it.id }
        fun text(value: String) { require(value.length <= 50000) { "A text field is too long." } }
        s.worlds.forEach { world ->
            require(world.name.isNotBlank() && world.name.length <= 200) { "World name is missing or too long." }
            text(world.description)
        }
        require(s.assets.map { it.fileName }.distinct().size == s.assets.size) { "Duplicate image filenames." }
        s.entries.forEach { e ->
            require(e.worldId in worlds && e.kind in setOf("character", "location") && e.name.isNotBlank()) { "Invalid world entry." }
            text(e.name); text(e.description); text(e.lore); text(e.authorNotes)
            require(e.primaryAssetId == null || assets[e.primaryAssetId]?.entityId == e.id) { "Broken primary image reference." }
        }
        s.assets.forEach { a ->
            require(a.entityId in entries && safeFile.matches(a.fileName)) { "Invalid asset reference or filename." }
            require(a.role in setOf("portrait", "background", "cutout", "reference")) { "Unsupported asset role." }
            require(a.referenceKind in setOf("canon", "alternate", "exploratory")) { "Unsupported reference kind." }
            require((s.formatVersion == 1 && a.contentHash.isBlank()) || a.contentHash.matches(Regex("[a-f0-9]{64}"))) { "Invalid image source hash." }
            text(a.label); text(a.provenance)
        }
        s.worlds.forEach { world ->
            require(world.coverAssetId == null || assets[world.coverAssetId]?.let { asset -> entries[asset.entityId]?.let { it.kind == "location" && it.worldId == world.id } } == true) { "Broken cover image reference." }
        }
        s.scenes.forEach { scene ->
            require(scene.worldId in worlds && entries[scene.characterId]?.let { it.kind == "character" && it.worldId == scene.worldId } == true && entries[scene.locationId]?.let { it.kind == "location" && it.worldId == scene.worldId } == true) { "Broken scene entry reference." }
            require(scene.backgroundAssetId == null || assets[scene.backgroundAssetId]?.entityId == scene.locationId) { "Broken scene background." }
            require(scene.cutoutAssetId == null || assets[scene.cutoutAssetId]?.let { it.entityId == scene.characterId && it.transparent } == true) { "Scene cutout must be a transparent character image." }
            require(scene.demoStep >= 0 && scene.savedAt >= 0) { "Invalid demo state." }
            text(scene.draft); text(scene.authorDirection); text(scene.userRole); text(scene.characterKnowledge); text(scene.provisionalState); text(scene.authorNotes)
        }
        require(s.messages.map { it.sceneId to it.sequence }.distinct().size == s.messages.size) { "Duplicate message order." }
        s.messages.forEach { m ->
            require(m.sceneId in scenes && m.speaker in setOf("user", "character", "narrator") && m.sequence >= 0) { "Invalid message reference." }
            require(m.mode in setOf("demo", "live")) { "Unsupported message mode." }
            require(m.generationId == null || s.generations.any { it.id == m.generationId && it.sceneId == m.sceneId && it.purpose == "roleplay" && it.status == "completed" }) { "Broken generation reference." }
            text(m.text)
        }
        require(s.descriptions.map { it.assetId }.distinct().size == s.descriptions.size) { "Duplicate visual descriptions." }
        s.descriptions.forEach { d ->
            require(d.assetId in assets && d.sourceHash.matches(Regex("[a-f0-9]{64}")) && d.updatedAt >= 0 && d.text.length <= 10000 && (!d.approved || d.text.isNotBlank())) { "Invalid visual description." }
            text(d.provenance)
        }
        fun sourceIds(encoded: String, sceneId: String): List<String> {
            require(encoded.length <= 200000) { "Too many source references." }
            val ids = json.decodeFromString<List<String>>(encoded)
            require(ids.distinct().size == ids.size && ids.all { id -> s.messages.any { it.id == id && it.sceneId == sceneId } }) { "Broken transcript source references." }
            return ids
        }
        s.chapters.forEach { chapter ->
            require(chapter.sceneId in scenes && chapter.title.isNotBlank() && chapter.title.length <= 200 && chapter.summary.length <= 20000 && chapter.updatedAt >= 0 && (!chapter.approved || chapter.summary.isNotBlank())) { "Invalid chapter." }
            require(sourceIds(chapter.sourceMessageIdsJson, chapter.sceneId).isNotEmpty()) { "Chapter has no transcript sources." }
        }
        s.scenes.forEach { scene ->
            val selected = s.scoped(scene.worldId, scene.id)
            val events = s.continuityEvents.filter { it.sceneId == scene.id }.sortedBy { Continuity.decode(it.afterJson).revision }
            var previous: ContinuityState? = null
            events.forEach { event ->
                require(event.createdAt >= 0) { "Invalid state event timestamp." }
                val before = Continuity.decode(event.beforeJson)
                val after = Continuity.decode(event.afterJson)
                Continuity.validate(before, s, scene); Continuity.validate(after, s, scene)
                require(after.revision == before.revision + 1 && (previous == null || previous == before)) { "Broken state revision history." }
                if (previous == null) require(before.revision == 0L) { "Missing initial state history." }
                event.sourceMessageId?.let { id ->
                    require(s.messages.any { it.id == id && it.sceneId == scene.id && it.speaker == "character" && it.mode == "live" }) { "Broken state transcript source." }
                }
                previous = after
            }
            if (scene.continuityJson.isNotBlank()) {
                val current = Continuity.current(selected)
                Continuity.validate(current, s, scene)
                require(previous == current) { "State does not match its revision history." }
            } else require(events.isEmpty()) { "State history has no current snapshot." }
        }
        require(s.continuityEvents.all { it.sceneId in scenes }) { "Broken state scene reference." }
        s.generations.forEach { g ->
            require(g.sceneId in scenes && g.purpose in setOf("roleplay", "vision", "summary") && g.status in setOf("pending", "completed", "failed", "cancelled")) { "Invalid generation record." }
            require(g.model.isNotBlank() && g.model.length <= 200 && g.contextJson.length <= 100000 && g.output.length <= 20000 && g.usageJson.length <= 10000 && g.createdAt >= 0) { "Invalid generation metadata." }
            require(g.sourceAssetId == null || assets[g.sourceAssetId]?.let { entries[it.entityId]?.worldId == scenes[g.sceneId]?.worldId } == true) { "Broken generation image source." }
            sourceIds(g.sourceMessageIdsJson, g.sceneId)
            text(g.userText); text(g.error)
        }
    }
}
