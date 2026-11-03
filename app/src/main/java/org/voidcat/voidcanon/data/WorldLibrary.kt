package org.voidcat.voidcanon.data

/** Presentation selection is device-local; portable records contain only stable domain IDs. */
data class LibrarySnapshot(val selected: WorldSnapshot, val worlds: List<WorldRecord>, val scenes: List<SceneRecord>)

fun WorldSnapshot.scoped(worldId: String, sceneId: String? = null): WorldSnapshot {
    val world = worlds.find { it.id == worldId } ?: return WorldSnapshot()
    val worldEntries = entries.filter { it.worldId == worldId }
    val entryIds = worldEntries.map { it.id }.toSet()
    val worldAssets = assets.filter { it.entityId in entryIds }
    val assetIds = worldAssets.map { it.id }.toSet()
    val scene = scenes.find { it.id == sceneId && it.worldId == worldId }
    return copy(worlds = listOf(world), entries = worldEntries, assets = worldAssets,
        scenes = listOfNotNull(scene), messages = messages.filter { scene != null && it.sceneId == scene.id },
        descriptions = descriptions.filter { it.assetId in assetIds },
        chapters = chapters.filter { scene != null && it.sceneId == scene.id },
        generations = generations.filter { scene != null && it.sceneId == scene.id })
}
