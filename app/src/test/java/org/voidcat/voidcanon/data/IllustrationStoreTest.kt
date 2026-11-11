package org.voidcat.voidcanon.data

import android.app.Application
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.voidcat.voidcanon.ai.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class IllustrationStoreTest {
    private lateinit var app: Application
    private lateinit var db: CanonDatabase
    private lateinit var repo: CanonRepository
    private lateinit var store: IllustrationStore
    private val name = "art-${UUID.randomUUID()}.db"
    @Before fun setup() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        open(); repo.initialize(); repo.artworkPreferences.resetPermissions()
    }
    private fun open() {
        db = Room.databaseBuilder(app, CanonDatabase::class.java, name).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
        repo = CanonRepository(app, db)
        store = IllustrationStore(db, app.filesDir, app.cacheDir)
    }
    @After fun close() { db.close(); app.deleteDatabase(name) }
    private suspend fun enable() {
        val s = repo.snapshot(); repo.artworkPreferences.configure(s.scene!!.id, s.world!!.id, true, "", "")
    }
    private suspend fun appearance(s: WorldSnapshot, value: String): WorldSnapshot {
        val current = repo.snapshot()
        require(current.scene!!.id == s.scene!!.id)
        val state = Continuity.current(current)
        repo.correctContinuity(current.scene!!.id, state.revision, state.copy(actors = state.actors.map {
            if (it.actorId == current.scene!!.characterId) it.copy(appearance = value) else it
        }))
        return repo.snapshot()
    }
    private fun png(): ByteArray {
        val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); bitmap.recycle() }.toByteArray()
    }

    @Test fun completedArtworkPublishesOnceAfterRestartAndSurvivesCacheEvictionAndBackup() = runBlocking {
        enable(); val before = repo.snapshot()
        store.plan(before, repo.artworkPreferences)
        val claimed = store.claim(store.jobs().single().id, "2026-11-10", 1700000000000L)!!
        val complete = store.finish(claimed, GeneratedImage(png(), CloudflareImageClient.MODEL, claimed.id, 32))
        db.close(); open()
        repo.retainIllustration(complete, store.file(complete)!!)
        repo.retainIllustration(complete, store.file(complete)!!)
        val after = repo.snapshot()
        val asset = after.assets.single { it.id == complete.id }
        assertEquals(before.assets.size + 1, after.assets.size)
        assertEquals(complete.plan().caption(), asset.label)
        assertEquals("exploratory", asset.referenceKind)
        assertEquals(before, after.copy(assets = before.assets))
        store.prune(maxBytes = 0)
        assertNull(store.file(store.jobs().single()))
        assertEquals(asset.contentHash, imageHash(File(repo.images, asset.fileName)))
        val backup = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        repo.restore(backup.inputStream())
        val restored = repo.snapshot().assets.single { it.id == asset.id }
        assertEquals(asset.label, restored.label)
        assertEquals(asset.contentHash, imageHash(File(repo.images, restored.fileName)))
    }

    @Test fun restoreCannotResurrectAnOldCompletedIllustration() = runBlocking {
        val before = repo.snapshot()
        val backup = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        enable(); store.plan(before, repo.artworkPreferences)
        val claimed = store.claim(store.jobs().single().id, "2026-11-10", 1700000000000L)!!
        val complete = store.finish(claimed, GeneratedImage(png(), CloudflareImageClient.MODEL, claimed.id, 32))
        val file = store.file(complete)!!
        repo.restore(backup.inputStream())
        repo.retainIllustration(complete, file)
        assertEquals(before.assets.map { it.id }, repo.snapshot().assets.map { it.id })
        assertEquals(before.messages, repo.snapshot().messages)
    }

    @Test fun generatedImageObservationsInformOnlyTheirExplicitCurrentSceneAndPreserveAuthorEdits() = runBlocking {
        enable(); val before = repo.snapshot()
        repo.artworkPreferences.configure(before.scene!!.id, before.world!!.id, true, "", "", true)
        store.plan(before, repo.artworkPreferences)
        val claimed = store.claim(store.jobs().single().id, "2026-11-10", 1700000000000L)!!
        val complete = store.finish(claimed, GeneratedImage(png(), CloudflareImageClient.MODEL, claimed.id, 32))
        repo.retainIllustration(complete, store.file(complete)!!)
        val asset = repo.snapshot().asset(complete.id)!!
        val context = ContextPreview(listOf(PromptMessage("user", "Describe one generated image")), listOf(asset.id), emptyList(), 4000,
            mapOf("automaticArtwork" to "1", "sourceHash" to asset.contentHash))
        val record = GenerationRecord("artwork-vision-${asset.id}", complete.sceneId, "vision", "fixture/vision", Json.encodeToString(context),
            SceneContext.fingerprint(repo.snapshot()), sourceAssetId = asset.id)
        assertTrue(repo.beginArtworkDescription(record)); assertFalse(repo.beginArtworkDescription(record))
        val reply = RouterResult("The image shows a red scarf and wet wooden rails.", "fixture/vision", "{}")
        repo.finishArtworkDescription(record.id, reply); repo.finishArtworkDescription(record.id, reply)
        val s = repo.snapshot();val description = s.descriptions.single { it.assetId == asset.id }
        assertFalse(description.approved)
        assertEquals(asset.contentHash, description.sourceHash)
        assertTrue(SceneContext.build(s, "Continue", illustrationId = asset.id).messages.any { it.content.contains(reply.text) })
        assertFalse(SceneContext.build(s, "Continue").messages.any { it.content.contains(reply.text) })
        val location = repo.createEntry("glass-harbor", "location", "Garden", "", "", "")
        repo.useInScene("mira", location)
        assertFalse(SceneContext.build(repo.snapshot(), "Continue", illustrationId = asset.id).messages.any { it.content.contains(reply.text) })
        assertEquals(before.messages, s.messages)
        assertEquals(before.entries, s.entries)
    }

    @Test fun pendingImageDescriptionCannotOverwriteAuthorReviewOrRepublishAfterRestore() = runBlocking {
        enable(); val before = repo.snapshot()
        repo.artworkPreferences.configure(before.scene!!.id, before.world!!.id, true, "", "", true)
        val original = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        store.plan(before, repo.artworkPreferences)
        val claimed = store.claim(store.jobs().single().id, "2026-11-10", 1700000000000L)!!
        val complete = store.finish(claimed, GeneratedImage(png(), CloudflareImageClient.MODEL, claimed.id, 32))
        repo.retainIllustration(complete, store.file(complete)!!)
        val asset = repo.snapshot().asset(complete.id)!!
        val context = ContextPreview(emptyList(), listOf(asset.id), emptyList(), 4000, mapOf("automaticArtwork" to "1", "sourceHash" to asset.contentHash))
        val record = GenerationRecord("artwork-vision-${asset.id}", complete.sceneId, "vision", "fixture/vision", Json.encodeToString(context),
            SceneContext.fingerprint(repo.snapshot()), sourceAssetId = asset.id)
        assertTrue(repo.beginArtworkDescription(record))
        repo.saveDescription(asset.id, "The author's correction", true)
        repo.finishArtworkDescription(record.id, RouterResult("An AI proposal", "fixture/vision", "{}"))
        assertEquals("The author's correction", repo.snapshot().descriptions.single { it.assetId == asset.id }.text)
        repo.restore(original.inputStream())
        repo.finishArtworkDescription(record.id, RouterResult("A late response", "fixture/vision", "{}"))
        assertEquals(before.descriptions, repo.snapshot().descriptions)
        assertEquals(before.assets.map { it.id }, repo.snapshot().assets.map { it.id })
    }

    @Test fun pendingCandidatesCoalesceAndStaleClaimsCannotDispatch() = runBlocking {
        enable(); val s = repo.snapshot()
        store.plan(s, repo.artworkPreferences)
        val original = store.jobs().single()
        store.plan(appearance(s, "Red coat"), repo.artworkPreferences)
        val current = store.plan(appearance(s, "Blue coat"), repo.artworkPreferences)
        assertEquals(1, store.jobs().count { it.status == "pending" })
        assertEquals(2, store.jobs().count { it.status == "superseded" })
        assertNull(store.claim(original.id, "2026-11-08", 100000))
        assertEquals(current.visualKey, store.jobs().last { it.status == "pending" }.visualKey)
    }

    @Test fun quotaAndCooldownCountDispatchedFailuresAcrossScenesAndSurviveRestore() = runBlocking {
        enable(); val s = repo.snapshot(); val base = 1700000000000L
        repeat(30) { n ->
            store.plan(appearance(s, "Coat $n"), repo.artworkPreferences)
            val pending = store.jobs().last { it.status == "pending" }
            val row = store.claim(pending.id, "2026-11-08", base + n * 30001L)!!
            store.fail(row.id, "failed", "Fixture failure")
        }
        store.plan(appearance(s, "Another coat"), repo.artworkPreferences)
        val pending = store.jobs().last { it.status == "pending" }
        assertNull(store.claim(pending.id, "2026-11-08", base + 31 * 30001))
        assertEquals(30, db.canonDao().illustrationAllowances().single().attempts)
        val backup = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        repo.restore(ByteArrayInputStream(backup))
        assertTrue(store.jobs().isEmpty())
        assertEquals(30, db.canonDao().illustrationAllowances().single().attempts)
        assertFalse(repo.artworkPreferences.enabled(s.scene!!.id))
        enable(); store.plan(repo.snapshot(), repo.artworkPreferences)
        assertNotNull(store.claim(store.jobs().single().id, "2026-11-09", base + 86400000))
    }

    @Test fun cooldownIsEnforcedAndManualRegenerationDoesNotConsumeAutomaticAllowance() = runBlocking {
        enable(); val s = repo.snapshot()
        store.plan(s, repo.artworkPreferences)
        val first = store.claim(store.jobs().single().id, "2026-11-08", 100000)!!
        store.fail(first.id, "failed", "test")
        store.plan(appearance(s, "Raincoat"), repo.artworkPreferences)
        val next = store.jobs().last { it.status == "pending" }
        assertNull(store.claim(next.id, "2026-11-08", 110000))
        store.plan(repo.snapshot(), repo.artworkPreferences, force = true)
        val manual = store.jobs().last { it.status == "pending" }
        assertFalse(manual.automatic)
        assertNotNull(store.claim(manual.id, "2026-11-08", 110000))
        assertEquals(1, db.canonDao().illustrationAllowances().single().attempts)
    }

    @Test fun restartNeverAutomaticallyReplaysASubmittedOrInterruptedRequest() = runBlocking {
        enable(); val s = repo.snapshot()
        store.plan(s, repo.artworkPreferences)
        store.claim(store.jobs().single().id, "2026-11-08", 100000)
        db.close(); open(); store.recover()
        assertEquals("interrupted", store.jobs().single().status)
        store.plan(repo.snapshot(), repo.artworkPreferences)
        assertEquals(1, store.jobs().size)
        assertNull(store.claim(store.jobs().single().id, "2026-11-08", 200000))
    }

    @Test fun cachedImagesDoNotAlterStoryFingerprintsAndEvictionNeverDeletesGalleryOriginals() = runBlocking {
        enable(); val s = repo.snapshot(); val fingerprint = SceneContext.fingerprint(s)
        store.plan(s, repo.artworkPreferences)
        val row = store.claim(store.jobs().single().id, "2026-11-08", 100000)!!
        val complete = store.finish(row, GeneratedImage(png(), CloudflareImageClient.MODEL, row.id, 32))
        assertNotNull(store.file(complete))
        assertEquals(fingerprint, SceneContext.fingerprint(repo.snapshot()))
        assertEquals(s.assets, repo.snapshot().assets)
        store.prune(protect = complete.id, maxBytes = 0)
        assertNotNull(store.file(complete))
        store.prune(maxBytes = 0)
        assertNull(store.file(store.jobs().single()))
        s.assets.forEach { assertTrue(File(repo.images, it.fileName).isFile) }
    }

    @Test fun acceptedStoryAndImageIntentCommitOnceButMalformedOptionalArtDoesNotRejectStory() = runBlocking {
        enable(); val s = repo.snapshot()
        val record = GenerationRecord("fixture-turn", s.scene!!.id, "roleplay", "fixture/free",
            Json.encodeToString(SceneContext.build(s, "Continue", artwork = true)), SceneContext.fingerprint(s), "Continue")
        repo.beginGeneration(record)
        val reply = Continuity.json.encodeToString(ContinuityReply(1, 0, "Mira raises a lantern.", visual = JsonPrimitive(42)))
        val result = RouterResult(reply, "fixture/free", "{}")
        repo.finishGeneration(record.id, result); repo.finishGeneration(record.id, result)
        assertEquals(s.messages.size + 2, repo.snapshot().messages.size)
        assertEquals(1, store.jobs().size)
        assertEquals("reply-fixture-turn", store.jobs().single().plan().sourceMessageId)
        assertEquals("pending", store.jobs().single().status)
        val frozen = store.frame(s.scene!!.id)
        assertTrue(runCatching { store.plan(s, repo.artworkPreferences) }.isFailure)
        assertEquals(frozen, store.frame(s.scene!!.id)) // An older observer cannot overwrite the committed visual intent.
        assertEquals(s.assets, repo.snapshot().assets)
    }

    @Test fun evictionUsesRecentAccessRatherThanCreationOrder() = runBlocking {
        enable(); val s = repo.snapshot()
        store.plan(s, repo.artworkPreferences)
        val first = store.claim(store.jobs().single().id, "2026-11-08", 100000)!!
        val one = store.finish(first, GeneratedImage(png(), CloudflareImageClient.MODEL, first.id, 32))
        store.plan(appearance(s, "Raincoat"), repo.artworkPreferences)
        val second = store.claim(store.jobs().last { it.status == "pending" }.id, "2026-11-08", 200000)!!
        val two = store.finish(second, GeneratedImage(png(), CloudflareImageClient.MODEL, second.id, 32))
        store.touch(one.id, System.currentTimeMillis() + 120000)
        store.prune(maxBytes = store.file(one)!!.length())
        val rows = store.jobs().associateBy { it.id }
        assertNotNull(store.file(rows[one.id]))
        assertNull(store.file(rows[two.id]))
    }

    @Test fun realSchemaThreeUpgradePreservesEveryDomainRowAndId() = runBlocking {
        val before = repo.workspaceSnapshot()
        db.close(); app.deleteDatabase(name)
        val relative = "schemas/org.voidcat.voidcanon.data.CanonDatabase/3.json"
        val schema = Json.parseToJsonElement(listOf(File(relative), File("app/$relative")).first { it.isFile }.readText()).jsonObject.getValue("database").jsonObject
        val encoded = Continuity.json.encodeToJsonElement(before).jsonObject
        val keys = mapOf("worlds" to "worlds", "entries" to "entries", "assets" to "assets", "scenes" to "scenes", "messages" to "messages", "visual_descriptions" to "descriptions", "chapters" to "chapters", "generations" to "generations", "continuity_events" to "continuityEvents")
        SQLiteDatabase.openOrCreateDatabase(app.getDatabasePath(name), null).use { old ->
            schema.getValue("entities").jsonArray.forEach { raw ->
                val entity = raw.jsonObject; val table = entity.getValue("tableName").jsonPrimitive.content
                old.execSQL(entity.getValue("createSql").jsonPrimitive.content.replace("${'$'}{TABLE_NAME}", table))
                encoded[keys.getValue(table)]!!.jsonArray.forEach { row ->
                    val values = android.content.ContentValues()
                    row.jsonObject.filterKeys { key -> key in entity.getValue("fields").jsonArray.map { it.jsonObject.getValue("columnName").jsonPrimitive.content } }.forEach { (key, value) ->
                        val p = value.jsonPrimitive
                        when {
                            value == JsonNull -> values.putNull(key)
                            p.booleanOrNull != null -> values.put(key, if (p.boolean) 1 else 0)
                            p.longOrNull != null && !p.isString -> values.put(key, p.long)
                            else -> values.put(key, p.content)
                        }
                    }
                    old.insertOrThrow(table, null, values)
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { old.execSQL(it.jsonPrimitive.content) }
            old.version = 3
        }
        open(); repo.initialize()
        assertEquals(before, repo.workspaceSnapshot())
        assertTrue(store.jobs().isEmpty())
        assertEquals(5, db.openHelper.writableDatabase.version)
    }
}
