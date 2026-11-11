@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package org.voidcat.voidcanon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.voidcat.voidcanon.CanonViewModel

@Composable
fun ArtworkSettingsSheet(vm: CanonViewModel, close: () -> Unit) {
    val s by vm.snapshot.collectAsStateWithLifecycle()
    val state by vm.artwork.collectAsStateWithLifecycle()
    val connected by vm.imageConnected.collectAsStateWithLifecycle()
    val descriptionStatus by vm.artworkDescriptionStatus.collectAsStateWithLifecycle()
    val scene = s.scene
    var enabled by rememberSaveable(scene?.id) { mutableStateOf(scene?.let { vm.artworkPreferences.enabled(it.id) } ?: false) }
    var brief by rememberSaveable(scene?.id) { mutableStateOf(scene?.let { vm.artworkPreferences.brief(it.id) }.orEmpty()) }
    var style by rememberSaveable(s.world?.id) { mutableStateOf(s.world?.let { vm.artworkPreferences.style(it.id) }.orEmpty()) }
    var describe by rememberSaveable(scene?.id) { mutableStateOf(scene?.let { vm.artworkPreferences.describeForRoleplay(it.id) } ?: false) }
    var promptOpen by remember { mutableStateOf(false) }
    var saveError by rememberSaveable(scene?.id) { mutableStateOf<String?>(null) }
    if (promptOpen) { ArtworkPromptSheet(vm) { promptOpen = false }; return }
    ModalBottomSheet(onDismissRequest = close, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.94f).imePadding()) {
            SheetHeader("Scene artwork", close)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (scene == null) Text("Choose a scene in World before configuring automatic artwork.", color = Muted) else {
                    Text("${s.entry(scene.characterId)?.name} · ${s.entry(scene.locationId)?.name}", style = MaterialTheme.typography.titleLarge)
                    Text(state.status, color = Violet)
                    Text(descriptionStatus, style = MaterialTheme.typography.bodySmall, color = Muted)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { vm.setIllustratedView(!state.illustrated) }) { Text(if (state.illustrated) "Layered view" else "Illustrated view") }
                        if (state.enabled) TextButton(onClick = { enabled = false; vm.pauseArtwork() }) { Text("Pause artwork") }
                        if (state.plan != null) TextButton(onClick = { promptOpen = true }) { Text("View prompt") }
                    }
                    Text("Meaningful visual changes produce 512 × 512 illustrations in Play. Dialogue continues while an image renders. Canonical artwork and story records stay under your control.", color = Muted)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(enabled, { enabled = it; saveError = null })
                        Text("Allow automatic uploads of selected visual details, visible story moments and one selected reference to Cloudflare", Modifier.weight(1f))
                    }
                    if (!connected) Text("You can save settings now. Import your image connection in Settings → Artwork connection before generation can start.", color = Violet)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(describe, { describe = it; saveError = null })
                        Text("Automatically describe generated images for roleplay using my selected OpenRouter image model", Modifier.weight(1f))
                    }
                    Text("One image-understanding request per image, respecting Free only. This sends the generated image to OpenRouter. Its observations can inform the current scene without manual approval; review or correct them in Gallery. They do not become canon or replace identity references.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    OutlinedTextField(brief, { brief = it.take(800) }, label = { Text("Character visual brief (optional)") },
                        supportingText = { Text("Use visible traits only. Blank uses the selected approved description or image reference.") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(style, { style = it.take(300) }, label = { Text("World artwork style (optional)") },
                        supportingText = { Text("Shared by this world's scenes on this device. Blank matches the reference style.") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                    Text("${state.used} / 30 automatic attempts used this UTC day across this device. Failed or interrupted attempts can count. The existing shared service allowance also applies; there are no automatic retries.", color = Muted)
                    Text("Completed illustrations and their scene captions save to Gallery automatically. Captions describe the scene context; they are not approved image interpretations. Private notes and character knowledge are excluded. Upload permission and artwork preferences stay on this device.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    saveError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Button(onClick = { saveError = vm.configureArtwork(enabled, brief, style, describe); if (saveError == null) close() }, modifier = Modifier.fillMaxWidth()) { Text("Save artwork settings") }
                }
            }
        }
    }
}

@Composable
private fun ArtworkPromptSheet(vm: CanonViewModel, close: () -> Unit) {
    val state by vm.artwork.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val textGenerating by vm.generating.collectAsStateWithLifecycle()
    val imageBusy by vm.imageBusy.collectAsStateWithLifecycle()
    val manual by vm.manualImageGenerating.collectAsStateWithLifecycle()
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val descriptionStatus by vm.artworkDescriptionStatus.collectAsStateWithLifecycle()
    val plan = state.job?.takeIf { state.image != null }?.plan() ?: state.plan
    ModalBottomSheet(onDismissRequest = close, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.94f)) {
            SheetHeader("Artwork prompt", close)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(state.status, color = Violet)
                plan?.let {
                    Text(it.reason, style = MaterialTheme.typography.titleMedium)
                    Text(it.caption(), style = MaterialTheme.typography.bodyMedium)
                    Text(descriptionStatus, color = Muted)
                    snapshot.descriptions.firstOrNull { description -> description.assetId == state.job?.id }?.let { description ->
                        Text("Generated image description", style = MaterialTheme.typography.titleMedium)
                        Text(description.text)
                    }
                    state.image?.let { file -> AsyncImage(file, "Current scene illustration", Modifier.fillMaxWidth().height(280.dp)) }
                    Text(it.prompt)
                    Text("512 × 512 · ${it.estimatedNeurons} estimated neurons · ${state.used}/30 automatic attempts this UTC day", color = Muted)
                    if (it.visualEvidence.isNotBlank()) Text("Visible source quote: ${it.visualEvidence}", color = Muted)
                    Text("Sources: ${it.sourceIds.joinToString()}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    if (it.omissions.isNotEmpty()) Text("Omitted for prompt length: ${it.omissions.joinToString()}", color = Muted)
                    Text("Reference: ${it.snapshot.referenceId ?: "Text only"}", color = Muted)
                    TextButton(onClick = vm::regenerateArtwork, enabled = !state.running && !manual && !busy) { Text("Regenerate image") }
                    if (snapshot.assets.any { asset -> asset.id == state.job?.id }) Text("Saved to Gallery automatically", color = Violet)
                    else if (state.image != null) Button(onClick = vm::keepIllustration, enabled = !busy && !imageBusy && textGenerating == null && !manual) { Text("Retry Gallery save") }
                }
            }
        }
    }
}
