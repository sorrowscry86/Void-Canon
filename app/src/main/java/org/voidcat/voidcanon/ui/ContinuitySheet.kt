@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package org.voidcat.voidcanon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.voidcat.voidcanon.CanonViewModel
import org.voidcat.voidcanon.data.*
import java.util.UUID

/** Optional author correction surface; ordinary play keeps the ledger out of the dialogue. */
@Composable
fun ContinuitySheet(s: WorldSnapshot, vm: CanonViewModel, dismiss: () -> Unit) {
    val scene = s.scene ?: return
    val original = remember(scene.id) { Continuity.current(s) }
    var edited by remember(scene.id) { mutableStateOf(original) }
    val generating by vm.generating.collectAsStateWithLifecycle()
    var actorId by remember { mutableStateOf(Continuity.PLAYER) }
    var objectId by remember { mutableStateOf<String?>(null) }
    var showObjects by remember { mutableStateOf(false) }
    val locations = s.entries.filter { it.kind == "location" }
    fun name(id: String) = if (id == Continuity.PLAYER) scene.userRole else s.entry(id)?.name ?: id
    val error = runCatching { Continuity.validate(edited, s); Continuity.project(s, edited) }.exceptionOrNull()?.message
    val stale = Continuity.current(s).revision != original.revision
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.94f).imePadding().verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Scene continuity", style = MaterialTheme.typography.headlineMedium)
            Text("Tracks where people and objects are now. Unknown details stay blank. Corrections do not change lore or the transcript. These details can be sent to your RP model; keep secrets in author-only notes.", color = Muted)
            Text("Revision ${original.revision} · ${s.continuityEvents.size} saved updates", color = Violet)
            StateField("Time at start of tracking (optional)", edited.timeAnchor, 120) { edited = edited.copy(timeAnchor = it) }
            Text("Elapsed since tracking began: ${edited.elapsedSeconds} seconds", color = Muted)
            Row {
                TextButton(onClick = { showObjects = false }) { Text("People") }
                TextButton(onClick = { showObjects = true }) { Text("Objects") }
            }
            if (!showObjects) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    edited.actors.forEach { actor -> FilterChip(actorId == actor.actorId, { actorId = actor.actorId }, label = { Text(name(actor.actorId)) }) }
                }
                val actor = edited.actors.find { it.actorId == actorId }
                actor?.let {
                    fun change(value: ActorState) { edited = edited.copy(
                        actors = edited.actors.map { if (it.actorId == value.actorId) value else it },
                        objects = edited.objects.map { if (it.holderId == value.actorId) it.copy(locationId = value.locationId) else it }) }
                    Text("Location", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        locations.forEach { location -> FilterChip(actor.locationId == location.id, { change(actor.copy(locationId = location.id)) }, label = { Text(location.name) }) }
                    }
                    Row { Checkbox(actor.present, { change(actor.copy(present = it)) }); Text("Present in the active scene") }
                    StateField("Position", actor.position) { change(actor.copy(position = it)) }
                    StateField("Current activity", actor.activity) { change(actor.copy(activity = it)) }
                    StateField("Relevant condition", actor.condition) { change(actor.copy(condition = it)) }
                    StateField("Appearance changes", actor.appearance) { change(actor.copy(appearance = it)) }
                    StateField("Facts learned in this scene", actor.knownFacts) { change(actor.copy(knownFacts = it)) }
                }
                val available = s.entries.filter { it.kind == "character" && edited.actors.none { actor -> actor.actorId == it.id } }
                if (edited.actors.size < 8 && available.isNotEmpty()) {
                    Text("Add a participant", style = MaterialTheme.typography.titleSmall)
                    FlowRow {
                        available.forEach { entry -> TextButton(onClick = {
                            edited = edited.copy(actors = edited.actors + ActorState(entry.id, scene.locationId))
                            actorId = entry.id
                        }) { Text(entry.name) } }
                    }
                }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    edited.objects.forEach { item -> FilterChip(objectId == item.id, { objectId = item.id }, label = { Text(item.name) }) }
                }
                if (edited.objects.size < 16) TextButton(onClick = {
                    val item = ObjectState(UUID.randomUUID().toString(), "New object", scene.locationId)
                    edited = edited.copy(objects = edited.objects + item); objectId = item.id
                }) { Text("Add object") }
                edited.objects.find { it.id == objectId }?.let { item ->
                    fun change(value: ObjectState) { edited = edited.copy(objects = edited.objects.map { if (it.id == value.id) value else it }) }
                    StateField("Object name", item.name, 80) { change(item.copy(name = it)) }
                    Text("Held or carried by", style = MaterialTheme.typography.titleSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(item.holderId.isBlank(), { change(item.copy(holderId = "", hand = "")) }, label = { Text("Nobody") })
                        edited.actors.forEach { actor -> FilterChip(item.holderId == actor.actorId, {
                            change(item.copy(holderId = actor.actorId, locationId = actor.locationId))
                        }, label = { Text(name(actor.actorId)) }) }
                    }
                    if (item.holderId.isNotBlank()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("" to "Carried", "left" to "Left hand", "right" to "Right hand", "both" to "Both hands").forEach { (value, label) ->
                            FilterChip(item.hand == value, { change(item.copy(hand = value)) }, label = { Text(label) })
                        }
                    } else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        locations.forEach { location -> FilterChip(item.locationId == location.id, { change(item.copy(locationId = location.id)) }, label = { Text(location.name) }) }
                    }
                    StateField("Position / placement", item.position) { change(item.copy(position = it)) }
                    TextButton(onClick = { edited = edited.copy(objects = edited.objects.filter { it.id != item.id }); objectId = null }) { Text("Stop tracking this object") }
                }
            }
            if (stale) Text("State changed while this editor was open. Close and reopen to correct the latest version.", color = MaterialTheme.colorScheme.error)
            if (error != null) Text(error, color = MaterialTheme.colorScheme.error)
            val last = s.continuityEvents.maxByOrNull { Continuity.decode(it.afterJson).revision }
            if (last != null) TextButton(onClick = { edited = Continuity.decode(last.beforeJson).copy(revision = original.revision) }) { Text("Use state before last update") }
            Button(onClick = { vm.correctContinuity(scene.id, original.revision, edited); dismiss() }, enabled = generating == null && !stale && error == null) { Text("Save state correction") }
            TextButton(onClick = dismiss) { Text("Close") }
            Text("Automatic tracking can miss or misinterpret details. Source passages and prior snapshots remain available in the saved history.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
}

@Composable
private fun StateField(label: String, value: String, limit: Int = 240, change: (String) -> Unit) {
    OutlinedTextField(value, { change(it.take(limit)) }, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}
