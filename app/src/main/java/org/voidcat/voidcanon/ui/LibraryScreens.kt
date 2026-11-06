@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package org.voidcat.voidcanon.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.voidcat.voidcanon.CanonViewModel
import org.voidcat.voidcanon.data.*

@Composable
fun WorldLibrarySheet(vm: CanonViewModel, currentId: String?, dismiss: () -> Unit) {
    val worlds by vm.worlds.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    ModalBottomSheet(onDismissRequest = dismiss, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f)) {
            SheetHeader("Your worlds", dismiss)
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                item {
                    Text("Independent settings, shared creative freedom.", color = Muted)
                    Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) { Text("New world") }
                }
                items(worlds, key = { it.id }) { world ->
                    Surface(onClick = { vm.selectWorld(world.id); dismiss() }, color = Panel, shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, if (world.id == currentId) Violet else Line)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(world.name, style = MaterialTheme.typography.titleLarge)
                            Text(if (world.id == currentId) "Current world" else "Open world", color = Violet, style = MaterialTheme.typography.labelMedium)
                            if (world.description.isNotBlank()) Text(world.description, maxLines = 3, color = Muted)
                        }
                    }
                }
            }
        }
    }
    if (creating) WorldDetailsDialog(null, { creating = false }) { name, description ->
        vm.createWorld(name, description); creating = false; dismiss()
    }
}

@Composable
fun WorldDetailsDialog(world: WorldRecord?, dismiss: () -> Unit, save: (String, String) -> Unit) {
    var name by rememberSaveable(world?.id) { mutableStateOf(world?.name.orEmpty()) }
    var description by rememberSaveable(world?.id) { mutableStateOf(world?.description.orEmpty()) }
    AlertDialog(onDismissRequest = dismiss, title = { Text(if (world == null) "Create world" else "Edit world") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(name, { name = it.take(200) }, label = { Text("World name") }, singleLine = true)
            OutlinedTextField(description, { description = it.take(50000) }, label = { Text("World description") }, minLines = 4, maxLines = 8)
            Text("Start with an idea. Characters, places, and artwork can come later.", color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }, confirmButton = { TextButton(onClick = { save(name, description) }, enabled = name.isNotBlank()) { Text(if (world == null) "Create world" else "Save world") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
fun ScenePicker(s: WorldSnapshot, preferred: EntryRecord?, vm: CanonViewModel, dismiss: () -> Unit, opened: () -> Unit) {
    val characters = s.entries.filter { it.kind == "character" }
    val locations = s.entries.filter { it.kind == "location" }
    var characterId by rememberSaveable { mutableStateOf(preferred?.takeIf { it.kind == "character" }?.id ?: s.scene?.characterId ?: characters.firstOrNull()?.id) }
    var locationId by rememberSaveable { mutableStateOf(preferred?.takeIf { it.kind == "location" }?.id ?: s.scene?.locationId ?: locations.firstOrNull()?.id) }
    AlertDialog(onDismissRequest = dismiss, title = { Text("Choose scene") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Choose people and places from ${s.world?.name}. Each pairing keeps its own history and chapter memory.", color = Muted)
            EntryChoice("Character", characters, characterId) { characterId = it }
            EntryChoice("Location", locations, locationId) { locationId = it }
            if (characters.isEmpty() || locations.isEmpty()) Text("Add at least one character and one location in World first.", color = Violet)
            Text("An existing pairing resumes its scene. A new pairing starts with an empty transcript; your other scenes stay saved.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }, confirmButton = { TextButton(onClick = {
        val c = characterId; val l = locationId
        if (c != null && l != null) { vm.useInScene(c, l, opened); dismiss() }
    }, enabled = characterId != null && locationId != null) { Text("Open scene") } }, dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun EntryChoice(label: String, entries: List<EntryRecord>, selected: String?, choose: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text(label, style = MaterialTheme.typography.labelLarge)
        OutlinedButton(onClick = { expanded = true }, enabled = entries.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
            Text(entries.find { it.id == selected }?.name ?: "No ${label.lowercase()} yet")
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 260.dp)) {
            entries.forEach { entry -> DropdownMenuItem(text = { Text(entry.name) }, onClick = { choose(entry.id); expanded = false }) }
        }
    }
}
