package org.voidcat.voidcanon.data

import android.graphics.BitmapFactory
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.withTransaction
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.voidcat.voidcanon.ai.*
import java.io.File
import java.util.UUID

@Entity(tableName = "illustration_jobs")
data class IllustrationJob(
    @PrimaryKey val id: String, val sceneId: String, val worldId: String, val visualKey: String,
    val planJson: String, val automatic: Boolean, val status: String, val createdAt: Long,
    val lastUsedAt: Long = 0, val dispatchedAt: Long = 0, val day: String = "", val fileName: String = "", val error: String = "",
) {
    fun plan() = Json.decodeFromString<ImagePromptPlan>(planJson)
}

@Entity(tableName = "illustration_frames")
data class IllustrationFrame(@PrimaryKey val sceneId: String, val planJson: String)

@Entity(tableName = "illustration_allowance")
data class IllustrationAllowance(@PrimaryKey val day: String, val attempts: Int, val lastDispatchAt: Long)

/** Operational outbox/cache only. Neither table participates in WorldSnapshot or story fingerprints. */
class IllustrationStore(private val db: CanonDatabase, files: File, private val scratch: File) {
    val directory = File(files, "scene-illustrations").apply { mkdirs() }
    private val dao get() = db.canonDao()
    suspend fun jobs() = dao.illustrationJobs()
    suspend fun frame(sceneId: String) = dao.illustrationFrame(sceneId)?.let { decode(it.planJson) }
    fun file(job: IllustrationJob?): File? = job?.fileName?.takeIf { it.matches(Regex("[a-zA-Z0-9-]+\\.png")) }
        ?.let { File(directory, it) }?.takeIf { it.isFile }

    suspend fun recover() = db.withTransaction {
        jobs().filter { it.status == "submitted" }.forEach { dao.putIllustration(it.copy(status = "interrupted", error = "Interrupted; no automatic retry. Regenerate explicitly.")) }
    }

    suspend fun plan(s: WorldSnapshot, prefs: ArtworkPreferences, beat: VisualBeat? = null, force: Boolean = false): ImagePromptPlan = db.withTransaction {
        val scene = requireNotNull(s.scene)
        val live = dao.snapshot().scoped(scene.worldId, scene.id)
        require(SceneContext.fingerprint(s) == SceneContext.fingerprint(live)) { "Scene updated; waiting for its latest visual state." }
        val plan = ArtworkPlanning.build(s, prefs.brief(scene.id), prefs.style(scene.worldId), frame(scene.id), beat)
        record(dao, plan, prefs.enabled(scene.id), force)
        plan
    }

    suspend fun claim(id: String, day: String, now: Long): IllustrationJob? = db.withTransaction {
        val jobs = jobs()
        val row = jobs.firstOrNull { it.id == id && it.status == "pending" } ?: return@withTransaction null
        if (dao.illustrationFrame(row.sceneId)?.let { decode(it.planJson)?.visualKey } != row.visualKey) return@withTransaction null
        if (row.automatic && ((dao.illustrationAllowances().firstOrNull { it.day == day }?.attempts ?: 0) >= DAILY_ATTEMPTS ||
                    dao.illustrationAllowances().maxOfOrNull { it.lastDispatchAt }?.let { now - it < COOLDOWN_MS } == true)) return@withTransaction null
        if (row.automatic) {
            val used = dao.illustrationAllowances().firstOrNull { it.day == day }?.attempts ?: 0
            dao.putIllustrationAllowance(IllustrationAllowance(day, used + 1, now))
        }
        row.copy(status = "submitted", dispatchedAt = now, day = day).also { dao.putIllustration(it) }
    }

    suspend fun finish(job: IllustrationJob, image: GeneratedImage): IllustrationJob {
        val (file, _) = ImageFiles.import(image.bytes.inputStream(), scratch, directory)
        try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            require(bounds.outWidth == 512 && bounds.outHeight == 512) { "Unexpected scene image dimensions." }
            val row = db.withTransaction {
                val current = jobs().firstOrNull { it.id == job.id && it.status == "submitted" }
                current?.copy(status = "complete", fileName = file.name, lastUsedAt = System.currentTimeMillis())?.also { dao.putIllustration(it) }
            }
            if (row == null) { file.delete(); error("This illustration was cancelled or its library was restored.") }
            return row
        } catch (e: Exception) { file.delete(); throw e }
    }

    suspend fun fail(id: String, status: String, message: String) = db.withTransaction {
        jobs().firstOrNull { it.id == id && it.status in setOf("pending", "submitted") }
            ?.let { dao.putIllustration(it.copy(status = status, error = message.take(1000))) }
    }

    suspend fun cancelPending(sceneId: String) = db.withTransaction {
        jobs().filter { it.sceneId == sceneId && it.status == "pending" }.forEach { dao.putIllustration(it.copy(status = "cancelled")) }
    }

    suspend fun touch(id: String, now: Long) = db.withTransaction {
        jobs().firstOrNull { it.id == id && it.status == "complete" && now - it.lastUsedAt > 60000 }
            ?.let { dao.putIllustration(it.copy(lastUsedAt = now)) }
    }

    suspend fun prune(protect: String? = null, maxBytes: Long = 50L * 1024 * 1024) {
        val rows = jobs().filter { it.status == "complete" }.sortedBy { it.lastUsedAt }
        val used = jobs().map { it.fileName }.toSet()
        directory.listFiles()?.filter { it.name !in used }?.forEach { it.delete() }
        var total = directory.listFiles()?.sumOf { it.length() } ?: 0L
        for (row in rows) {
            if (total <= maxBytes) break
            if (row.id == protect) continue
            val file = file(row) ?: continue
            val length = file.length()
            if (file.delete()) { total -= length; dao.putIllustration(row.copy(fileName = "")) }
        }
    }

    companion object {
        const val DAILY_ATTEMPTS = 30
        const val COOLDOWN_MS = 30000L
        fun decode(text: String): ImagePromptPlan? = runCatching { Json.decodeFromString<ImagePromptPlan>(text) }.getOrNull()

        /** Call in the same transaction as the accepted RP turn. Invalid optional hints never enter here. */
        suspend fun record(dao: CanonDao, plan: ImagePromptPlan, enabled: Boolean, force: Boolean = false) {
            dao.putIllustrationFrame(IllustrationFrame(plan.snapshot.sceneId, Json.encodeToString(plan)))
            if (!enabled && !force) return
            val rows = dao.illustrationJobs()
            rows.filter { it.status == "pending" && (it.automatic || it.sceneId == plan.snapshot.sceneId) && (force || it.visualKey != plan.visualKey) }
                .forEach { dao.putIllustration(it.copy(status = "superseded")) }
            // Submitted, failed and interrupted attempts are never replayed automatically.
            if (!force && rows.any { it.sceneId == plan.snapshot.sceneId && it.visualKey == plan.visualKey }) return
            dao.putIllustration(IllustrationJob(UUID.randomUUID().toString(), plan.snapshot.sceneId, plan.snapshot.worldId,
                plan.visualKey, Json.encodeToString(plan), !force, "pending", maxOf(System.currentTimeMillis(), (rows.maxOfOrNull { it.createdAt } ?: 0L) + 1)))
        }
    }
}
