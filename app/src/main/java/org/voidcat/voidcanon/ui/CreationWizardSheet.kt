@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package org.voidcat.voidcanon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.voidcat.voidcanon.CanonViewModel

@Composable
fun CreationWizardSheet(vm: CanonViewModel, dismiss: () -> Unit, ai: () -> Unit) {
    val draft by vm.wizardDraft.collectAsStateWithLifecycle()
    val generating by vm.generating.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val error by vm.wizardError.collectAsStateWithLifecycle()
    val s by vm.snapshot.collectAsStateWithLifecycle()
    val d = draft ?: return
    var review by rememberSaveable(d.id) { mutableStateOf(d.name.isNotBlank()) }
    var discard by remember { mutableStateOf(false) }
    val enabled = generating == null && !busy
    LaunchedEffect(d.proposalRevision) { if (d.proposalRevision > 0) review = true }
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.95f).imePadding()) {
            SheetHeader(if (d.mode == "world") "World creation wizard" else "Character creation wizard", dismiss)
            if (generating != null) Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                Text("Working: $generating", Modifier.weight(1f))
                TextButton(onClick = vm::cancelGeneration) { Text("Cancel request") }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Your draft saves on this device. Close and resume from World. Saved creations join normal backups; unfinished wizard drafts stay on this device.", color = Muted, style = MaterialTheme.typography.bodySmall)
                if (error != null) Text(error.orEmpty(), color = MaterialTheme.colorScheme.error)
                if (!review) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(d.mode == "character", { vm.updateWizard(d.copy(mode = "character", name = "", description = "", lore = "", entries = emptyList(), model = "")) }, enabled = enabled, label = { Text("Character") })
                        FilterChip(d.mode == "world", { vm.updateWizard(d.copy(mode = "world", name = "", description = "", lore = "", entries = emptyList(), model = "")) }, enabled = enabled, label = { Text("New world") })
                    }
                    if (d.mode == "character") Text("Target: ${vm.worlds.value.find { it.id == d.worldId }?.name ?: "World unavailable"}", color = Violet)
                    OutlinedTextField(d.premise, { vm.updateWizard(d.copy(premise = it.take(4000))) }, label = { Text("Starting idea") }, minLines = 3, maxLines = 6, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                    OutlinedTextField(d.direction, { vm.updateWizard(d.copy(direction = it.take(2000))) }, label = { Text("Tone, motivations, constraints, unanswered questions") }, minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                    Text("Interpretation", style = MaterialTheme.typography.titleSmall)
                    listOf("Original interpretation", "Follow supplied source canon", "Alternate interpretation").forEach { option ->
                        FilterChip(d.interpretation == option, { vm.updateWizard(d.copy(interpretation = option)) }, enabled = enabled, label = { Text(option) })
                    }
                    HorizontalDivider(color = Line)
                    Text("Optional reference", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(d.sourceUrl, { vm.updateWizard(d.copy(sourceUrl = it.take(2000), sourceTitle = "", sourceRevision = "", sourceLicense = "", retrievedAt = 0, reference = if (d.retrievedAt != 0L) "" else d.reference)) }, label = { Text("Fandom / Wikia article URL") }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                    OutlinedButton(onClick = vm::fetchWizardReference, enabled = enabled && d.sourceUrl.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Load selected article") }
                    Text("Loading contacts that wiki only. Review the excerpt below; access may be unavailable. Pasted reference text works offline. No images are downloaded.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    if (d.sourceTitle.isNotBlank()) Text("${d.sourceTitle} · revision ${d.sourceRevision.ifBlank { "unknown" }}", color = Violet)
                    OutlinedTextField(d.reference, { vm.updateWizard(d.copy(reference = it.take(12000))) }, label = { Text("Reference text / editable excerpt") }, minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                    val descriptions = s.descriptions.filter { desc -> (d.mode == "world" || s.world?.id == d.worldId) && desc.approved && s.asset(desc.assetId)?.let { it.contentHash == desc.sourceHash && it.referenceKind != "exploratory" } == true }
                    if (descriptions.isNotEmpty()) {
                        Text("Or use an approved image description", style = MaterialTheme.typography.labelLarge)
                        descriptions.forEach { desc ->
                            TextButton(onClick = { vm.updateWizard(d.copy(reference = desc.text.take(12000), sourceTitle = "Image ${desc.assetId} · ${desc.sourceHash}", sourceUrl = "", sourceRevision = "", sourceLicense = "", retrievedAt = 0)) }, enabled = enabled) {
                                Text(s.asset(desc.assetId)?.label ?: desc.assetId)
                            }
                        }
                    }
                    Text("Generate sends the idea, direction, interpretation, selected reference text, and (for a character) up to 4,000 characters of the target world's public description to OpenRouter. No private notes, transcripts, or source images are sent. Uses your selected RP/text model and current pricing controls.", color = Muted, style = MaterialTheme.typography.bodySmall)
                    Button(onClick = vm::generateWizard, enabled = enabled && d.premise.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text(if (d.name.isBlank()) "Generate proposal" else "Regenerate proposal") }
                    TextButton(onClick = ai, enabled = enabled) { Text("AI settings") }
                    OutlinedButton(onClick = { review = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text(if (d.name.isBlank()) "Write manually / skip AI" else "Review existing draft") }
                } else {
                    Text("Review and edit", style = MaterialTheme.typography.titleLarge)
                    Text("Only Save creation adds these entries. Uncheck any starter entries you do not want. Nothing rewrites existing lore.", color = Muted)
                    OutlinedTextField(d.name, { vm.updateWizard(d.copy(name = it.take(200))) }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                    OutlinedTextField(d.description, { vm.updateWizard(d.copy(description = it.take(4000))) }, label = { Text("Description / appearance") }, minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                    OutlinedTextField(d.lore, { vm.updateWizard(d.copy(lore = it.take(4000))) }, label = { Text("Lore, personality, motivations, relationships") }, minLines = 3, maxLines = 8, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                    d.entries.forEachIndexed { index, entry ->
                        fun edit(next: org.voidcat.voidcanon.ai.WizardEntry) { vm.updateWizard(d.copy(entries = d.entries.toMutableList().also { it[index] = next })) }
                        HorizontalDivider(color = Line)
                        Row {
                            Checkbox(entry.selected, { edit(entry.copy(selected = it)) }, enabled = enabled)
                            Text("Include ${entry.kind}", Modifier.padding(top = 14.dp))
                        }
                        OutlinedTextField(entry.name, { edit(entry.copy(name = it.take(200))) }, label = { Text("Entry name") }, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                        OutlinedTextField(entry.description, { edit(entry.copy(description = it.take(4000))) }, label = { Text("Entry description") }, maxLines = 6, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                        OutlinedTextField(entry.lore, { edit(entry.copy(lore = it.take(4000))) }, label = { Text("Entry lore") }, maxLines = 6, modifier = Modifier.fillMaxWidth(), enabled = enabled)
                    }
                    if (d.model.isNotBlank()) Text("Proposed by ${d.model}. Reported usage: ${d.usage}", style = MaterialTheme.typography.bodySmall, color = Muted)
                    if (d.reference.isNotBlank()) Text("The reviewed reference excerpt and available source metadata will be retained with the creation. Imported material is a reference, not automatically authoritative canon. Check the original page's attribution/license before redistribution.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Button(onClick = vm::acceptWizard, enabled = enabled && d.name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Save creation") }
                    OutlinedButton(onClick = { review = false }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Back to idea and references") }
                }
                TextButton(onClick = { discard = true }, enabled = enabled) { Text("Discard draft") }
            }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Discard this working draft?") }, text = { Text("Saved world entries and artwork are unaffected.") }, confirmButton = {
        TextButton(onClick = { vm.discardWizard(); dismiss() }) { Text("Discard") }
    }, dismissButton = { TextButton(onClick = { discard = false }) { Text("Keep draft") } })
}
