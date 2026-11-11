@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.voidcat.voidcanon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.voidcat.voidcanon.CanonViewModel
import org.voidcat.voidcanon.data.*

@Composable
fun PlayTool(label: String, icon: ImageVector, enabled: Boolean = true, active: Boolean = false, action: () -> Unit) {
    TooltipBox(positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = { PlainTooltip { Text(label) } }, state = rememberTooltipState()) {
        IconButton(onClick = action, enabled = enabled, modifier = Modifier.size(48.dp)) {
            Icon(icon, label, tint = if (active) Violet else LocalContentColor.current)
        }
    }
}

@Composable
fun PlaySettingsSheet(scene: SceneRecord, vm: CanonViewModel, live: Boolean, setLive: (Boolean) -> Unit, close: () -> Unit) {
    // The modal itself is transient. Reopening must seed from committed data,
    // never resurrect a canceled draft from the Activity saved-state registry.
    var mode by remember(scene.id) { mutableStateOf(scene.narrative.mode) }
    var preset by remember(scene.id) { mutableStateOf(scene.narrative.preset) }
    var pacing by remember(scene.id) { mutableStateOf(scene.narrative.pacing) }
    var length by remember(scene.id) { mutableStateOf(scene.narrative.length) }
    var sourceLive by remember(scene.id) { mutableStateOf(live) }
    var error by remember(scene.id) { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val generating by vm.generating.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val editable = !saving && generating == null && !busy
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = { if (!saving) close() }, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.94f)) {
            SheetHeader("Play mode", { if (!saving) close() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Saved separately for this scene. Changes affect future replies; existing dialogue and lore stay intact.", color = Muted)
                if (scene.id == "harbor-scene") {
                    Text("Reply source", style = MaterialTheme.typography.titleMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(!sourceLive, { sourceLive = false }, enabled = editable, label = { Text("Scripted demo") })
                        FilterChip(sourceLive, { sourceLive = true }, enabled = editable, label = { Text("Live RP") })
                    }
                    if (!sourceLive) Text("Offline scripted replies do not use narrative preferences.", color = Muted)
                }
                NarrativeChoices("Narrative mode", NarrativePlay.modes, mode, editable) {
                    mode = it; error = null
                    if (it == "freeform") sourceLive = true
                }
                if (mode == "freeform") {
                    Text("MLRPE-inspired · first increment", color = Violet)
                    NarrativeChoices("Narrative preset", NarrativePlay.presets, preset, editable) { preset = it; error = null }
                    NarrativeChoices("Pacing", NarrativePlay.pacings, pacing, editable) { pacing = it; error = null }
                    NarrativeChoices("Response length", NarrativePlay.lengths, length, editable) { length = it; error = null }
                    Text("You control your role. These are requests to the selected RP model; exact length and portrayal can vary. No dice or background character simulation.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                HorizontalDivider()
                Text("AURPG · planned", color = Muted)
                Text("Dice, momentum and clocks come in a later increment.", color = Muted, style = MaterialTheme.typography.bodySmall)
                if (generating != null) Text("Wait for generation or cancel it before saving a mode change.", color = Violet)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    saving = true; error = null
                    scope.launch {
                        error = vm.saveNarrativeSettings(scene.id, NarrativeSettings(mode, preset, pacing, length))
                        saving = false
                        if (error == null) { setLive(sourceLive); close() }
                    }
                }, enabled = editable, modifier = Modifier.fillMaxWidth()) { Text(if (saving) "Saving…" else "Save play settings") }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun NarrativeChoices(title: String, options: List<NarrativeOption>, selected: String, enabled: Boolean, select: (String) -> Unit) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option -> FilterChip(selected == option.id, { select(option.id) }, enabled = enabled, label = { Text(option.label) }) }
    }
    Text(options.first { it.id == selected }.description, color = Muted, style = MaterialTheme.typography.bodySmall)
}
