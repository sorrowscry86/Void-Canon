package org.voidcat.voidcanon.data

import android.app.Application
import android.graphics.Bitmap
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FilterInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.UUID
import org.voidcat.voidcanon.ai.*
import kotlinx.serialization.json.*
import android.database.sqlite.SQLiteDatabase
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RepositoryTest {
    private lateinit var context: Application
    private lateinit var db: CanonDatabase
    private lateinit var repo: CanonRepository
    private lateinit var dbName: String

    @Before fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        dbName = "test-${UUID.randomUUID()}.db"
        openDatabase()
        repo.initialize()
    }

    private fun openDatabase() {
        db = Room.databaseBuilder(context, CanonDatabase::class.java, dbName).addMigrations(MIGRATION_1_2, MIGRATION_2_3).allowMainThreadQueries().build()
        repo = CanonRepository(context, db)
    }

    @After fun tearDown() { db.close(); context.deleteDatabase(dbName) }

    private suspend fun trackedRequest(input: String = "I wait."): GenerationRecord {
        val s = repo.snapshot()
        return GenerationRecord(UUID.randomUUID().toString(), s.scene!!.id, "roleplay", "fixture/free",
            Json.encodeToString(SceneContext.build(s, input)), SceneContext.fingerprint(s), input).also { repo.beginGeneration(it) }
    }

    @Test fun trackedTurnStateAndSourcesCommitOnceAndSurviveReopenAndBackup() = runBlocking {
        repo.saveDraft("I wait.")
        val before = repo.snapshot()
        val request = trackedRequest()
        val actor = Continuity.current(before).actors.last().copy(position = "by the railing")
        val payload = ContinuityReply(1, 0, "Mira waits by the railing.", actors = listOf(ActorChange(actor, "by the railing")))
        val response = RouterResult(Continuity.json.encodeToString(payload), "actual/free", "{\"total_tokens\":42}")
        repo.finishGeneration(request.id, response)
        repo.finishGeneration(request.id, response)
        val after = repo.snapshot()
        assertEquals(3, after.messages.size)
        assertEquals("Mira waits by the railing.", after.messages.last().text)
        assertEquals("", after.scene!!.draft)
        assertEquals(1L, Continuity.current(after).revision)
        assertEquals("reply-${request.id}", after.continuityEvents.single().sourceMessageId)
        assertTrue(after.generations.single().output.contains("baseRevision"))
        db.close(); openDatabase(); repo.initialize()
        assertEquals(after.scenes, repo.snapshot().scenes)
        assertEquals(after.continuityEvents, repo.snapshot().continuityEvents)
        repo.saveChapter(null, "Waiting", "A memory that cannot move Mira", true)
        assertEquals(Continuity.current(after), Continuity.current(repo.snapshot()))
        val bytes = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        repo.correctContinuity(after.scene!!.id, 1, Continuity.current(after).copy(timeAnchor = "midnight"))
        repo.restore(ByteArrayInputStream(bytes))
        assertEquals(after.continuityEvents, repo.snapshot().continuityEvents)
        assertEquals(Continuity.current(after), Continuity.current(repo.snapshot()))
        repo.editEntry("mira", "Renamed Mira", "", "")
        assertEquals("mira", Continuity.current(repo.snapshot()).actors.last().actorId)
        assertTrue(Continuity.project(repo.snapshot()).text.contains("Renamed Mira"))
    }

    @Test fun malformedStateResponsePreservesDraftAndRecordsDiagnosticsWithoutPublishingStory() = runBlocking {
        repo.saveDraft("I wait.")
        val before = repo.snapshot()
        val request = trackedRequest()
        assertTrue(runCatching { repo.finishGeneration(request.id, RouterResult("Plain prose without state", "actual/free", "{\"total_tokens\":5}")) }.isFailure)
        val after = repo.snapshot()
        assertEquals(before.messages, after.messages)
        assertEquals(before.scenes, after.scenes)
        assertTrue(after.continuityEvents.isEmpty())
        assertEquals("failed", after.generations.single().status)
        assertEquals("actual/free", after.generations.single().actualModel)
        assertTrue(after.generations.single().usageJson.contains("5"))
    }

    @Test fun manualCorrectionInvalidatesPendingResponsesAndDoesNotChangeTranscript() = runBlocking {
        val request = trackedRequest()
        val before = repo.snapshot()
        val corrected = Continuity.current(before).copy(actors = Continuity.current(before).actors.map { it.copy(activity = "waiting") })
        repo.correctContinuity(before.scene!!.id, 0, corrected)
        assertTrue(runCatching { repo.correctContinuity(before.scene!!.id, 0, corrected) }.isFailure)
        assertTrue(runCatching { repo.finishGeneration(request.id, RouterResult(Continuity.json.encodeToString(ContinuityReply(1, 0, "Too late")), "fixture/free", "{}")) }.isFailure)
        assertEquals(before.messages, repo.snapshot().messages)
        assertEquals(1L, Continuity.current(repo.snapshot()).revision)
        assertEquals(1, repo.snapshot().continuityEvents.size)
    }

    @Test fun trackedCompletionUpdatesItsSourceSceneEvenAfterSelectionChanges() = runBlocking {
        val request = trackedRequest()
        val before = repo.snapshot()
        val world = repo.createWorld("Other world", "")
        val c = repo.createEntry(world, "character", "Other", "", "", "")
        val l = repo.createEntry(world, "location", "Elsewhere", "", "", "")
        repo.useInScene(c, l)
        repo.finishGeneration(request.id, RouterResult(Continuity.json.encodeToString(ContinuityReply(1, 0, "Mira waits.")), "fixture/free", "{}"))
        assertEquals(0L, Continuity.current(repo.snapshot()).revision)
        assertTrue(repo.snapshot().continuityEvents.isEmpty())
        repo.selectScene(before.scene!!.id)
        assertEquals(1L, Continuity.current(repo.snapshot()).revision)
        assertEquals(1, repo.snapshot().continuityEvents.size)
    }

    @Test fun stateArchiveRejectsBrokenAuditAndForeignOwnershipBeforeReplacingData() = runBlocking {
        val before = repo.snapshot()
        repo.correctContinuity(before.scene!!.id, 0, Continuity.current(before))
        val valid = repo.workspaceSnapshot()
        val event = valid.continuityEvents.single()
        val broken = listOf(
            valid.copy(continuityEvents = listOf(event.copy(sourceMessageId = "missing-message"))),
            valid.copy(continuityEvents = listOf(event.copy(sceneId = "missing-scene"))),
            valid.copy(continuityEvents = emptyList()),
            valid.copy(formatVersion = 3),
            valid.copy(scenes = valid.scenes.map { it.copy(continuityJson = Continuity.json.encodeToString(Continuity.current(before).copy(revision = 42))) })
        )
        broken.forEach { candidate ->
            assertTrue(runCatching { repo.restore(ByteArrayInputStream(rawArchive(candidate))) }.isFailure)
            assertEquals(valid, repo.workspaceSnapshot())
        }
    }

    @Test fun actualVersionTwoDatabaseMigratesWithAllRowsAndEmptyTracking() = runBlocking {
        val before = repo.workspaceSnapshot()
        db.close(); context.deleteDatabase(dbName)
        val relative = "schemas/org.voidcat.voidcanon.data.CanonDatabase/2.json"
        val schemaFile = listOf(File(relative), File("app/$relative")).first { it.exists() }
        val schema = Json.parseToJsonElement(schemaFile.readText()).jsonObject.getValue("database").jsonObject
        val file = context.getDatabasePath(dbName)
        val encoded = Continuity.json.encodeToJsonElement(before).jsonObject
        val tableKeys = mapOf("worlds" to "worlds", "entries" to "entries", "assets" to "assets", "scenes" to "scenes", "messages" to "messages",
            "visual_descriptions" to "descriptions", "chapters" to "chapters", "generations" to "generations")
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            schema.getValue("entities").jsonArray.forEach { entity ->
                val table = entity.jsonObject
                val name = table.getValue("tableName").jsonPrimitive.content
                old.execSQL(table.getValue("createSql").jsonPrimitive.content.replace("${'$'}{TABLE_NAME}", name))
                encoded[tableKeys[name]]?.jsonArray.orEmpty().forEach { row ->
                    val values = android.content.ContentValues()
                    row.jsonObject.filterKeys { it != "continuityJson" }.forEach { (key, value) ->
                        val primitive = value.jsonPrimitive
                        when {
                            value == JsonNull -> values.putNull(key)
                            primitive.booleanOrNull != null -> values.put(key, if (primitive.boolean) 1 else 0)
                            primitive.longOrNull != null && !primitive.isString -> values.put(key, primitive.long)
                            else -> values.put(key, primitive.content)
                        }
                    }
                    old.insertOrThrow(name, null, values)
                }
            }
            schema.getValue("setupQueries").jsonArray.forEach { old.execSQL(it.jsonPrimitive.content) }
            old.version = 2
        }
        openDatabase(); repo.initialize()
        val after = repo.workspaceSnapshot()
        assertEquals(before, after)
        assertTrue(after.continuityEvents.isEmpty())
        assertEquals(0L, Continuity.current(repo.snapshot()).revision)
    }

    @Test fun seedIsIdempotentAndRenamePreservesReferencesAcrossDatabaseReopen() = runBlocking {
        val initial = repo.snapshot()
        repo.editEntry("mira", "Mira Renamed", "New description", "Author lore")
        repo.initialize()
        repo.saveDraft("An unfinished thought")
        repo.send("I follow the lanterns.")
        repo.saveDraft("Keep this unsent")
        db.close()
        openDatabase()
        repo.initialize()
        val restored = repo.snapshot()
        assertEquals("Mira Renamed", restored.entry("mira")?.name)
        assertEquals(initial.scene?.characterId, restored.scene?.characterId)
        assertEquals(initial.assets, restored.assets)
        assertEquals("Keep this unsent", restored.scene?.draft)
        assertEquals(3, restored.messages.size)
        assertEquals(listOf(0L, 1L, 2L), restored.messages.map { it.sequence })
    }

    @Test fun backupRoundTripRestoresEditsImagesSelectionDraftAndTranscript() = runBlocking {
        repo.editEntry("docks", "The Quiet Quay", "A place of my own", "My setting")
        val png = imageBytes(transparent = true)
        repo.importImage("mira", "cutout", ByteArrayInputStream(png))
        repo.send("Who came before us?")
        repo.saveDraft("A saved draft")
        val before = repo.snapshot()
        val imageBytes = before.assets.associate { it.id to File(repo.images, it.fileName).readBytes() }
        val archive = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        repo.editEntry("docks", "Changed after export", "Changed", "Changed")
        repo.send("This should disappear after restore.")
        repo.restore(ByteArrayInputStream(archive))
        val after = repo.snapshot()
        assertEquals(before.worlds, after.worlds)
        assertEquals(before.entries, after.entries)
        assertEquals(before.scenes, after.scenes)
        assertEquals(before.messages, after.messages)
        assertEquals(before.assets.map { it.id }, after.assets.map { it.id })
        after.assets.forEach { assertArrayEquals(imageBytes[it.id], File(repo.images, it.fileName).readBytes()) }
        assertFalse(context.cacheDir.listFiles().orEmpty().any { it.name.startsWith("restore-") })
    }

    @Test fun restoringIntoCleanInstallationDoesNotReseedOrBreakImages() = runBlocking {
        repo.editEntry("mira", "My Traveler", "Custom", "Custom lore")
        val archive = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        db.close()
        context.deleteDatabase(dbName)
        repo.images.deleteRecursively()
        openDatabase()
        repo.restore(ByteArrayInputStream(archive))
        repo.initialize()
        assertEquals("My Traveler", repo.snapshot().entry("mira")?.name)
        repo.snapshot().assets.forEach { assertTrue(File(repo.images, it.fileName).isFile) }
    }

    @Test fun corruptZipDoesNotChangeCurrentDataOrImages() = runBlocking {
        assertRestoreRejected("not a zip".toByteArray())
    }

    @Test fun unsupportedVersionDoesNotChangeCurrentDataOrImages() = runBlocking {
        assertRestoreRejected(rawArchive(repo.snapshot().copy(formatVersion = 99)))
    }

    @Test fun missingImageDoesNotChangeCurrentDataOrImages() = runBlocking {
        assertRestoreRejected(rawArchive(repo.snapshot(), omitImage = true))
    }

    @Test fun unsafeArchivePathDoesNotEscapeStagingOrChangeCurrentData() = runBlocking {
        assertRestoreRejected(rawArchive(repo.snapshot(), extraPath = "images/../../escaped.png"))
        assertFalse(File(context.cacheDir, "escaped.png").exists())
    }

    @Test fun duplicateZipPathIsRejected() = runBlocking {
        // Different legal ZIP names cannot alias the same target file.
        assertRestoreRejected(rawArchive(repo.snapshot(), extraPath = "images/./demo-mira.png"))
    }

    @Test fun brokenEntityReferenceDoesNotChangeCurrentData() = runBlocking {
        val original = repo.snapshot()
        val broken = original.copy(scenes = original.scenes.map { it.copy(characterId = "missing-character") })
        assertRestoreRejected(rawArchive(broken))
    }

    @Test fun corruptImageDoesNotChangeCurrentData() = runBlocking {
        assertRestoreRejected(rawArchive(repo.snapshot(), corruptImage = true))
    }

    @Test fun opaqueImageCannotBeUsedAsCutoutAndFailedImportLeavesNoFile() = runBlocking {
        val before = repo.snapshot()
        val filesBefore = repo.images.list()!!.toSet()
        var rejected = false
        try { repo.importImage("mira", "cutout", ByteArrayInputStream(imageBytes(false))) }
        catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertEquals(before, repo.snapshot())
        assertEquals(filesBefore, repo.images.list()!!.toSet())
    }

    @Test fun selectedPortraitAndBackgroundSurviveRestart() = runBlocking {
        repo.importImage("mira", "portrait", ByteArrayInputStream(imageBytes(false)))
        repo.importImage("docks", "background", ByteArrayInputStream(imageBytes(false)))
        val before = repo.snapshot()
        db.close(); openDatabase()
        val after = repo.snapshot()
        assertEquals(before, after)
        assertEquals(after.world?.coverAssetId, after.scene?.backgroundAssetId)
        assertNotEquals("mira-art", after.entry("mira")?.primaryAssetId)
    }

    @Test fun emptyInputDoesNotAppendTurn() = runBlocking {
        val before = repo.snapshot()
        try { repo.send("   ") } catch (_: IllegalArgumentException) { /* expected */ }
        assertEquals(before, repo.snapshot())
    }

    @Test fun closingScreenDuringRestoreCannotDeleteCommittedImageFiles() = runBlocking {
        val archived = repo.snapshot()
        val archive = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        repo.editEntry("mira", "Edited after backup", "Current text", "Current lore")
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val input = object : FilterInputStream(ByteArrayInputStream(archive)) {
            var firstRead = true
            override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                if (firstRead) {
                    firstRead = false
                    entered.countDown()
                    check(release.await(10, TimeUnit.SECONDS))
                }
                return super.read(buffer, offset, length)
            }
        }
        val restore = launch(Dispatchers.IO) { repo.restore(input) }
        try {
            assertTrue(entered.await(10, TimeUnit.SECONDS))
            restore.cancel() // Equivalent to destruction of the owning ViewModel's scope.
        } finally { release.countDown() }
        restore.join()
        assertTrue(restore.isCancelled)
        val after = repo.snapshot()
        assertEquals(archived.entries, after.entries)
        after.assets.forEach { assertTrue("Committed image must still exist", File(repo.images, it.fileName).isFile) }
        // A new export also exercises the restored graph and every referenced image.
        repo.export(ByteArrayOutputStream())
    }

    @Test fun visualApprovalCorrectionsAndExploratoryExclusion() = runBlocking {
        repo.editEntry("mira", "Mira", "", "", "hidden-author-note")
        repo.saveDescription("mira-art", "Visible amber eyes and a teal coat", false)
        assertFalse(SceneContext.build(repo.snapshot(), "Hello").messages.toString().contains("Visible amber"))
        repo.saveDescription("mira-art", "Visible green eyes and a teal coat", true)
        val approved = SceneContext.build(repo.snapshot(), "Hello").messages.toString()
        assertTrue(approved.contains("Visible green eyes"))
        assertFalse(approved.contains("hidden-author-note"))
        repo.setReferenceKind("mira-art", "exploratory")
        val exploratory = SceneContext.build(repo.snapshot(), "Hello")
        assertFalse(exploratory.messages.toString().contains("Visible green eyes"))
        assertTrue(exploratory.omissions.any { it.contains("Exploratory") })
    }

    @Test fun selectedAlternateAppearanceDoesNotMixOldOutfitsOrRewriteLore() = runBlocking {
        repo.saveDescription("mira-art", "OLD OUTFIT blue cloak", true)
        repo.importImage("mira", "cutout", ByteArrayInputStream(imageBytes(true)))
        val alternate = repo.snapshot().assets.first { it.id !in setOf("mira-art", "harbor-art") }
        repo.setReferenceKind(alternate.id, "alternate")
        repo.saveDescription(alternate.id, "NEW OUTFIT silver cape", true)
        val s = repo.snapshot()
        val context = SceneContext.build(s, "Continue").messages.toString()
        assertTrue(context.contains("NEW OUTFIT"))
        assertFalse(context.contains("OLD OUTFIT"))
        assertTrue(s.entry("mira")!!.lore.contains("unfinished map"))
        val stale = s.copy(descriptions = s.descriptions.map { it.copy(sourceHash = "0".repeat(64)) })
        assertFalse(SceneContext.build(stale, "Continue").messages.toString().contains("NEW OUTFIT"))
    }

    @Test fun authorOnlySecretsNeverReachContextAndTruncationIsInspectable() = runBlocking {
        repo.editEntry("mira", "Mira", "Known face", "L".repeat(30000), "private-origin-998")
        repo.updateScene("Quiet tone", "Traveler", "The ship has arrived", "At the quay", "private-secret-777")
        val context = SceneContext.build(repo.snapshot(), "Where now?", 6000)
        val encoded = Json.encodeToString(context)
        assertFalse(encoded.contains("private-origin-998"))
        assertFalse(encoded.contains("private-secret-777"))
        assertTrue(context.omissions.any { it.contains("omitted") })
        assertTrue(context.messages.sumOf { it.content.length } <= 6000)
    }

    @Test fun cancelledFailedAndDuplicateRequestsNeverAppendExtraTurns() = runBlocking {
        repo.saveDraft("My action")
        val s = repo.snapshot()
        fun record() = GenerationRecord(UUID.randomUUID().toString(), s.scene!!.id, "roleplay", "fixture/text:free", Json.encodeToString(SceneContext.build(s, "My action")), SceneContext.fingerprint(s), "My action", createdAt = 1)
        val cancelled = record()
        repo.beginGeneration(cancelled); repo.failGeneration(cancelled.id, true, "Cancelled")
        repo.finishGeneration(cancelled.id, RouterResult("Must not appear", "fixture/text:free", "{}"))
        assertEquals(s.messages, repo.snapshot().messages)
        assertEquals("My action", repo.snapshot().scene!!.draft)
        val failed = record()
        repo.beginGeneration(failed); repo.failGeneration(failed.id, false, "Rate limit")
        assertEquals(s.messages, repo.snapshot().messages)
        val completed = record()
        repo.beginGeneration(completed)
        val response = RouterResult(Continuity.json.encodeToString(ContinuityReply(1, 0, "One response")), "actual/text:free", "{\"total_tokens\":12}")
        repo.finishGeneration(completed.id, response); repo.finishGeneration(completed.id, response)
        val after = repo.snapshot()
        assertEquals(3, after.messages.size)
        assertEquals("", after.scene!!.draft)
        assertEquals("actual/text:free", after.generations.find { it.id == completed.id }!!.actualModel)
        val archive = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        repo.restore(ByteArrayInputStream(archive))
        assertEquals(after.generations, repo.snapshot().generations)
        assertEquals(after.messages, repo.snapshot().messages)
    }

    @Test fun changedSceneRejectsAStaleResponseAndKeepsDraft() = runBlocking {
        repo.saveDraft("My action")
        val s = repo.snapshot()
        val request = GenerationRecord(UUID.randomUUID().toString(), s.scene!!.id, "roleplay", "fixture/text:free", "{}", SceneContext.fingerprint(s), "My action")
        repo.beginGeneration(request)
        repo.editEntry("mira", "Changed name", "New lore", "Changed")
        var rejected = false
        try { repo.finishGeneration(request.id, RouterResult("Stale answer", "fixture/text:free", "{}")) } catch (_: IllegalArgumentException) { rejected = true }
        assertTrue(rejected)
        assertEquals(s.messages, repo.snapshot().messages)
        assertEquals("My action", repo.snapshot().scene!!.draft)
        assertEquals("Stale answer", repo.snapshot().generations.single().output)
        assertEquals("failed", repo.snapshot().generations.single().status)
    }

    @Test fun approvedChapterMemorySurvivesRestoreWithoutRewritingSources() = runBlocking {
        repo.send("Follow the lanterns")
        repo.saveDescription("mira-art", "Author-approved portrait", true)
        val transcript = repo.snapshot().messages
        repo.saveChapter(null, "The harbor", "MEMORY: We followed the lanterns", false)
        var s = repo.snapshot()
        assertFalse(SceneContext.build(s, "Continue").messages.toString().contains("MEMORY:"))
        repo.saveChapter(s.chapters.single().id, "The harbor", "MEMORY: We followed the lanterns", true)
        assertTrue(SceneContext.build(repo.snapshot(), "Continue").messages.toString().contains("MEMORY:"))
        val archive = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        repo.restore(ByteArrayInputStream(archive))
        s = repo.snapshot()
        assertEquals(transcript, s.messages)
        assertTrue(s.chapters.single().approved)
        assertTrue(s.descriptions.single().approved)
        repo.saveChapter(s.chapters.single().id, "Revised", "A corrected summary", true)
        assertEquals(transcript, repo.snapshot().messages)
        assertEquals(0, SceneContext.sourceMessages(repo.snapshot()).size)
    }

    @Test fun newVisionProposalDoesNotReplaceAnApprovedDescription() = runBlocking {
        repo.saveDescription("mira-art", "Author approved details", true)
        val s = repo.snapshot()
        val request = GenerationRecord(UUID.randomUUID().toString(), s.scene!!.id, "vision", "fixture/vision:free", "{}", SceneContext.fingerprint(s), sourceAssetId = "mira-art", createdAt = System.currentTimeMillis())
        repo.beginGeneration(request)
        repo.finishGeneration(request.id, RouterResult("An unreviewed proposal", "fixture/vision:free", "{}"))
        assertEquals("Author approved details", repo.snapshot().descriptions.single().text)
        assertTrue(repo.snapshot().descriptions.single().approved)
        assertEquals("An unreviewed proposal", repo.snapshot().generations.single().output)
    }

    @Test fun stageOneArchiveUpgradesWithStableIdsAndNoMissingImages() = runBlocking {
        val before = repo.snapshot()
        val legacy = before.copy(formatVersion = 1, assets = before.assets.map { it.copy(contentHash = "") })
        repo.restore(ByteArrayInputStream(rawArchive(legacy)))
        val after = repo.snapshot()
        assertEquals(4, after.formatVersion)
        assertEquals(before.entries, after.entries)
        assertEquals(before.messages, after.messages)
        assertTrue(after.assets.all { it.contentHash.length == 64 && File(repo.images, it.fileName).exists() })
    }

    @Test fun realVersionOneDatabaseMigratesWithoutLosingUserEdits() = runBlocking {
        val before = repo.snapshot()
        db.close(); context.deleteDatabase(dbName)
        val relative = "schemas/org.voidcat.voidcanon.data.CanonDatabase/1.json"
        val schemaFile = listOf(File(relative), File("app/$relative")).first { it.exists() }
        val schema = Json.parseToJsonElement(schemaFile.readText()).jsonObject.getValue("database").jsonObject
        val file = context.getDatabasePath(dbName)
        file.parentFile!!.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
            schema.getValue("entities").jsonArray.forEach { entity ->
                val table = entity.jsonObject
                val sql = table.getValue("createSql").jsonPrimitive.content.replace("${'$'}{TABLE_NAME}", table.getValue("tableName").jsonPrimitive.content)
                old.execSQL(sql)
            }
            schema.getValue("setupQueries").jsonArray.forEach { old.execSQL(it.jsonPrimitive.content) }
            old.execSQL("INSERT INTO worlds VALUES ('glass-harbor','My edited world','My description','harbor-art')")
            old.execSQL("INSERT INTO entries VALUES ('mira','glass-harbor','character','My edited Mira','My description','My lore','mira-art')")
            old.execSQL("INSERT INTO entries VALUES ('docks','glass-harbor','location','The Docks','My location','My location lore','harbor-art')")
            before.assets.forEach { a -> old.execSQL("INSERT INTO assets VALUES (?,?,?,?,?,?,?)", arrayOf<Any>(a.id,a.entityId,a.fileName,a.role,a.label,a.provenance,if(a.transparent)1 else 0)) }
            old.execSQL("INSERT INTO scenes VALUES ('harbor-scene','glass-harbor','mira','docks','harbor-art','mira-art','Retained draft',0,0)")
            old.execSQL("INSERT INTO messages VALUES ('opening','harbor-scene','character','Retained original message',0)")
            old.version = 1
        }
        openDatabase(); repo.initialize()
        val migrated = repo.snapshot()
        assertEquals("My edited Mira", migrated.entry("mira")!!.name)
        assertEquals("Retained draft", migrated.scene!!.draft)
        assertEquals("Retained original message", migrated.messages.single().text)
        assertTrue(migrated.assets.all { it.contentHash.length == 64 })
        assertEquals("", migrated.entry("mira")!!.authorNotes)
        assertTrue(migrated.chapters.isEmpty())
    }

    @Test fun blankWorldAndEntriesPersistWithoutDependingOnAScene() = runBlocking {
        val id = repo.createWorld("Moon Garden", "An author-owned world")
        assertEquals(id, repo.snapshot().world!!.id)
        assertNull(repo.snapshot().scene)
        assertTrue(repo.snapshot().entries.isEmpty())
        val character = repo.createEntry(id, "character", "Kuro", "Dark curls", "Moon lore", "SECRET")
        val location = repo.createEntry(id, "location", "Garden", "Silver leaves", "", "")
        repo.editWorld(id, "Moon Garden Renamed", "Edited")
        repo.editEntry(character, "Kuro Renamed", "Dark curls", "Moon lore")
        db.close(); openDatabase(); repo.initialize()
        val after = repo.snapshot()
        assertEquals(id, after.world!!.id)
        assertEquals("Moon Garden Renamed", after.world!!.name)
        assertNull(after.scene)
        assertEquals(setOf(character, location), after.entries.map { it.id }.toSet())
        assertEquals("SECRET", after.entry(character)!!.authorNotes)
        assertEquals(2, repo.workspaceSnapshot().worlds.size)
    }

    @Test fun scenePairingPreservesDraftsAndDoesNotMixOtherWorldsOrCharacters() = runBlocking {
        repo.send("Harbor transcript")
        repo.saveChapter(null, "Harbor memory", "HARBOR ONLY", true)
        repo.saveDraft("Harbor draft")
        val world = repo.createWorld("Moon", "Moon context")
        val c = repo.createEntry(world, "character", "Kuro", "Moon character", "", "SECRET")
        val l = repo.createEntry(world, "location", "Garden", "Silver leaves", "", "")
        val scene = repo.useInScene(c, l)
        repo.saveDraft("Moon draft")
        assertTrue(repo.snapshot().messages.isEmpty())
        assertTrue(repo.snapshot().chapters.isEmpty())
        val prompt = SceneContext.build(repo.snapshot(), "Hello").messages.joinToString { it.content }
        assertTrue(prompt.contains("Moon context")); assertFalse(prompt.contains("HARBOR ONLY")); assertFalse(prompt.contains("SECRET"))
        val c2 = repo.createEntry(world, "character", "Another character", "", "", "")
        val second = repo.useInScene(c2, l)
        assertNotEquals(scene, second)
        repo.saveDraft("Other character draft")
        assertEquals(scene, repo.useInScene(c, l))
        assertEquals("Moon draft", repo.snapshot().scene!!.draft)
        repo.selectWorld("glass-harbor")
        assertEquals("Harbor draft", repo.snapshot().scene!!.draft)
        assertEquals(3, repo.snapshot().messages.size)
        repo.selectWorld(world)
        assertEquals(scene, repo.snapshot().scene!!.id)
        assertEquals("Moon draft", repo.snapshot().scene!!.draft)
        assertTrue(SceneContext.sourceMessages(repo.snapshot()).isEmpty())
        assertTrue(runCatching { repo.useInScene(c, "docks") }.isFailure)
        db.close(); openDatabase(); repo.initialize()
        assertEquals(world, repo.snapshot().world!!.id)
        assertEquals(scene, repo.snapshot().scene!!.id)
        assertEquals("Moon draft", repo.snapshot().scene!!.draft)
    }

    @Test fun pendingResultUsesItsOriginalSceneEvenAfterOtherWorldEdits() = runBlocking {
        val before = repo.snapshot()
        val request = GenerationRecord(UUID.randomUUID().toString(), before.scene!!.id, "roleplay", "fixture/free", "{}", SceneContext.fingerprint(before), "Harbor action")
        repo.beginGeneration(request)
        val world = repo.createWorld("Elsewhere", "Completely independent")
        repo.createEntry(world, "character", "Someone else", "", "", "")
        repo.finishGeneration(request.id, RouterResult("Harbor reply", "fixture/free", "{}"))
        assertTrue(repo.snapshot().messages.isEmpty())
        repo.selectWorld("glass-harbor")
        assertEquals("Harbor reply", repo.snapshot().messages.last().text)
        assertEquals("completed", repo.snapshot().generations.single().status)
    }

    @Test fun artworkCanBeImportedBeforeSceneAndSelectedWithoutChangingOtherWorld() = runBlocking {
        val sample = repo.snapshot()
        val world = repo.createWorld("Visual world", "")
        val c = repo.createEntry(world, "character", "Kuro", "", "", "")
        val l = repo.createEntry(world, "location", "Garden", "", "", "")
        repo.importImage(c, "cutout", ByteArrayInputStream(imageBytes(true)))
        repo.importImage(l, "background", ByteArrayInputStream(imageBytes(false)))
        assertNull(repo.snapshot().scene)
        val images = repo.snapshot().assets
        val cutout = images.single { it.entityId == c }.id
        val background = images.single { it.entityId == l }.id
        repo.saveDescription(cutout, "Approved Kuro appearance", true)
        repo.useInScene(c, l)
        assertEquals(cutout, repo.snapshot().scene!!.cutoutAssetId)
        assertEquals(background, repo.snapshot().scene!!.backgroundAssetId)
        assertTrue(SceneContext.build(repo.snapshot(), "Hello").messages.first().content.contains("Approved Kuro appearance"))
        repo.selectWorld("glass-harbor")
        assertEquals(sample.world, repo.snapshot().world)
        assertEquals(sample.scene, repo.snapshot().scene)
    }

    @Test fun libraryBackupRestoresMultipleWorldsBlankWorldAndImageReferences() = runBlocking {
        val world = repo.createWorld("New world", "")
        val c = repo.createEntry(world, "character", "Kuro", "", "", "")
        repo.importImage(c, "portrait", ByteArrayInputStream(imageBytes(true)))
        val l = repo.createEntry(world, "location", "Garden", "", "", "")
        repo.useInScene(c, l)
        val current = repo.snapshot()
        val generation = GenerationRecord(UUID.randomUUID().toString(), current.scene!!.id, "roleplay", "fixture/free", "{}", SceneContext.fingerprint(current), "Moon action")
        repo.beginGeneration(generation)
        repo.finishGeneration(generation.id, RouterResult("Moon reply", "fixture/free", "{}"))
        repo.saveChapter(null, "Moon chapter", "Moon memory", true)
        repo.createWorld("Unwritten", "No entries or scene yet")
        val before = repo.workspaceSnapshot()
        val bytes = before.assets.associate { it.id to File(repo.images, it.fileName).readBytes() }
        val archive = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        repo.createWorld("Later", "This should disappear")
        repo.restore(ByteArrayInputStream(archive))
        val after = repo.workspaceSnapshot()
        assertEquals(before.worlds, after.worlds)
        assertEquals(before.entries, after.entries)
        assertEquals(before.scenes, after.scenes)
        assertEquals(before.messages, after.messages)
        assertEquals(before.chapters, after.chapters)
        assertEquals(before.generations, after.generations)
        assertEquals(4, after.formatVersion)
        after.assets.forEach { assertArrayEquals(bytes[it.id], File(repo.images, it.fileName).readBytes()) }
        BackupArchive.validateGraph(after)
    }

    @Test fun crossWorldAndCrossSceneArchiveReferencesFailWithoutReplacingLibrary() = runBlocking {
        repo.saveChapter(null, "Harbor", "Original memory", true)
        val world = repo.createWorld("Another", "")
        val c = repo.createEntry(world, "character", "Kuro", "", "", "")
        val l = repo.createEntry(world, "location", "Garden", "", "", "")
        val scene = repo.useInScene(c, l)
        val before = repo.workspaceSnapshot()
        val broken = listOf(
            before.copy(scenes = before.scenes.map { if (it.id == scene) it.copy(locationId = "docks") else it }),
            before.copy(worlds = before.worlds.map { if (it.id == world) it.copy(coverAssetId = "harbor-art") else it }),
            before.copy(chapters = before.chapters.map { it.copy(sceneId = scene) }))
        broken.forEach { candidate ->
            assertTrue(runCatching { repo.restore(ByteArrayInputStream(rawArchive(candidate))) }.isFailure)
            assertEquals(before, repo.workspaceSnapshot())
        }
    }

    @Test fun versionTwoArchiveRemainsImportableWithItsStableIds() = runBlocking {
        val before = repo.workspaceSnapshot()
        repo.restore(ByteArrayInputStream(rawArchive(before.copy(formatVersion = 2))))
        assertEquals(before.worlds, repo.workspaceSnapshot().worlds)
        assertEquals(before.scenes, repo.workspaceSnapshot().scenes)
        assertEquals(4, repo.workspaceSnapshot().formatVersion)
    }

    private suspend fun assertRestoreRejected(bytes: ByteArray) {
        val before = repo.snapshot()
        val images = before.assets.associate { it.fileName to File(repo.images, it.fileName).readBytes() }
        var failed = false
        try { repo.restore(ByteArrayInputStream(bytes)) } catch (_: Exception) { failed = true }
        assertTrue("Restore must fail", failed)
        assertEquals(before, repo.snapshot())
        images.forEach { (file, bytesBefore) -> assertArrayEquals(bytesBefore, File(repo.images, file).readBytes()) }
    }

    @Test fun generatedArtworkPreservesOwnerBaselineAndProvenanceAcrossBackup() = runBlocking {
        val before = repo.snapshot()
        val draft = GeneratedImageDraft(UUID.randomUUID().toString(), "mira", "Mira", "Change only the outfit", "portrait",
            "mira-cutout", before.assets.first { it.entityId == "mira" }.contentHash, CloudflareImageClient.MODEL,
            "preview.png", System.currentTimeMillis(), 32)
        repo.createWorld("Another world", "")
        repo.importGeneratedImage(draft, imageBytes(false).inputStream())
        repo.importGeneratedImage(draft, imageBytes(false).inputStream())
        val all = repo.workspaceSnapshot()
        val output = all.assets.single { it.id == draft.requestId }
        assertEquals("mira", output.entityId)
        assertEquals("exploratory", output.referenceKind)
        assertTrue(output.provenance.contains(draft.prompt))
        assertTrue(output.provenance.contains(draft.model))
        assertEquals(before.entries.first { it.id == "mira" }.primaryAssetId, all.entries.first { it.id == "mira" }.primaryAssetId)
        assertEquals(before.scene!!.backgroundAssetId, all.scenes.first { it.id == before.scene!!.id }.backgroundAssetId)
        val archive = ByteArrayOutputStream().also { repo.export(it) }.toByteArray()
        repo.restore(archive.inputStream())
        val restored = repo.workspaceSnapshot().assets.first { it.id == draft.requestId }
        assertEquals(output.copy(fileName = restored.fileName), restored)
        assertEquals(output.contentHash, imageHash(File(repo.images, restored.fileName)))
    }

    @Test fun generatedPreviewSurvivesReopenAndChecksOutputDimensions() {
        val files = File(context.cacheDir, "draft-test-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val store = GeneratedImageDraftStore(files, context.cacheDir)
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888).apply { eraseColor(android.graphics.Color.BLUE) }
            val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
            bitmap.recycle()
            val result = GeneratedImage(bytes, CloudflareImageClient.MODEL, UUID.randomUUID().toString(), 27)
            val draft = store.save("mira", "Mira", "A portrait", ImageSize.Portrait, null, null, result)
            val file = File(store.directory, draft.fileName)
            val originalHash = imageHash(file)
            val reference = ImageFiles.generationReference(file)
            val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
            android.graphics.BitmapFactory.decodeByteArray(reference, 0, reference.size, bounds)
            assertEquals(448, bounds.outWidth); assertEquals(448, bounds.outHeight)
            assertEquals(originalHash, imageHash(file))
            assertEquals(draft, GeneratedImageDraftStore(files, context.cacheDir).load())
            assertTrue(runCatching { store.save("mira", "Mira", "A second", ImageSize.Portrait, null, null, result) }.isFailure)
            store.clear(); assertNull(store.load())
            assertTrue(runCatching { store.save("mira", "Mira", "Wrong dimensions", ImageSize.Large, null, null, result) }.isFailure)
            assertNull(store.load())
        } finally { files.deleteRecursively() }
    }

    private fun imageBytes(transparent: Boolean): ByteArray {
        val bitmap = Bitmap.createBitmap(24, 24, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(if (transparent) 0x00000000 else 0xFF334455.toInt())
        bitmap.setPixel(12, 12, 0xFF7755AA.toInt())
        return ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it); bitmap.recycle() }.toByteArray()
    }

    private fun rawArchive(s: WorldSnapshot, omitImage: Boolean = false, extraPath: String? = null, corruptImage: Boolean = false): ByteArray {
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            zip.putNextEntry(ZipEntry("manifest.json")); zip.write(BackupArchive.json.encodeToString(s).toByteArray()); zip.closeEntry()
            s.assets.forEachIndexed { i, asset ->
                if (!(omitImage && i == 0)) {
                    zip.putNextEntry(ZipEntry("images/${asset.fileName}"))
                    zip.write(if (corruptImage && i == 0) "invalid png".toByteArray() else File(repo.images, asset.fileName).readBytes())
                    zip.closeEntry()
                }
            }
            if (extraPath != null) { zip.putNextEntry(ZipEntry(extraPath)); zip.write(byteArrayOf(1)); zip.closeEntry() }
        }
        return out.toByteArray()
    }
}
