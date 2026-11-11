package org.voidcat.voidcanon

import android.app.Application
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.voidcat.voidcanon.ai.ImageLane
import org.voidcat.voidcanon.data.IllustrationStore
import androidx.room.Room
import org.voidcat.voidcanon.data.CanonDatabase
import org.voidcat.voidcanon.data.CanonRepository
import org.voidcat.voidcanon.data.MIGRATION_1_2
import org.voidcat.voidcanon.data.MIGRATION_3_4
import org.voidcat.voidcanon.data.MIGRATION_5_6
import org.voidcat.voidcanon.data.MIGRATION_4_5
import org.voidcat.voidcanon.data.MIGRATION_2_3
import org.voidcat.voidcanon.ai.OpenRouterSettings
import org.voidcat.voidcanon.ai.OpenRouterClient

class VoidCanonApplication : Application() {
    val imageLane = ImageLane()
    private val artworkRecoveryLock = Mutex()
    private var artworkRecovered = false
    suspend fun recoverArtwork(store: IllustrationStore) = artworkRecoveryLock.withLock {
        if (!artworkRecovered) { store.recover(); artworkRecovered = true }
    }
    val database by lazy { Room.databaseBuilder(this, CanonDatabase::class.java, "void-canon.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build() }
    val aiSettings by lazy { OpenRouterSettings(this) }
    var aiClient = OpenRouterClient()
    val deepSeek by lazy { org.voidcat.voidcanon.ai.DeepSeekBackup(this) }
    var deepSeekClient = OpenRouterClient(baseUrl = "https://api.deepseek.com/v1/", providerName = "DeepSeek", routerRouting = false)
    val imageSettings by lazy { org.voidcat.voidcanon.ai.ImageGenerationSettings(this) }
    var imageClient = org.voidcat.voidcanon.ai.CloudflareImageClient()
    val repository by lazy { CanonRepository(this, database) }
}
