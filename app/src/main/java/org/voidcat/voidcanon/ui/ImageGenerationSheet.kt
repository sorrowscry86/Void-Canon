@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package org.voidcat.voidcanon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.voidcat.voidcanon.CanonViewModel
import org.voidcat.voidcanon.ai.ImageSize
import org.voidcat.voidcanon.data.EntryRecord
import org.voidcat.voidcanon.data.WorldSnapshot

@Composable
fun ImageGenerationSheet(snapshot: WorldSnapshot, owner: EntryRecord, vm: CanonViewModel, dismiss: () -> Unit) {
    val connected by vm.imageConnected.collectAsStateWithLifecycle()
    val generating by vm.generating.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val preview by vm.imagePreview.collectAsStateWithLifecycle()
    val error by vm.imageError.collectAsStateWithLifecycle()
    val active = generating != null
    var prompt by rememberSaveable(owner.id) { mutableStateOf("") }
    var sizeName by rememberSaveable(owner.id) { mutableStateOf(if (owner.kind == "location") ImageSize.Background.name else ImageSize.Portrait.name) }
    var referenceId by rememberSaveable(owner.id) { mutableStateOf<String?>(null) }
    var consent by rememberSaveable { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).imePadding()) {
            SheetHeader("Generate artwork", dismiss)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Cloudflare · FLUX.2 Klein 4B", color = Violet)
                ImageConnectionControls(vm)
                if (preview != null) {
                    val draft = preview!!
                    Text("Preview for ${draft.entityName}", style = MaterialTheme.typography.titleLarge)
                    AsyncImage(model = vm.generatedPreviewFile(), contentDescription = "Generated image preview", contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 440.dp))
                    Text(draft.prompt, color = Muted)
                    Text("Saved images keep the prompt, model and selected reference identity. This image has not replaced a portrait or background.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = vm::saveGeneratedImage, enabled = !busy && !active, modifier = Modifier.fillMaxWidth()) { Text("Save to Gallery") }
                    OutlinedButton(onClick = vm::discardGeneratedImage, enabled = !busy && !active, modifier = Modifier.fillMaxWidth()) { Text("Discard preview") }
                } else {
                    Text("For ${owner.name}", style = MaterialTheme.typography.titleLarge)
                    OutlinedTextField(prompt, { if (it.length <= 2000) prompt = it }, label = { Text("Image prompt") },
                        placeholder = { Text("Describe the image, or the change to your reference…") }, minLines = 3, maxLines = 8,
                        enabled = !active && !busy, modifier = Modifier.fillMaxWidth(), supportingText = { Text("${prompt.length} / 2000") })
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageSize.entries.forEach { size -> FilterChip(selected = sizeName == size.name, onClick = { sizeName = size.name }, enabled = !active && !busy,
                            label = { Text(size.title) }) }
                    }
                    Text("Reference image (optional)", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = referenceId == null, onClick = { referenceId = null }, enabled = !active && !busy, label = { Text("Text only") })
                        snapshot.assets.filter { it.entityId == owner.id }.forEach { asset ->
                            FilterChip(selected = referenceId == asset.id, onClick = { referenceId = asset.id }, enabled = !active && !busy, label = { Text(asset.label) })
                        }
                    }
                    referenceId?.let { id -> AsyncImage(model = vm.imageFile(id), contentDescription = "Selected reference to send", contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxWidth().height(140.dp)) }
                    Text("Only this prompt and optional reference are sent to Cloudflare. References are reduced to 448 pixels per side. Story history, lore and author notes are not sent.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Text("Development service budget: 5,000 estimated neurons daily. Cloudflare's 10,000-neuron free allowance is shared with other account usage. No retries or alternate models. These images are not transparent cutouts.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Row {
                        Checkbox(checked = consent, onCheckedChange = { consent = it }, enabled = !active)
                        Text("Allow sending this prompt and selected reference to Cloudflare", modifier = Modifier.padding(top = 12.dp))
                    }
                    Button(onClick = { vm.generateArtwork(owner.id, prompt, ImageSize.valueOf(sizeName), referenceId, consent) },
                        enabled = connected && consent && prompt.isNotBlank() && !active && !busy, modifier = Modifier.fillMaxWidth()) { Text("Generate image") }
                }
                if (active) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text("Generating image…", color = Muted)
                    OutlinedButton(onClick = vm::cancelGeneration, modifier = Modifier.fillMaxWidth()) { Text("Cancel generation") }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
