package org.voidcat.voidcanon

import android.app.Application
import androidx.room.Room
import org.voidcat.voidcanon.data.CanonDatabase
import org.voidcat.voidcanon.data.CanonRepository
import org.voidcat.voidcanon.data.MIGRATION_1_2
import org.voidcat.voidcanon.data.MIGRATION_2_3
import org.voidcat.voidcanon.ai.OpenRouterSettings
import org.voidcat.voidcanon.ai.OpenRouterClient

class VoidCanonApplication : Application() {
    val database by lazy { Room.databaseBuilder(this, CanonDatabase::class.java, "void-canon.db").addMigrations(MIGRATION_1_2, MIGRATION_2_3).build() }
    val aiSettings by lazy { OpenRouterSettings(this) }
    val aiClient by lazy { OpenRouterClient() }
    val repository by lazy { CanonRepository(this, database) }
}
