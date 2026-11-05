package org.voidcat.voidcanon.data

import android.content.Context
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.voidcat.voidcanon.ai.*

class CanonRepository(private val context: Context, private val db: CanonDatabase) {
    private val dao = db.canonDao()
    private val lock = Mutex()
    val images = File(context.filesDir, "images").apply { mkdirs() }
    private val cache = context.cacheDir

    private val selection = context.getSharedPreferences("library-selection", Context.MODE_PRIVATE)
    private val selectionRevision = MutableStateFlow(0)

    fun observe(): Flow<LibrarySnapshot> = db.invalidationTracker.createFlow("worlds", "entries", "assets", "scenes", "messages", "visual_descriptions", "chapters", "generations", "continuity_events")
        .combine(selectionRevision) { _, _ -> library() }.flowOn(Dispatchers.IO)

    suspend fun workspaceSnapshot(): WorldSnapshot = db.withTransaction { dao.snapshot() }
    suspend fun library(): LibrarySnapshot {
        val all = workspaceSnapshot()
        val world = all.worlds.find { it.id == selection.getString("world", null) } ?: all.worlds.firstOrNull()
        val scenes = all.scenes.filter { it.worldId == world?.id }
        val scene = scenes.find { it.id == selection.getString("scene-${world?.id}", null) } ?: scenes.firstOrNull()
        return LibrarySnapshot(if (world == null) WorldSnapshot() else all.scoped(world.id, scene?.id), all.worlds, scenes)
    }
    suspend fun snapshot(): WorldSnapshot = library().selected

    // Checked commit reports selection failures; mutations execute on Dispatchers.IO.
    @android.annotation.SuppressLint("ApplySharedPref", "UseKtx")
    private fun rememberSelection(worldId: String, sceneId: String? = null) {
        val editor = selection.edit().putString("world", worldId)
        if (sceneId != null) editor.putString("scene-$worldId", sceneId)
        check(editor.commit()) { "Could not save the selected world on this device." }
        selectionRevision.value += 1
    }

    suspend fun selectWorld(id: String) = mutate {
        require(dao.worlds().any { it.id == id }) { "This world is unavailable." }
        rememberSelection(id)
    }
    suspend fun selectScene(id: String) = mutate {
        val scene = dao.scenes().first { it.id == id }
        rememberSelection(scene.worldId, scene.id)
    }
    suspend fun createWorld(name: String, description: String): String = mutate {
        validateName(name); require(description.length <= 50000)
        val id = UUID.randomUUID().toString()
        dao.putWorlds(listOf(WorldRecord(id, name.trim(), description, null)))
        rememberSelection(id)
        id
    }
    suspend fun editWorld(id: String, name: String, description: String) = mutate {
        validateName(name); require(description.length <= 50000)
        val world = dao.worlds().first { it.id == id }
        dao.putWorlds(listOf(world.copy(name = name.trim(), description = description)))
    }
    suspend fun createEntry(worldId: String, kind: String, name: String, description: String, lore: String, notes: String): String = mutate {
        validateName(name)
        require(kind in setOf("character", "location") && dao.worlds().any { it.id == worldId }) { "Choose a world and an entry type." }
        require(listOf(description, lore, notes).all { it.length <= 50000 }) { "Entry fields can contain at most 50,000 characters." }
        val id = UUID.randomUUID().toString()
        dao.putEntries(listOf(EntryRecord(id, worldId, kind, name.trim(), description, lore, authorNotes = notes)))
        id
    }
    /** Reuse an existing pair's scene; choosing a new pair never rewrites old transcripts. */
    suspend fun useInScene(characterId: String, locationId: String): String = mutate {
        val character = dao.entries().first { it.id == characterId }
        val location = dao.entries().first { it.id == locationId }
        require(character.kind == "character" && location.kind == "location" && character.worldId == location.worldId) { "Choose a character and location from the same world." }
        val existing = dao.scenes().firstOrNull { it.characterId == characterId && it.locationId == locationId && it.worldId == character.worldId }
        val scene = existing ?: SceneRecord(UUID.randomUUID().toString(), character.worldId, characterId, locationId,
            location.primaryAssetId, dao.assets().firstOrNull { it.entityId == characterId && it.role == "cutout" && it.transparent }?.id)
        if (existing == null) dao.putScenes(listOf(scene))
        rememberSelection(scene.worldId, scene.id)
        scene.id
    }
    private fun validateName(name: String) {
        require(name.isNotBlank() && name.length <= 200) { "Enter a name of 1–200 characters." }
    }

    suspend fun initialize() = mutate {
        if (dao.worlds().isNotEmpty()) {
            dao.putAssets(dao.assets().filter { it.contentHash.isBlank() }.map { it.copy(contentHash = imageHash(File(images, it.fileName))) })
            dao.putGenerations(dao.generations().filter { it.status == "pending" }.map { it.copy(status = "failed", error = "Interrupted before completion. Retry explicitly; history is unchanged.") })
            return@mutate
        }
        val harbor = File(images, "demo-harbor.png")
        val mira = File(images, "demo-mira.png")
        context.assets.open("demo/harbor.png").use { input -> harbor.outputStream().use { input.copyTo(it) } }
        context.assets.open("demo/mira.png").use { input -> mira.outputStream().use { input.copyTo(it) } }
        val initial = WorldSnapshot(
            worlds = listOf(WorldRecord("glass-harbor", "The Glass Harbor", "A luminous port where trade, ambition, and old mysteries meet the sea.", "harbor-art")),
            entries = listOf(
                EntryRecord("mira", "glass-harbor", "character", "Mira", "A well-traveled explorer with a watchful gaze, a dark teal coat, and a satchel full of maps.",
                    "Traveler · Explorer · Reluctant hero\n\nMira has returned to the harbor to follow an unfinished map. This is editable sample lore, not a generated interpretation of her portrait.", "mira-art"),
                EntryRecord("docks", "glass-harbor", "location", "The Docks", "Lanterns sway above the quiet water. Every arriving ship brings a new beginning—and an old secret.",
                    "The western quay of the Glass Harbor. Demo setting; replace it with a place from your own world.", "harbor-art"),
            ),
            assets = listOf(
                AssetRecord("harbor-art", "docks", harbor.name, "background", "Harbor at dusk", "Generated demo artwork · OpenAI image generation · 2026-11-01", false),
                AssetRecord("mira-art", "mira", mira.name, "cutout", "Mira · traveler", "Generated demo artwork · OpenAI image generation · 2026-11-01", true),
            ),
            scenes = listOf(SceneRecord("harbor-scene", "glass-harbor", "mira", "docks", "harbor-art", "mira-art")),
            messages = listOf(MessageRecord("opening", "harbor-scene", "character", "The harbor is quiet. Someone got here before us.", 0)),
        )
        val hashed = initial.copy(assets = initial.assets.map { it.copy(contentHash = imageHash(File(images, it.fileName))) })
        BackupArchive.validateGraph(hashed)
        db.withTransaction { dao.replace(hashed) }
    }

    suspend fun editEntry(id: String, name: String, description: String, lore: String, authorNotes: String? = null) = mutate {
        require(name.isNotBlank() && name.length <= 200) { "Enter a name of 1–200 characters." }
        require(description.length <= 50000 && lore.length <= 50000) { "Descriptions can contain at most 50,000 characters." }
        val entry = dao.entries().first { it.id == id }
        dao.putEntries(listOf(entry.copy(name = name.trim(), description = description, lore = lore, authorNotes = authorNotes ?: entry.authorNotes)))
    }

    suspend fun updateScene(direction: String, userRole: String, knowledge: String, provisional: String, authorNotes: String) = mutate {
        require(listOf(direction, userRole, knowledge, provisional, authorNotes).all { it.length <= 5000 }) { "Scene fields can contain at most 5,000 characters." }
        dao.putScenes(snapshot().scenes.map { it.copy(authorDirection = direction, userRole = userRole.ifBlank { "Traveler" }, characterKnowledge = knowledge, provisionalState = provisional, authorNotes = authorNotes) })
    }

    suspend fun correctContinuity(sceneId: String, expectedRevision: Long, corrected: ContinuityState) = mutate {
        db.withTransaction {
            val all = dao.snapshot()
            val scene = all.scenes.first { it.id == sceneId }
            val s = all.scoped(scene.worldId, scene.id)
            val before = Continuity.current(s)
            require(before.revision == expectedRevision) { "Scene state changed. Reopen it before correcting." }
            val after = corrected.copy(revision = before.revision + 1)
            Continuity.validate(after, s)
            Continuity.project(s, after)
            dao.putScenes(listOf(scene.copy(continuityJson = Continuity.json.encodeToString(after))))
            dao.putContinuityEvents(listOf(ContinuityEvent(UUID.randomUUID().toString(), scene.id, null,
                Continuity.json.encodeToString(before), Continuity.json.encodeToString(after), System.currentTimeMillis())))
        }
    }

    suspend fun setReferenceKind(assetId: String, kind: String) = mutate {
        require(kind in setOf("canon", "alternate", "exploratory"))
        dao.putAssets(dao.assets().filter { it.id == assetId }.map { it.copy(referenceKind = kind) })
    }

    suspend fun saveDescription(assetId: String, text: String, approved: Boolean, provenance: String = "Author-written") = mutate {
        saveDescriptionLocked(assetId, text, approved, provenance)
    }
    private suspend fun saveDescriptionLocked(assetId: String, text: String, approved: Boolean, provenance: String) {
        require(text.length <= 10000 && (!approved || text.isNotBlank())) { "Enter a description before approval (maximum 10,000 characters)." }
        val asset = dao.assets().first { it.id == assetId }
        val hash = imageHash(File(images, asset.fileName))
        val previous = dao.descriptions().find { it.assetId == assetId }
        dao.putDescriptions(listOf(VisualDescription(previous?.id ?: UUID.randomUUID().toString(), assetId, hash, text, approved, provenance, System.currentTimeMillis())))
    }

    suspend fun saveChapter(id: String?, title: String, summary: String, approved: Boolean) = mutate {
        require(title.isNotBlank() && title.length <= 200 && summary.length <= 20000 && (!approved || summary.isNotBlank())) { "Enter a title and a summary before approval." }
        val s = snapshot()
        val previous = s.chapters.find { it.id == id }
        val ids = previous?.sourceMessageIdsJson ?: Json.encodeToString(SceneContext.sourceMessages(s).map { it.id })
        require(Json.decodeFromString<List<String>>(ids).isNotEmpty()) { "There are no new messages to save as a chapter." }
        dao.putChapters(listOf(ChapterRecord(previous?.id ?: UUID.randomUUID().toString(), s.scene!!.id, title, summary, ids, approved, System.currentTimeMillis())))
    }

    suspend fun beginGeneration(record: GenerationRecord) = mutate {
        require(record.status == "pending" && dao.generations().none { it.id == record.id })
        require(record.sceneId == snapshot().scene?.id) { "This request belongs to another scene." }
        require(record.fingerprint == SceneContext.fingerprint(snapshot())) { "The scene changed. Review the context and try again." }
        dao.putGenerations(listOf(record))
    }
    suspend fun finishGeneration(id: String, result: RouterResult) = mutate {
        var rejection: String? = null
        db.withTransaction {
            val all = dao.snapshot()
            val record = all.generations.first { it.id == id }
            val sourceScene = all.scenes.first { it.id == record.sceneId }
            val s = all.scoped(sourceScene.worldId, sourceScene.id)
            if (record.status != "pending") return@withTransaction
            if (record.fingerprint != SceneContext.fingerprint(s)) {
                dao.putGenerations(listOf(record.copy(status = "failed", actualModel = result.model, output = result.text,
                    usageJson = result.usageJson, error = "Scene changed; response was not applied.")))
                rejection = "The scene changed during generation. History and state are unchanged; the response is retained in Context."
                return@withTransaction
            }
            when (record.purpose) {
                "roleplay" -> {
                    val tracked = runCatching { Json.decodeFromString<ContextPreview>(record.contextJson).settings["continuityProtocol"] == "1" }.getOrDefault(false)
                    val resolved = try {
                        if (tracked) Continuity.resolve(result.text, record.userText, s) else result.text to null
                    } catch (e: Exception) {
                        rejection = e.message ?: "Invalid scene state response."
                        dao.putGenerations(listOf(record.copy(status = "failed", actualModel = result.model, output = result.text,
                            usageJson = result.usageJson, error = rejection!!)))
                        return@withTransaction
                    }
                    val narrative = resolved.first
                    resolved.second?.let { after ->
                        dao.putContinuityEvents(listOf(ContinuityEvent("state-$id", record.sceneId, "reply-$id",
                            Continuity.json.encodeToString(Continuity.current(s)), Continuity.json.encodeToString(after), System.currentTimeMillis())))
                    }
                    val sequence = (s.messages.maxOfOrNull { it.sequence } ?: -1) + 1
                    dao.putMessages(listOf(
                        MessageRecord("user-$id", record.sceneId, "user", record.userText, sequence, id, "live"),
                        MessageRecord("reply-$id", record.sceneId, "character", narrative, sequence + 1, id, "live")))
                    dao.putScenes(s.scenes.map { it.copy(draft = if (it.draft.trim() == record.userText.trim()) "" else it.draft, savedAt = System.currentTimeMillis(),
                        continuityJson = resolved.second?.let { state -> Continuity.json.encodeToString(state) } ?: it.continuityJson) })
                }
                "vision" -> {
                    val assetId = requireNotNull(record.sourceAssetId)
                    // Keep an existing approved appearance active until the author chooses the new proposal.
                    if (s.descriptions.none { it.assetId == assetId && it.approved })
                        saveDescriptionLocked(assetId, result.text, false, "OpenRouter · ${result.model} · proposed observations")
                }
                "summary" -> dao.putChapters(listOf(ChapterRecord(UUID.randomUUID().toString(), record.sceneId, "Chapter ${s.chapters.size + 1}", result.text, record.sourceMessageIdsJson, false, System.currentTimeMillis())))
                else -> error("Unknown generation purpose.")
            }
            dao.putGenerations(listOf(record.copy(status = "completed", actualModel = result.model, output = result.text, usageJson = result.usageJson)))
        }
        require(rejection == null) { rejection!! }
    }
    suspend fun failGeneration(id: String, cancelled: Boolean, error: String) = mutate {
        dao.putGenerations(dao.generations().filter { it.id == id && it.status == "pending" }.map { it.copy(status = if (cancelled) "cancelled" else "failed", error = error.take(1000)) })
    }

    suspend fun saveDraft(text: String) = mutate {
        require(text.length <= 5000) { "A demo message can contain at most 5,000 characters." }
        val scene = requireNotNull(snapshot().scene) { "Choose a character and location to start a scene." }
        dao.putScenes(listOf(scene.copy(draft = text)))
    }

    suspend fun send(text: String) = mutate {
        require(text.isNotBlank() && text.length <= 5000) { "Write a message of 1–5,000 characters." }
        db.withTransaction {
            val scene = requireNotNull(snapshot().scene) { "Choose a character and location to start a scene." }
            val sequence = (dao.messages().filter { it.sceneId == scene.id }.maxOfOrNull { it.sequence } ?: -1) + 1
            require(scene.id == "harbor-scene") { "Scripted demo replies are available only in the sample harbor scene. Choose Live RP for your own world." }
            val responses = listOf(
                "Then let's take the lantern road. If there is a trail, it starts where the ships came in.",
                "I found a mark like this on my map. Stay close; the tide will hide the lower steps soon.\n\nA bell sounds across the water. Mira pauses, one hand resting on the strap of her satchel. The harbor holds its breath, and for a moment the only sound is the soft creak of a mooring rope.",
                "We can decide where the story goes from here. For now, the map and the harbor are ours to explore.",
            )
            dao.putMessages(listOf(
                MessageRecord(UUID.randomUUID().toString(), scene.id, "user", text.trim(), sequence),
                MessageRecord(UUID.randomUUID().toString(), scene.id, "character", responses[scene.demoStep % responses.size], sequence + 1),
            ))
            dao.putScenes(listOf(scene.copy(draft = "", demoStep = (scene.demoStep + 1) % responses.size, savedAt = System.currentTimeMillis())))
        }
    }

    suspend fun saveScene() = mutate {
        dao.putScenes(snapshot().scenes.map { it.copy(savedAt = System.currentTimeMillis()) })
    }

    suspend fun importImage(entityId: String, role: String, input: InputStream) = mutate {
        val entry = dao.entries().first { it.id == entityId }
        require(role in if (entry.kind == "character") setOf("portrait", "cutout", "reference") else setOf("background", "reference")) { "Choose an appropriate image role." }
        val (file, transparent) = ImageFiles.import(input, cache, images)
        try {
            require(role != "cutout" || transparent) { "Character cutouts need a transparent background. Import this as a portrait instead." }
            val asset = AssetRecord(UUID.randomUUID().toString(), entityId, file.name, role,
                "${entry.name} · ${role.replaceFirstChar { it.uppercase() }}", "Imported by author using Android's document picker", transparent, contentHash = imageHash(file))
            db.withTransaction {
                dao.putAssets(listOf(asset))
                selectAssetLocked(asset, role)
            }
        } catch (e: Exception) { file.delete(); throw e }
    }

    suspend fun selectAsset(assetId: String, role: String) = mutate {
        db.withTransaction { selectAssetLocked(dao.assets().first { it.id == assetId }, role) }
    }

    /** Save a reviewed output against its original owner. Activation and approval are separate. */
    suspend fun importGeneratedImage(draft: GeneratedImageDraft, input: InputStream) = mutate {
        require(dao.entries().any { it.id == draft.entityId }) { "The original image owner is unavailable." }
        if (dao.assets().any { it.id == draft.requestId }) return@mutate
        val (file, transparent) = ImageFiles.import(input, cache, images)
        try {
            dao.putAssets(listOf(AssetRecord(draft.requestId, draft.entityId, file.name, "reference",
                "${draft.entityName} · Generated", draft.provenance(), transparent, imageHash(file), "exploratory")))
        } catch (e: Exception) { file.delete(); throw e }
    }

    private suspend fun selectAssetLocked(asset: AssetRecord, role: String) {
        val entry = dao.entries().first { it.id == asset.entityId }
        val scene = snapshot().scene
        when (role) {
            "portrait" -> {
                require(entry.kind == "character") { "Choose a character image." }
                dao.putEntries(listOf(entry.copy(primaryAssetId = asset.id)))
            }
            "cutout" -> {
                require(entry.kind == "character" && asset.transparent) { "Choose a transparent character image." }
                dao.putAssets(dao.assets().filter { it.entityId == entry.id && it.role == "cutout" && it.id != asset.id }.map { it.copy(role = "reference") } + asset.copy(role = "cutout"))
                if (scene?.characterId == entry.id) dao.putScenes(listOf(scene.copy(cutoutAssetId = asset.id)))
            }
            "background" -> {
                require(entry.kind == "location") { "Choose a location image." }
                if (scene?.locationId == entry.id) dao.putScenes(listOf(scene.copy(backgroundAssetId = asset.id)))
                dao.putEntries(listOf(entry.copy(primaryAssetId = asset.id)))
                val world = dao.worlds().first { it.id == entry.worldId }
                dao.putWorlds(listOf(world.copy(coverAssetId = asset.id)))
            }
            "reference" -> Unit
            else -> error("Unknown image role.")
        }
    }

    suspend fun export(output: OutputStream) = mutate {
        output.use { BackupArchive.write(workspaceSnapshot(), images, it) }
    }

    /** Files are prepared under new unique names; one DB commit switches the graph. */
    suspend fun restore(input: InputStream) = mutate {
        input.use { BackupArchive.read(it, cache) }.use { staged ->
            val installed = mutableListOf<File>()
            var committed = false
            try {
                val assets = staged.snapshot.assets.map { asset ->
                    val file = File(images, "${UUID.randomUUID()}.png")
                    installed += file
                    File(staged.directory, asset.fileName).copyTo(file)
                    asset.copy(fileName = file.name)
                }
                db.withTransaction { dao.replace(staged.snapshot.copy(assets = assets)) }
                committed = true
                selectionRevision.value += 1
                // Old files are harmless orphans if deletion is interrupted, never broken references.
                val retained = assets.map { it.fileName }.toSet()
                images.listFiles()?.filter { it.name !in retained }?.forEach { it.delete() }
            } finally {
                if (!committed) installed.forEach { it.delete() }
            }
        }
    }

    // Once a mutation starts, finishing the file/DB publication must outlive the
    // screen's coroutine. Otherwise cancellation after commit could delete files
    // which the committed database now references. Waiting for the lock remains cancellable.
    private suspend fun <T> mutate(block: suspend () -> T): T = withContext(Dispatchers.IO) {
        lock.withLock { withContext(NonCancellable) { block() } }
    }
}
