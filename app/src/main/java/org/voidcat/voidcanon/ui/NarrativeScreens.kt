@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.voidcat.voidcanon.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.serialization.json.Json
import org.voidcat.voidcanon.CanonViewModel
import org.voidcat.voidcanon.data.*
import java.text.DateFormat
import java.util.Date

@Composable
fun AiSettingsSheet(vm: CanonViewModel, dismiss: () -> Unit) {
    var deepSeekOpen by remember { mutableStateOf(false) }
    if (deepSeekOpen) {
        key("deepseek-connection") { DeepSeekSheet(vm) { deepSeekOpen = false } }
    } else {
        key("openrouter-connection") { OpenRouterSettingsSheet(vm, dismiss) { deepSeekOpen = true } }
    }
}

@Composable
private fun OpenRouterSettingsSheet(vm: CanonViewModel, dismiss: () -> Unit, openDeepSeek: () -> Unit) {
    val direct by vm.deepSeekActive.collectAsStateWithLifecycle()
    val stored by vm.configuration.collectAsStateWithLifecycle()
    var config by remember { mutableStateOf(stored) }
    var key by remember { mutableStateOf("") }
    val hasKey by vm.hasKey.collectAsStateWithLifecycle()
    val models by vm.models.collectAsStateWithLifecycle()
    val refreshing by vm.loadingModels.collectAsStateWithLifecycle()
    val catalogTime by vm.catalogTime.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var visionOnly by rememberSaveable { mutableStateOf(false) }
    var freeVariants by rememberSaveable { mutableStateOf(true) }
    val visible = models.filter { (!config.freeOnly || it.free) && (!config.freeOnly || !freeVariants || it.id.endsWith(":free") || it.id == "openrouter/free") && (!visionOnly || it.vision) && (it.name.contains(query, true) || it.id.contains(query, true)) }
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).imePadding()) {
            SheetHeader("OpenRouter", dismiss) {
                TextButton(onClick = { vm.saveAiSettings(config, key); key = ""; dismiss() }) { Text("Save") }
            }
            LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item {
                    Text(if (direct) "Text provider: DeepSeek (direct)" else "Text provider: OpenRouter", color = Violet)
                    OutlinedButton(onClick = openDeepSeek, modifier = Modifier.fillMaxWidth()) { Text("DeepSeek direct backup") }
                    Text("Choose your models", style = MaterialTheme.typography.titleLarge)
                    Text("Browse the current free options without entering a key. Refresh checks OpenRouter's live catalog; availability and rate limits can change.", color = Muted)
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Free only")
                            Text("Zero-price catalog entries and zero-price routing caps", style = MaterialTheme.typography.bodySmall, color = Muted)
                        }
                        Switch(config.freeOnly, { config = config.copy(freeOnly = it) })
                    }
                    Text("Free mode never switches to a paid model. If no free endpoint is available, the request fails safely. Turning Free only off allows your chosen model to spend OpenRouter credits.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text("Free variants do not require a credit purchase. Other $0 models may require a funded account. Free models still have quotas and can become unavailable.", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                item {
                    OutlinedTextField(key, { key = it }, label = { Text(if (hasKey) "New key (leave blank to keep saved key)" else "OpenRouter API key") },
                        visualTransformation = PasswordVisualTransformation(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), modifier = Modifier.fillMaxWidth())
                    Text("The key is encrypted with Android Keystore on this device and excluded from exports. Never send it in chat or put it in lore.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    if (hasKey) TextButton(onClick = vm::clearKey) { Text("Remove saved key") }
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(config.consent, { config = config.copy(consent = it) })
                        Text("Allow selected story text to be sent to OpenRouter and its model provider.", modifier = Modifier.weight(1f))
                    }
                    Text("RP sends lore, approved visual descriptions, recent dialogue, and approved chapter memory. Image-description actions send the one selected image. Author-only notes are excluded. Requests are explicit: images allow 1,000 output tokens, RP with state and summaries up to 4,096 (including model reasoning), and no automatic retries.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                item {
                    Text("Roleplay & summaries: ${config.rpModel.ifBlank { "not selected" }}", color = Violet)
                    Text("Image descriptions: ${config.visionModel.ifBlank { "not selected" }}", color = Violet)
                    OutlinedButton(onClick = vm::refreshModels, enabled = !refreshing, modifier = Modifier.fillMaxWidth()) {
                        Text(if (refreshing) "Refreshing…" else "Refresh current models")
                    }
                    Text(if (catalogTime == 0L) "No catalog loaded yet." else "Cached catalog: ${DateFormat.getDateTimeInstance().format(Date(catalogTime))}", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
                item {
                    OutlinedTextField(query, { query = it }, label = { Text("Find a model") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    FilterChip(visionOnly, { visionOnly = !visionOnly }, label = { Text("Image-capable only") })
                    if (config.freeOnly) FilterChip(freeVariants, { freeVariants = !freeVariants }, label = { Text("Free variants · no credit purchase") })
                    Text("${visible.size} matching models", style = MaterialTheme.typography.bodySmall, color = Muted)
                    if (visible.isEmpty()) Text("No matching options in this catalog. Refresh or change the filters. Free vision availability varies; manual descriptions always work offline.", color = Muted)
                }
                items(visible, key = { it.id }) { model ->
                    Surface(color = Ink, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Line)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(model.name, style = MaterialTheme.typography.titleMedium)
                            Text(model.id, style = MaterialTheme.typography.bodySmall, color = Muted)
                            Text("${if (model.free) "FREE" else "Paid"} · ${if (model.vision) "Text + images" else "Text"} · ${model.contextLength} context tokens", color = Violet, fontSize = 12.sp)
                            if (!model.free) Text("USD/token: input ${model.promptPrice}, output ${model.completionPrice}", style = MaterialTheme.typography.bodySmall, color = Muted)
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { config = config.copy(rpModel = model.id) }) { Text(if (config.rpModel == model.id) "RP selected ✓" else "Use for RP") }
                                if (model.vision) OutlinedButton(onClick = { config = config.copy(visionModel = model.id) }) { Text(if (config.visionModel == model.id) "Images selected ✓" else "Use for images") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DescriptionDialog(s: WorldSnapshot, asset: AssetRecord, vm: CanonViewModel, dismiss: () -> Unit) {
    val existing = s.descriptions.find { it.assetId == asset.id }
    val proposal = s.generations.lastOrNull { it.sourceAssetId == asset.id && it.purpose == "vision" && it.status == "completed" && it.createdAt > (existing?.updatedAt ?: 0) }
    var provenance by rememberSaveable(asset.id, existing?.updatedAt) { mutableStateOf(existing?.provenance ?: "Author-written") }
    var text by rememberSaveable(asset.id, existing?.updatedAt) { mutableStateOf(existing?.text.orEmpty()) }
    var remoteConfirmation by remember { mutableStateOf(false) }
    val generating by vm.generating.collectAsStateWithLifecycle()
    AlertDialog(onDismissRequest = dismiss, title = { Text("Visual description") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(asset.label, color = Violet)
            Text(when { existing == null -> "No description yet. Write observable details or request a draft from the image."; existing.sourceHash != asset.contentHash -> "Source changed: approval is stale. Review and approve against this image."; existing.approved -> "Approved · linked to this source image"; else -> "Draft · excluded from RP until approved" }, color = Muted)
            OutlinedTextField(text, { if (it.length <= 10000) text = it }, label = { Text("Visible appearance or setting") }, minLines = 5, maxLines = 9)
            if (proposal != null) TextButton(onClick = { text = proposal.output; provenance = "OpenRouter · ${proposal.actualModel.ifBlank { proposal.model }} · author reviewed" }) { Text("Load latest AI proposal") }
            Text("Describe visible details and uncertainty. Do not infer motives, history, ancestry, or personality from an image.", style = MaterialTheme.typography.bodySmall, color = Muted)
            Text(provenance, color = Muted, style = MaterialTheme.typography.bodySmall)
            TextButton(onClick = { remoteConfirmation = true }, enabled = generating == null) { Text("Propose from image…") }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("canon", "alternate", "exploratory").forEach { kind -> FilterChip(asset.referenceKind == kind, { vm.setReferenceKind(asset.id, kind) }, label = { Text(kind.replaceFirstChar { it.uppercase() }, fontSize = 11.sp) }) }
            }
            Text("Exploratory references stay out of scene context. Alternate appearances are used only when selected for the scene.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }, confirmButton = { TextButton(onClick = { vm.saveDescription(asset.id, text, true, provenance); dismiss() }, enabled = text.isNotBlank() && generating == null) { Text("Approve") } },
        dismissButton = { Row { TextButton(onClick = { vm.saveDescription(asset.id, text, false, provenance); dismiss() }, enabled = generating == null) { Text("Save draft") }; TextButton(onClick = dismiss) { Text("Close") } } })
    if (remoteConfirmation) AlertDialog(onDismissRequest = { remoteConfirmation = false }, title = { Text("Send this image to OpenRouter?") },
        text = { Text("Only ${asset.label} and an observation prompt will be sent. The result stays a draft until you review it. Your selected model's limits and pricing apply; Free only caps routing prices at zero.") },
        confirmButton = { TextButton(onClick = { remoteConfirmation = false; dismiss(); vm.describeImage(asset.id) }) { Text("Send selected image") } },
        dismissButton = { TextButton(onClick = { remoteConfirmation = false }) { Text("Cancel") } })
}

@Composable
fun SceneDetailsDialog(s: WorldSnapshot, vm: CanonViewModel, dismiss: () -> Unit) {
    val scene = s.scene ?: return
    var stateOpen by remember { mutableStateOf(false) }
    if (stateOpen) ContinuitySheet(s, vm) { stateOpen = false }
    var direction by rememberSaveable { mutableStateOf(scene.authorDirection) }
    var role by rememberSaveable { mutableStateOf(scene.userRole) }
    var known by rememberSaveable { mutableStateOf(scene.characterKnowledge) }
    var provisional by rememberSaveable { mutableStateOf(scene.provisionalState) }
    var notes by rememberSaveable { mutableStateOf(scene.authorNotes) }
    if (!stateOpen) AlertDialog(onDismissRequest = dismiss, title = { Text("Scene & knowledge") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextButton(onClick = { stateOpen = true }) { Text("Review scene continuity") }
            OutlinedTextField(role, { role = it.take(5000) }, label = { Text("Your role") })
            OutlinedTextField(direction, { direction = it.take(5000) }, label = { Text("Author direction (sent to RP)") }, minLines = 2)
            OutlinedTextField(known, { known = it.take(5000) }, label = { Text("Facts the character knows") }, minLines = 2)
            OutlinedTextField(provisional, { provisional = it.take(5000) }, label = { Text("Provisional scene state") }, minLines = 2)
            OutlinedTextField(notes, { notes = it.take(5000) }, label = { Text("Author-only notes / secrets") }, minLines = 2)
            Text("Private notes never enter model context. Lore and character-known facts do; move secrets into the private field. Model compliance with supplied knowledge is not guaranteed.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { vm.saveSceneDetails(direction, role, known, provisional, notes); dismiss() }) { Text("Save scene details") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
fun ContextSheet(s: WorldSnapshot, vm: CanonViewModel, dismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.94f)) {
            SheetHeader("Scene context", dismiss)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Text("Author-only notes are excluded. RP uses approved descriptions; it sends no source images.", color = Muted) }
                val preview = runCatching { vm.contextPreview() }
                preview.getOrNull()?.let { context ->
                    item { Text("Next request preview · ${context.budgetCharacters} character budget", color = Violet) }
                    item { Text("Play settings: " + context.settings.filterKeys { it != "continuityProtocol" }.entries.joinToString { "${it.key}=${it.value}" }, color = Muted, style = MaterialTheme.typography.bodySmall) }
                    if (context.omissions.isNotEmpty()) item { Text("Omissions / missing references\n" + context.omissions.joinToString("\n"), color = MaterialTheme.colorScheme.error) }
                    items(context.messages) { message -> Text("${message.role.uppercase()}\n${message.content}") }
                    item { Text("Source IDs: ${context.sourceIds.joinToString()}", style = MaterialTheme.typography.bodySmall, color = Muted) }
                }
                preview.exceptionOrNull()?.let { item { Text(it.message.orEmpty()) } }
                item { HorizontalDivider(); Text("Recorded requests", style = MaterialTheme.typography.titleLarge) }
                items(s.generations.asReversed(), key = { it.id }) { generation ->
                    var expanded by remember { mutableStateOf(false) }
                    TextButton(onClick = { expanded = !expanded }) { Text("${generation.purpose} · ${generation.status} · ${generation.actualModel.ifBlank { generation.model }}") }
                    if (expanded) Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(generation.contextJson, style = MaterialTheme.typography.bodySmall)
                        if (generation.error.isNotBlank()) Text(generation.error, color = MaterialTheme.colorScheme.error)
                        Text("Reported usage: ${generation.usageJson}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
fun ChaptersScreen(s: WorldSnapshot, vm: CanonViewModel) {
    val direct by vm.deepSeekActive.collectAsStateWithLifecycle()
    val textProvider = if (direct) "DeepSeek" else "OpenRouter"
    var editing by remember { mutableStateOf<ChapterRecord?>(null) }
    var newChapter by remember { mutableStateOf(false) }
    val generating by vm.generating.collectAsStateWithLifecycle()
    val summaryError by vm.summaryError.collectAsStateWithLifecycle()
    val lastSummary = s.generations.lastOrNull { it.purpose == "summary" }
    val error = summaryError ?: lastSummary?.takeIf { it.status in listOf("failed", "cancelled") }?.error?.takeIf { it.isNotBlank() }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Chapters", style = MaterialTheme.typography.headlineLarge)
            Text("Preserve the transcript. Approve the memory you carry forward.", color = Muted)
            OutlinedButton(onClick = { newChapter = true }, modifier = Modifier.fillMaxWidth()) { Text("Write chapter summary") }
            OutlinedButton(onClick = vm::proposeChapter, enabled = generating == null, modifier = Modifier.fillMaxWidth()) { Text(if (error != null && generating == null) "Retry summary with $textProvider" else "Propose summary with $textProvider") }
            if (error != null && generating != "summary") {
                Text("Summary wasn't saved. Your transcript is unchanged.", color = MaterialTheme.colorScheme.error)
                Text(error, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Text("Proposals cover messages after the last approved chapter. They remain drafts until approved; editing a summary never edits its source messages.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        if (s.chapters.isEmpty()) item { Text("Your chapter library is waiting. A manual summary works offline.", color = Muted) }
        items(s.chapters.asReversed(), key = { it.id }) { chapter ->
            Surface(onClick = { editing = chapter }, shape = RoundedCornerShape(16.dp), color = Panel, border = BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (chapter.approved) "APPROVED MEMORY" else "DRAFT · NOT IN CONTEXT", color = Violet, fontSize = 11.sp)
                    Text(chapter.title, style = MaterialTheme.typography.titleLarge)
                    Text(chapter.summary.ifBlank { "No summary yet" }, maxLines = 6, color = Muted)
                    val ids = runCatching { Json.decodeFromString<List<String>>(chapter.sourceMessageIdsJson) }.getOrDefault(emptyList())
                    Text("${ids.size} source messages · Tap to review", style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
        }
    }
    if (newChapter || editing != null) {
        val chapter = editing
        var title by rememberSaveable(chapter?.id) { mutableStateOf(chapter?.title ?: "Chapter ${s.chapters.size + 1}") }
        var summary by rememberSaveable(chapter?.id) { mutableStateOf(chapter?.summary.orEmpty()) }
        AlertDialog(onDismissRequest = { newChapter = false; editing = null }, title = { Text("Review chapter") }, text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(title, { title = it.take(200) }, label = { Text("Chapter title") })
                OutlinedTextField(summary, { summary = it.take(20000) }, label = { Text("Editable memory summary") }, minLines = 6, maxLines = 10)
                if (chapter != null) {
                    val ids = runCatching { Json.decodeFromString<List<String>>(chapter.sourceMessageIdsJson) }.getOrDefault(emptyList())
                    Text("Source transcript (read only)", color = Violet)
                    s.messages.filter { it.id in ids }.forEach { Text("${it.speaker}: ${it.text}", style = MaterialTheme.typography.bodySmall) }
                }
            }
        }, confirmButton = { TextButton(onClick = { vm.saveChapter(chapter?.id, title, summary, true); newChapter = false; editing = null }, enabled = title.isNotBlank() && summary.isNotBlank()) { Text("Approve chapter") } },
            dismissButton = { Row { TextButton(onClick = { vm.saveChapter(chapter?.id, title, summary, false); newChapter = false; editing = null }, enabled = title.isNotBlank()) { Text("Save draft") }; TextButton(onClick = { newChapter = false; editing = null }) { Text("Cancel") } } })
    }
}
