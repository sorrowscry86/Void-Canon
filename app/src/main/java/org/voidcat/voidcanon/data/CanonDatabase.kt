package org.voidcat.voidcanon.data

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Dao
abstract class CanonDao {
    @Query("SELECT * FROM worlds ORDER BY id") abstract suspend fun worlds(): List<WorldRecord>
    @Query("SELECT * FROM entries ORDER BY kind, id") abstract suspend fun entries(): List<EntryRecord>
    @Query("SELECT * FROM assets ORDER BY id") abstract suspend fun assets(): List<AssetRecord>
    @Query("SELECT * FROM scenes ORDER BY id") abstract suspend fun scenes(): List<SceneRecord>
    @Query("SELECT * FROM messages ORDER BY sequence, id") abstract suspend fun messages(): List<MessageRecord>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putWorlds(rows: List<WorldRecord>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putEntries(rows: List<EntryRecord>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putAssets(rows: List<AssetRecord>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putScenes(rows: List<SceneRecord>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putMessages(rows: List<MessageRecord>)
    @Query("DELETE FROM worlds") abstract suspend fun clearWorlds()
    @Query("DELETE FROM entries") abstract suspend fun clearEntries()
    @Query("DELETE FROM assets") abstract suspend fun clearAssets()
    @Query("DELETE FROM scenes") abstract suspend fun clearScenes()
    @Query("DELETE FROM messages") abstract suspend fun clearMessages()

    @Query("SELECT * FROM visual_descriptions ORDER BY id") abstract suspend fun descriptions(): List<VisualDescription>
    @Query("SELECT * FROM chapters ORDER BY updatedAt, id") abstract suspend fun chapters(): List<ChapterRecord>
    @Query("SELECT * FROM generations ORDER BY createdAt, id") abstract suspend fun generations(): List<GenerationRecord>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putDescriptions(rows: List<VisualDescription>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putChapters(rows: List<ChapterRecord>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putGenerations(rows: List<GenerationRecord>)
    @Query("DELETE FROM visual_descriptions") abstract suspend fun clearDescriptions()
    @Query("DELETE FROM chapters") abstract suspend fun clearChapters()
    @Query("DELETE FROM generations") abstract suspend fun clearGenerations()

    @Query("SELECT * FROM continuity_events ORDER BY createdAt, id") abstract suspend fun continuityEvents(): List<ContinuityEvent>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putContinuityEvents(rows: List<ContinuityEvent>)
    @Query("DELETE FROM continuity_events") abstract suspend fun clearContinuityEvents()

    @Query("SELECT * FROM illustration_jobs ORDER BY createdAt, id") abstract suspend fun illustrationJobs(): List<IllustrationJob>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putIllustration(row: IllustrationJob)
    @Query("SELECT * FROM illustration_frames WHERE sceneId=:sceneId") abstract suspend fun illustrationFrame(sceneId: String): IllustrationFrame?
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putIllustrationFrame(row: IllustrationFrame)
    @Query("DELETE FROM illustration_jobs") abstract suspend fun clearIllustrations()
    @Query("SELECT * FROM illustration_allowance") abstract suspend fun illustrationAllowances(): List<IllustrationAllowance>
    @Insert(onConflict = OnConflictStrategy.REPLACE) abstract suspend fun putIllustrationAllowance(row: IllustrationAllowance)
    @Query("DELETE FROM illustration_frames") abstract suspend fun clearIllustrationFrames()

    // Call within a Room transaction for a consistent graph.
    suspend fun snapshot() = WorldSnapshot(
        worlds = worlds(), entries = entries(), assets = assets(), scenes = scenes(), messages = messages(),
        descriptions = descriptions(), chapters = chapters(), generations = generations(), continuityEvents = continuityEvents(),
    )
    suspend fun replace(snapshot: WorldSnapshot) {
        clearIllustrations(); clearIllustrationFrames()
        clearContinuityEvents(); clearGenerations(); clearChapters(); clearDescriptions()
        clearMessages(); clearScenes(); clearAssets(); clearEntries(); clearWorlds()
        putWorlds(snapshot.worlds); putEntries(snapshot.entries); putAssets(snapshot.assets)
        putScenes(snapshot.scenes); putMessages(snapshot.messages)
        putDescriptions(snapshot.descriptions); putChapters(snapshot.chapters); putGenerations(snapshot.generations); putContinuityEvents(snapshot.continuityEvents)
    }
}

@Database(
    entities = [WorldRecord::class, EntryRecord::class, AssetRecord::class, SceneRecord::class, MessageRecord::class, VisualDescription::class, ChapterRecord::class, GenerationRecord::class, ContinuityEvent::class, IllustrationJob::class, IllustrationFrame::class, IllustrationAllowance::class],
    version = 6, exportSchema = true,
)
abstract class CanonDatabase : RoomDatabase() {
    abstract fun canonDao(): CanonDao
}

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE entries ADD COLUMN authorNotes TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE assets ADD COLUMN contentHash TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE assets ADD COLUMN referenceKind TEXT NOT NULL DEFAULT 'canon'")
        for (column in listOf("authorDirection", "characterKnowledge", "provisionalState", "authorNotes"))
            db.execSQL("ALTER TABLE scenes ADD COLUMN $column TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE scenes ADD COLUMN userRole TEXT NOT NULL DEFAULT 'Traveler'")
        db.execSQL("ALTER TABLE messages ADD COLUMN generationId TEXT")
        db.execSQL("ALTER TABLE messages ADD COLUMN mode TEXT NOT NULL DEFAULT 'demo'")
        db.execSQL("CREATE TABLE IF NOT EXISTS visual_descriptions (id TEXT NOT NULL PRIMARY KEY, assetId TEXT NOT NULL, sourceHash TEXT NOT NULL, text TEXT NOT NULL, approved INTEGER NOT NULL, provenance TEXT NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS chapters (id TEXT NOT NULL PRIMARY KEY, sceneId TEXT NOT NULL, title TEXT NOT NULL, summary TEXT NOT NULL, sourceMessageIdsJson TEXT NOT NULL, approved INTEGER NOT NULL, updatedAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS generations (id TEXT NOT NULL PRIMARY KEY, sceneId TEXT NOT NULL, purpose TEXT NOT NULL, model TEXT NOT NULL, contextJson TEXT NOT NULL, fingerprint TEXT NOT NULL, userText TEXT NOT NULL, sourceAssetId TEXT, sourceMessageIdsJson TEXT NOT NULL, status TEXT NOT NULL, actualModel TEXT NOT NULL, output TEXT NOT NULL, usageJson TEXT NOT NULL, error TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE scenes ADD COLUMN continuityJson TEXT NOT NULL DEFAULT ''")
        db.execSQL("CREATE TABLE IF NOT EXISTS continuity_events (id TEXT NOT NULL PRIMARY KEY, sceneId TEXT NOT NULL, sourceMessageId TEXT, beforeJson TEXT NOT NULL, afterJson TEXT NOT NULL, createdAt INTEGER NOT NULL)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS illustration_jobs (id TEXT NOT NULL PRIMARY KEY, sceneId TEXT NOT NULL, worldId TEXT NOT NULL, visualKey TEXT NOT NULL, planJson TEXT NOT NULL, automatic INTEGER NOT NULL, status TEXT NOT NULL, createdAt INTEGER NOT NULL, lastUsedAt INTEGER NOT NULL, dispatchedAt INTEGER NOT NULL, day TEXT NOT NULL, fileName TEXT NOT NULL, error TEXT NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS illustration_allowance (day TEXT NOT NULL PRIMARY KEY, attempts INTEGER NOT NULL, lastDispatchAt INTEGER NOT NULL)")
        db.execSQL("CREATE TABLE IF NOT EXISTS illustration_frames (sceneId TEXT NOT NULL PRIMARY KEY, planJson TEXT NOT NULL)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for ((column, value) in listOf("mode" to "classic", "preset" to "collaborative", "pacing" to "balanced", "length" to "standard"))
            db.execSQL("ALTER TABLE scenes ADD COLUMN narrative_$column TEXT NOT NULL DEFAULT '$value'")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE scenes ADD COLUMN aurpgJson TEXT NOT NULL DEFAULT ''")
    }
}
