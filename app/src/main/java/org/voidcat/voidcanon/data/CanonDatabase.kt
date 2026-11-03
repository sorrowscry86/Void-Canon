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

    // Call within a Room transaction for a consistent graph.
    suspend fun snapshot() = WorldSnapshot(
        worlds = worlds(), entries = entries(), assets = assets(), scenes = scenes(), messages = messages(),
        descriptions = descriptions(), chapters = chapters(), generations = generations(),
    )
    suspend fun replace(snapshot: WorldSnapshot) {
        clearGenerations(); clearChapters(); clearDescriptions()
        clearMessages(); clearScenes(); clearAssets(); clearEntries(); clearWorlds()
        putWorlds(snapshot.worlds); putEntries(snapshot.entries); putAssets(snapshot.assets)
        putScenes(snapshot.scenes); putMessages(snapshot.messages)
        putDescriptions(snapshot.descriptions); putChapters(snapshot.chapters); putGenerations(snapshot.generations)
    }
}

@Database(
    entities = [WorldRecord::class, EntryRecord::class, AssetRecord::class, SceneRecord::class, MessageRecord::class, VisualDescription::class, ChapterRecord::class, GenerationRecord::class],
    version = 2, exportSchema = true,
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
