package org.voidcat.voidcanon.ai

import androidx.room.withTransaction
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.voidcat.voidcanon.data.*
import java.io.File
import java.time.Instant
import java.time.ZoneOffset

class ImageLane {
    val mutex = Mutex()
    val busy = MutableStateFlow(false)
    var manualWaiting = false
}

data class ArtworkUiState(
    val plan: ImagePromptPlan? = null, val job: IllustrationJob? = null, val image: File? = null,
    val enabled: Boolean = false, val illustrated: Boolean = true, val running: Boolean = false,
    val used: Int = 0, val status: String = "Automatic artwork is off.",
)

/** Foreground-only dispatcher. A conflated channel keeps text commits independent of slow images. */
class ArtworkDirector(
    private val db: CanonDatabase, private val store: IllustrationStore, private val prefs: ArtworkPreferences,
    private val connection: () -> ImageConnection, private val connected: () -> Boolean, private val client: () -> CloudflareImageClient,
    private val files: File, private val lane: ImageLane, private val scope: CoroutineScope,
    private val now: () -> Long = System::currentTimeMillis,
    private val recover: suspend () -> Unit = { store.recover() },
    private val canDispatch: () -> Boolean = { true },
    private val retain: suspend (IllustrationJob, File) -> Unit,
) {
    val ui = MutableStateFlow(ArtworkUiState())
    private val updates = Channel<Unit>(Channel.CONFLATED)
    private var source = WorldSnapshot()
    private var visible = false
    private var request: Job? = null
    private var timer: Job? = null
    private var requestScene: String? = null
    private var connectionRejected = false
    private val retentionLock = Mutex()
    private val retained = mutableSetOf<String>()
    private val retentionErrors = mutableMapOf<String, String>()

    private suspend fun retainCompleted() = retentionLock.withLock {
        for (job in store.jobs().filter { it.status == "complete" && it.id !in retained }) {
            val file = store.file(job) ?: continue
            try {
                retain(job, file)
                retained += job.id
                retentionErrors.remove(job.id)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { retentionErrors[job.id] = "Gallery save failed: ${e.message ?: "Try saving again."}" }
        }
    }

    init {
        scope.launch { lane.busy.collect { if (!it) refresh() } }
        scope.launch {
            recover()
            for (ignored in updates) {
                try { reconcile() }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) { ui.value = ui.value.copy(status = e.message ?: "Artwork is unavailable; your story is safe.") }
            }
        }
    }
    fun source(snapshot: WorldSnapshot) {
        val previous = source
        source = snapshot
        // Draft keystrokes and save timestamps do not need database/cache planning.
        if (previous.world?.id != snapshot.world?.id || previous.scene?.id != snapshot.scene?.id ||
            previous.scene?.continuityJson != snapshot.scene?.continuityJson || previous.assets != snapshot.assets ||
            previous.entries != snapshot.entries || previous.descriptions != snapshot.descriptions ||
            previous.messages.lastOrNull()?.id != snapshot.messages.lastOrNull()?.id) refresh()
    }
    fun visible(value: Boolean) { visible = value; refresh() }
    fun refresh() { updates.trySend(Unit) }
    fun reconnected() { connectionRejected = false; refresh() }
    fun pause(sceneId: String) {
        prefs.pause(sceneId)
        if (requestScene == sceneId) request?.cancel()
        scope.launch { store.cancelPending(sceneId); refresh() }
    }
    fun regenerate() {
        val s = source
        val scene = s.scene ?: return
        if (!prefs.authorized(scene.id)) { ui.value = ui.value.copy(status = "Allow artwork uploads in scene artwork settings first."); return }
        scope.launch {
            try { store.plan(s, prefs, force = true); refresh() }
            catch (e: Exception) { ui.value = ui.value.copy(status = e.message.orEmpty()) }
        }
    }
    fun imageFile() = ui.value.image

    private suspend fun reconcile() {
        retainCompleted()
        timer?.cancel(); timer = null
        val s = source
        val scene = s.scene
        if (scene == null) { ui.value = ArtworkUiState(); return }
        val enabled = prefs.enabled(scene.id)
        // Planning is local; no connection or text-model request is made here.
        val plan = try { store.plan(s, prefs) } catch (e: Exception) {
            ui.value = ArtworkUiState(enabled = enabled, illustrated = prefs.illustrated(scene.id), status = e.message.orEmpty())
            return
        }
        val jobs = store.jobs().filter { it.sceneId == scene.id && it.visualKey == plan.visualKey }
        val cached = jobs.lastOrNull { it.status == "complete" && store.file(it) != null }
        if (cached != null) store.touch(cached.id, now())
        val job = jobs.lastOrNull()
        val day = Instant.ofEpochMilli(now()).atZone(ZoneOffset.UTC).toLocalDate().toString()
        val allowance = db.canonDao().illustrationAllowances()
        val used = allowance.firstOrNull { it.day == day }?.attempts ?: 0
        var status = when {
            request != null && requestScene == scene.id -> "Rendering artwork…"
            job?.status == "pending" -> "Artwork queued."
            job?.status in setOf("failed", "interrupted", "cancelled") -> job?.error?.ifBlank { "Artwork stopped. Regenerate explicitly." }.orEmpty()
            cached != null -> "Scene illustration ready."
            job?.status == "complete" -> "Illustration left the cache. Regenerate to draw it again."
            !enabled -> "Automatic artwork is off."
            else -> "Waiting for a visible moment."
        }
        if (!visible && enabled) status = "Automatic artwork resumes in Play."
        cached?.id?.let { id -> retentionLock.withLock { retentionErrors[id] } }?.let { status = it }
        ui.value = ArtworkUiState(plan, cached ?: job, store.file(cached), enabled, prefs.illustrated(scene.id),
            request != null && requestScene == scene.id, used, status)
        if (!visible || !canDispatch() || request != null || lane.manualWaiting || lane.busy.value || job?.status != "pending") return
        if (job.automatic && !enabled) return
        if (!connected() || connectionRejected) { ui.value = ui.value.copy(status = "Connect the image service in Settings to continue."); return }
        if (prefs.blockedDay() == day) { ui.value = ui.value.copy(status = "Image allowance exhausted. Resumes after the UTC daily reset."); return }
        if (job.automatic && used >= IllustrationStore.DAILY_ATTEMPTS) { ui.value = ui.value.copy(status = "30 automatic attempts used today. Manual Gallery generation remains available within the service budget."); return }
        val remaining = if (job.automatic) (IllustrationStore.COOLDOWN_MS - (now() - (allowance.maxOfOrNull { it.lastDispatchAt } ?: 0))).coerceAtLeast(0) else 0
        if (remaining > 0) {
            ui.value = ui.value.copy(status = "Waiting briefly before the next illustration.")
            timer = scope.launch { delay(remaining); refresh() }
            return
        }
        requestScene = scene.id
        request = scope.launch {
            try {
                lane.mutex.withLock {
                    // Re-check after a manual request or navigation won the lane.
                    if (!visible || !canDispatch() || source.scene?.id != job.sceneId || ui.value.plan?.visualKey != job.visualKey || lane.manualWaiting ||
                        (job.automatic && !prefs.enabled(job.sceneId))) return@withLock
                    lane.busy.value = true
                    try {
                        ui.value = ui.value.copy(running = true, status = "Checking image allowance…")
                        val service = connection()
                        val remote = client().status(service)
                        require(remote.day == day) { "Daily allowance changed; regenerate explicitly." }
                        if (remote.remaining < plan.estimatedNeurons) {
                            prefs.block(day)
                            ui.value = ui.value.copy(status = "The image service's daily allowance is used up.")
                            return@withLock
                        }
                        val reference = plan.snapshot.referenceId?.let { id ->
                            val asset = requireNotNull(s.asset(id)) { "Visual reference is unavailable." }
                            val file = File(files, asset.fileName)
                            withContext(Dispatchers.IO) {
                                require(asset.entityId == plan.snapshot.ownerId && imageHash(file) == plan.snapshot.referenceHash) { "The visual reference changed; choose it again." }
                                ImageFiles.generationReference(file)
                            }
                        }
                        // The transaction also checks the stored desired fingerprint before reserving an attempt.
                        if (source.scene?.id != job.sceneId || ui.value.plan?.visualKey != job.visualKey || !visible || !canDispatch() ||
                            (job.automatic && !prefs.enabled(job.sceneId))) return@withLock
                        val claimed = store.claim(job.id, day, now()) ?: return@withLock
                        if (!visible || !canDispatch() || source.scene?.id != job.sceneId || ui.value.plan?.visualKey != job.visualKey ||
                            (job.automatic && !prefs.enabled(job.sceneId))) {
                            store.fail(claimed.id, "cancelled", "Scene changed before upload. Regenerate explicitly if needed.")
                            return@withLock
                        }
                        ui.value = ui.value.copy(status = "Rendering artwork…")
                        val image = client().generate(service, claimed.id, plan.prompt, ImageSize.Portrait, reference)
                        withContext(NonCancellable + Dispatchers.IO) {
                            store.finish(claimed, image)
                            retainCompleted()
                            store.prune(if (ui.value.plan?.visualKey == job.visualKey) job.id else ui.value.job?.id)
                        }
                    } finally { lane.busy.value = false }
                }
            } catch (e: CancellationException) {
                withContext(NonCancellable) { store.fail(job.id, "cancelled", "Artwork cancelled; no automatic retry. Provider work may still have been counted.") }
                throw e
            } catch (e: Exception) {
                if (e is ImageServiceException) {
                    if (e.status in setOf(401, 403)) connectionRejected = true
                    if (e.status == 429) prefs.block(day)
                }
                store.fail(job.id, "failed", e.message ?: "Artwork failed. Your story is unchanged.")
            } finally {
                request = null; requestScene = null
                refresh()
            }
        }
    }
}
