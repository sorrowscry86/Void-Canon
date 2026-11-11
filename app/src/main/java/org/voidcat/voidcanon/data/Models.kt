package org.voidcat.voidcanon.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Embedded
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "worlds")
data class WorldRecord(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val coverAssetId: String?,
)

@Serializable
@Entity(tableName = "entries")
data class EntryRecord(
    @PrimaryKey val id: String,
    val worldId: String,
    val kind: String,
    val name: String,
    val description: String,
    val lore: String = "",
    val primaryAssetId: String? = null,
    @ColumnInfo(defaultValue = "''") val authorNotes: String = "",
)

@Serializable
@Entity(tableName = "assets")
data class AssetRecord(
    @PrimaryKey val id: String,
    val entityId: String,
    val fileName: String,
    val role: String,
    val label: String,
    val provenance: String,
    val transparent: Boolean,
    @ColumnInfo(defaultValue = "''") val contentHash: String = "",
    @ColumnInfo(defaultValue = "'canon'") val referenceKind: String = "canon",
)

@Serializable
@Entity(tableName = "scenes")
data class SceneRecord(
    @PrimaryKey val id: String,
    val worldId: String,
    val characterId: String,
    val locationId: String,
    val backgroundAssetId: String?,
    val cutoutAssetId: String?,
    val draft: String = "",
    val demoStep: Int = 0,
    val savedAt: Long = 0,
    @ColumnInfo(defaultValue = "''") val authorDirection: String = "",
    @ColumnInfo(defaultValue = "'Traveler'") val userRole: String = "Traveler",
    @ColumnInfo(defaultValue = "''") val characterKnowledge: String = "",
    @ColumnInfo(defaultValue = "''") val provisionalState: String = "",
    @ColumnInfo(defaultValue = "''") val authorNotes: String = "",
    @ColumnInfo(defaultValue = "''") val continuityJson: String = "",
    @Embedded(prefix = "narrative_") val narrative: NarrativeSettings = NarrativeSettings(),
    @ColumnInfo(defaultValue = "''") val aurpgJson: String = "",
)

@Serializable
@Entity(tableName = "messages")
data class MessageRecord(
    @PrimaryKey val id: String,
    val sceneId: String,
    val speaker: String,
    val text: String,
    val sequence: Long,
    val generationId: String? = null,
    @ColumnInfo(defaultValue = "'demo'") val mode: String = "demo",
)

@Serializable
@Entity(tableName = "visual_descriptions")
data class VisualDescription(
    @PrimaryKey val id: String,
    val assetId: String,
    val sourceHash: String,
    val text: String,
    val approved: Boolean,
    val provenance: String,
    val updatedAt: Long,
)

@Serializable
@Entity(tableName = "chapters")
data class ChapterRecord(
    @PrimaryKey val id: String,
    val sceneId: String,
    val title: String,
    val summary: String,
    val sourceMessageIdsJson: String,
    val approved: Boolean,
    val updatedAt: Long,
)

@Serializable
@Entity(tableName = "generations")
data class GenerationRecord(
    @PrimaryKey val id: String,
    val sceneId: String,
    val purpose: String,
    val model: String,
    val contextJson: String,
    val fingerprint: String,
    val userText: String = "",
    val sourceAssetId: String? = null,
    val sourceMessageIdsJson: String = "[]",
    val status: String = "pending",
    val actualModel: String = "",
    val output: String = "",
    val usageJson: String = "{}",
    val error: String = "",
    val createdAt: Long = 0,
)

/** Portable world-library data contract. Local filenames carry no device-specific paths. */
@Serializable
data class WorldSnapshot(
    val formatVersion: Int = 6,
    val exportedAt: String = "",
    val worlds: List<WorldRecord> = emptyList(),
    val entries: List<EntryRecord> = emptyList(),
    val assets: List<AssetRecord> = emptyList(),
    val scenes: List<SceneRecord> = emptyList(),
    val messages: List<MessageRecord> = emptyList(),
    val descriptions: List<VisualDescription> = emptyList(),
    val chapters: List<ChapterRecord> = emptyList(),
    val generations: List<GenerationRecord> = emptyList(),
    val continuityEvents: List<ContinuityEvent> = emptyList(),
) {
    val world: WorldRecord? get() = worlds.firstOrNull()
    val scene: SceneRecord? get() = scenes.firstOrNull()
    fun entry(id: String?) = entries.find { it.id == id }
    fun asset(id: String?) = assets.find { it.id == id }
}
