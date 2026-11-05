@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package org.voidcat.voidcanon.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import org.voidcat.voidcanon.CanonViewModel
import org.voidcat.voidcanon.data.AssetRecord
import org.voidcat.voidcanon.data.EntryRecord
import org.voidcat.voidcanon.data.WorldSnapshot
import java.text.DateFormat
import java.util.Date

private enum class Destination(val label: String, val icon: ImageVector) {
    World("World", CanonIcons.World), Gallery("Gallery", CanonIcons.Images),
    Play("Play", CanonIcons.Play), Chapters("Chapters", CanonIcons.Book),
}

@Composable
fun CanonApp(vm: CanonViewModel) {
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val ready by vm.ready.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val notice by vm.notice.collectAsStateWithLifecycle()
    val fatal by vm.fatalError.collectAsStateWithLifecycle()
    val revision by vm.restoreRevision.collectAsStateWithLifecycle()
    var destinationName by rememberSaveable { mutableStateOf(Destination.World.name) }
    val destination = Destination.valueOf(destinationName)
    var detailId by rememberSaveable { mutableStateOf<String?>(null) }
    var aiOpen by remember { mutableStateOf(false) }
    var contextOpen by remember { mutableStateOf(false) }
    var sceneOpen by remember { mutableStateOf(false) }
    val generating by vm.generating.collectAsStateWithLifecycle()
    var backupOpen by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    val snackbar = remember { SnackbarHostState() }
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val exportPicker = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        if (uri != null) vm.export(uri)
    }
    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }

    LaunchedEffect(notice) {
        notice?.let { snackbar.showSnackbar(it); if (vm.notice.value == it) vm.notice.value = null }
    }
    LaunchedEffect(revision) { detailId = null }
    LaunchedEffect(snapshot.world?.id) { detailId = null; destinationName = Destination.World.name }
    BackHandler(detailId != null || destination != Destination.World) {
        if (detailId != null) detailId = null else destinationName = Destination.World.name
    }

    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        containerColor = Ink,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            if (generating != null) Surface(color = Panel) {
                Column(Modifier.statusBarsPadding()) {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Generating ${generating}…", modifier = Modifier.weight(1f))
                        TextButton(onClick = vm::cancelGeneration) { Text("Cancel") }
                    }
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        bottomBar = {
            if (!keyboardVisible) NavigationBar(containerColor = Ink, tonalElevation = 0.dp) {
                Destination.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = destination == tab,
                        onClick = { detailId = null; destinationName = tab.name },
                        enabled = !busy && ready,
                        icon = { Icon(tab.icon, null) }, label = { Text(tab.label) },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = Panel, selectedIconColor = Violet, selectedTextColor = Violet),
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val detail = snapshot.entry(detailId)
            key(snapshot.world?.id, snapshot.scene?.id) {
            when {
                fatal != null -> EmptyView("The world could not open", fatal.orEmpty(), CanonIcons.Alert)
                !ready -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                detail != null -> EntryScreen(snapshot, detail, vm, { detailId = null }, {
                    detailId = null; destinationName = Destination.Play.name
                }, { detailId = null; destinationName = Destination.Gallery.name })
                destination == Destination.World -> WorldScreen(snapshot, vm, { destinationName = Destination.Play.name }, { detailId = it }, { backupOpen = true }, { aiOpen = true })
                destination == Destination.Gallery -> GalleryScreen(snapshot, vm) { aiOpen = true }
                destination == Destination.Play && snapshot.scene == null -> EmptyView("Your scene starts here", "Add a character and a location in World, then choose a scene. Your world can grow without AI.", CanonIcons.Book)
                destination == Destination.Chapters && snapshot.scene == null -> EmptyView("No chapters yet", "Start a scene from World to preserve its transcript and chapter memory.", CanonIcons.Book)
                destination == Destination.Play -> PlayScreen(snapshot, vm, keyboardVisible, { aiOpen = true }, { contextOpen = true }, { sceneOpen = true })
                else -> ChaptersScreen(snapshot, vm)
            }
            }
            if (busy) Surface(onClick = {}, color = Ink.copy(alpha = .65f), modifier = Modifier.fillMaxSize()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(color = Panel, shape = RoundedCornerShape(16.dp)) {
                        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Working with local files…", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }

    if (aiOpen) AiSettingsSheet(vm) { aiOpen = false }
    if (contextOpen) ContextSheet(snapshot, vm) { contextOpen = false }
    if (sceneOpen) SceneDetailsDialog(snapshot, vm) { sceneOpen = false }
    if (backupOpen) ModalBottomSheet(onDismissRequest = { backupOpen = false }, containerColor = Panel) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Your library, with you", style = MaterialTheme.typography.headlineMedium)
            Text("A portable backup includes every world, private author notes, artwork, visual descriptions, drafts, transcripts, chapters, and request diagnostics. Your OpenRouter key and AI settings are excluded.", color = Muted)
            FilledTonalButton(onClick = { backupOpen = false; exportPicker.launch("Void-Canon-backup.zip") }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Icon(CanonIcons.Upload, null); Spacer(Modifier.width(8.dp)); Text("Export backup")
            }
            OutlinedButton(onClick = { backupOpen = false; restorePicker.launch(arrayOf("application/zip", "application/octet-stream", "application/x-zip-compressed")) }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                Icon(CanonIcons.Download, null); Spacer(Modifier.width(8.dp)); Text("Restore backup")
            }
            Text("Local work is available offline. AI actions send the selected story context or image you authorize; backup exports are a separate action.", style = MaterialTheme.typography.bodySmall, color = Muted)
        }
    }
    restoreUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { restoreUri = null },
            title = { Text("Replace your local library?") },
            text = { Text("Restore replaces ALL local worlds, images, visual descriptions, scenes, and chapters. Export a backup first to keep them. The archive is validated before anything changes.") },
            confirmButton = { TextButton(onClick = { restoreUri = null; vm.restore(uri) }) { Text("Replace and restore") } },
            dismissButton = { TextButton(onClick = { restoreUri = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, color = Violet, fontSize = 10.sp, letterSpacing = 2.sp, maxLines = 1)
}

@Composable
private fun LocalImage(vm: CanonViewModel, id: String?, description: String?, modifier: Modifier, scale: ContentScale = ContentScale.Crop, alignment: Alignment = Alignment.Center) {
    AsyncImage(model = vm.imageFile(id), contentDescription = description, modifier = modifier, contentScale = scale, alignment = alignment)
}

@Composable
private fun WorldScreen(s: WorldSnapshot, vm: CanonViewModel, play: () -> Unit, detail: (String) -> Unit, backup: () -> Unit, ai: () -> Unit) {
    val world = s.world ?: return
    var libraryOpen by remember { mutableStateOf(false) }
    var worldEditing by remember { mutableStateOf(false) }
    var creatingKind by remember { mutableStateOf<String?>(null) }
    var scenePicker by remember { mutableStateOf(false) }
    var search by rememberSaveable { mutableStateOf("") }
    val scenes by vm.sceneLibrary.collectAsStateWithLifecycle()
    if (libraryOpen) WorldLibrarySheet(vm, world.id) { libraryOpen = false }
    if (worldEditing) WorldDetailsDialog(world, { worldEditing = false }) { name, description -> vm.editWorld(world.id, name, description); worldEditing = false }
    creatingKind?.let { kind ->
        EditEntryDialog(EntryRecord("new-$kind", world.id, kind, "", ""), { creatingKind = null }, { name, description, lore, notes ->
            vm.createEntry(world.id, kind, name, description, lore, notes); creatingKind = null
        }, creating = true)
    }
    if (scenePicker) ScenePicker(s, null, vm, { scenePicker = false }, play)
    val entries = s.entries.filter { search.isBlank() || it.name.contains(search, true) || it.description.contains(search, true) }.sortedWith(compareBy({ it.kind }, { it.name.lowercase() }))
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(344.dp)) {
                LocalImage(vm, world.coverAssetId, "${world.name} cover", Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(alpha = .25f), Color.Transparent, Ink), startY = 0f)))
                Row(Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("VOID CANON", fontSize = 13.sp, letterSpacing = 3.sp)
                        Text("Your worlds. Their stories.", fontSize = 11.sp, color = Warm.copy(alpha = .75f))
                    }
                    TextButton(onClick = { libraryOpen = true }) { Text("Worlds", color = Warm) }
                    TextButton(onClick = ai) { Text("AI", color = Warm) }
                    IconButton(onClick = backup) { Icon(CanonIcons.Box, "Backup and restore") }
                }
                Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 24.dp).padding(bottom = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow(if (world.id == "glass-harbor") "Sample world" else "Your world")
                    Text(world.name, style = MaterialTheme.typography.headlineLarge, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(world.description, color = Muted, style = MaterialTheme.typography.bodyMedium, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        item {
            Button(onClick = { if (s.scene == null) scenePicker = true else play() }, modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp).heightIn(min = 54.dp),
                shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5C497D), contentColor = Warm)) {
                Icon(CanonIcons.Play, null); Spacer(Modifier.width(12.dp))
                Text(if (s.scene == null) "Choose your first scene" else "Continue scene", Modifier.weight(1f))
                Icon(CanonIcons.Right, null)
            }
        }
        item {
            Column(Modifier.padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { worldEditing = true }, modifier = Modifier.weight(1f)) { Text("Edit world") }
                    OutlinedButton(onClick = { scenePicker = true }, modifier = Modifier.weight(1f)) { Text("Choose scene") }
                }
                Text("People & places", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { creatingKind = "character" }, modifier = Modifier.weight(1f)) { Text("Add character") }
                    OutlinedButton(onClick = { creatingKind = "location" }, modifier = Modifier.weight(1f)) { Text("Add location") }
                }
                OutlinedTextField(search, { search = it }, label = { Text("Search people and places") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (s.entries.isEmpty()) Text("An unwritten world. Add its first character or place; no artwork or AI is required.", color = Muted)
                else if (entries.isEmpty()) Text("No matching people or places.", color = Muted)
            }
        }
        items(entries.chunked(2), key = { row -> row.joinToString { it.id } }) { row ->
            Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { entry -> EntityCard(entry, s, vm, Modifier.weight(1f)) { detail(entry.id) } }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (scenes.isNotEmpty()) item {
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Saved scenes", style = MaterialTheme.typography.titleLarge)
                scenes.forEach { scene ->
                    OutlinedButton(onClick = { vm.selectScene(scene.id, play) }, modifier = Modifier.fillMaxWidth()) {
                        Text("${s.entry(scene.characterId)?.name} · ${s.entry(scene.locationId)?.name}" + if (scene.id == s.scene?.id) " ✓" else "")
                    }
                }
            }
        }
        item {
            Surface(onClick = backup, color = Panel.copy(alpha = .6f), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Icon(CanonIcons.Offline, null, tint = Violet)
                    Column(Modifier.weight(1f)) {
                        Text("At home on your device", style = MaterialTheme.typography.labelLarge)
                        Text("Offline by design · Back up your world", style = MaterialTheme.typography.bodySmall, color = Muted)
                    }
                    Icon(CanonIcons.Right, null, tint = Muted)
                }
            }
        }
    }
}

@Composable
private fun EntityCard(entry: EntryRecord, s: WorldSnapshot, vm: CanonViewModel, modifier: Modifier, click: () -> Unit) {
    Surface(onClick = click, modifier = modifier, shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Line), color = Panel) {
        Column {
            Box(Modifier.fillMaxWidth().height(164.dp)) {
                if (entry.kind == "character") LocalImage(vm, s.scene?.backgroundAssetId, null, Modifier.fillMaxSize())
                LocalImage(vm, entry.primaryAssetId, entry.name, Modifier.fillMaxSize(), alignment = if (entry.kind == "character") Alignment.TopCenter else Alignment.Center)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Panel.copy(alpha = .8f)))))
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(entry.name, fontFamily = FontFamily.Serif, fontSize = 19.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(if (entry.kind == "character") "Character" else "Location", fontSize = 12.sp, color = Muted)
            }
        }
    }
}

@Composable
private fun EntryScreen(s: WorldSnapshot, entry: EntryRecord, vm: CanonViewModel, back: () -> Unit, play: () -> Unit, gallery: () -> Unit) {
    var section by rememberSaveable(entry.id) { mutableIntStateOf(0) }
    var editing by remember { mutableStateOf(false) }
    var scenePicker by remember { mutableStateOf(false) }
    if (scenePicker) ScenePicker(s, entry, vm, { scenePicker = false }, play)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Box(Modifier.fillMaxWidth().height(378.dp)) {
                if (entry.kind == "character") LocalImage(vm, s.scene?.backgroundAssetId, null, Modifier.fillMaxSize())
                LocalImage(vm, entry.primaryAssetId, entry.name, Modifier.fillMaxSize(), alignment = if (entry.kind == "character") Alignment.TopCenter else Alignment.Center)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(alpha = .6f), Color.Transparent, Ink))))
                Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = back) { Icon(CanonIcons.Back, "Back to world") }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = { editing = true }) { Icon(CanonIcons.Edit, "Edit ${entry.kind}") }
                }
                Column(Modifier.align(Alignment.BottomStart).padding(24.dp)) {
                    Eyebrow(entry.kind)
                    Text(entry.name, style = MaterialTheme.typography.headlineLarge)
                }
            }
        }
        item {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().padding(horizontal = 24.dp)) {
                listOf("Overview", "Images", "Lore").forEachIndexed { index, label ->
                    SegmentedButton(selected = section == index, onClick = { section = index }, shape = SegmentedButtonDefaults.itemShape(index, 3), icon = {}) { Text(label) }
                }
            }
        }
        item {
            Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                when (section) {
                    0 -> {
                        Text(if (entry.kind == "character") "Appearance & identity" else "About this place", style = MaterialTheme.typography.titleLarge)
                        Text(entry.description.ifBlank { "Add your own description." }, color = Muted)
                        TextButton(onClick = { editing = true }) { Icon(CanonIcons.Edit, null); Spacer(Modifier.width(8.dp)); Text("Edit details") }
                    }
                    1 -> {
                        Text("Visual references", style = MaterialTheme.typography.titleLarge)
                        Text("${s.assets.count { it.entityId == entry.id }} linked images. Choose a portrait, background, or transparent cutout in Gallery.", color = Muted)
                        OutlinedButton(onClick = gallery, modifier = Modifier.fillMaxWidth()) { Text("Open gallery") }
                    }
                    else -> {
                        Text("Lore", style = MaterialTheme.typography.titleLarge)
                        Text(entry.lore.ifBlank { "An unwritten story. Add your lore here." }, color = Muted)
                        TextButton(onClick = { editing = true }) { Text("Edit lore") }
                    }
                }
                Button(onClick = { scenePicker = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(14.dp)) {
                    Icon(if (entry.kind == "character") CanonIcons.Person else CanonIcons.Place, null)
                    Spacer(Modifier.width(10.dp)); Text("Use in scene")
                }
            }
        }
    }
    if (editing) EditEntryDialog(entry, dismiss = { editing = false }, save = { name, description, lore, notes ->
        vm.editEntry(entry.id, name, description, lore, notes); editing = false
    })
}

@Composable
private fun EditEntryDialog(entry: EntryRecord, dismiss: () -> Unit, save: (String, String, String, String) -> Unit, creating: Boolean = false) {
    var name by rememberSaveable(entry.id) { mutableStateOf(entry.name) }
    var description by rememberSaveable(entry.id) { mutableStateOf(entry.description) }
    var lore by rememberSaveable(entry.id) { mutableStateOf(entry.lore) }
    var notes by rememberSaveable(entry.id) { mutableStateOf(entry.authorNotes) }
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (creating) "Create ${entry.kind}" else "Edit ${entry.kind}") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(name, { if (it.length <= 200) name = it }, label = { Text("Name") }, singleLine = true)
                OutlinedTextField(description, { if (it.length <= 50000) description = it }, label = { Text("Description") }, minLines = 3, maxLines = 6)
                OutlinedTextField(lore, { if (it.length <= 50000) lore = it }, label = { Text("Lore (sent to RP)") }, minLines = 3, maxLines = 6)
                OutlinedTextField(notes, { if (it.length <= 50000) notes = it }, label = { Text("Author-only notes / secrets") }, minLines = 2, maxLines = 6)
                Text("Description and lore can enter model context. Author-only notes stay private.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = { TextButton(onClick = { save(name, description, lore, notes) }, enabled = name.isNotBlank()) { Text(if (creating) "Create ${entry.kind}" else "Save changes") } },
        dismissButton = { TextButton(onClick = dismiss) { Text("Cancel") } },
    )
}

@Composable
private fun GalleryScreen(s: WorldSnapshot, vm: CanonViewModel, ai: () -> Unit) {
    if (s.entries.isEmpty()) { EmptyView("Your visual library starts here", "Add a character or location in World, then import its images here. No scene or AI is required.", CanonIcons.Images); return }
    var entityId by rememberSaveable { mutableStateOf(s.scene?.characterId ?: s.entries.first().id) }
    val entity = s.entry(entityId) ?: s.entries.first()
    var role by rememberSaveable { mutableStateOf(if (entity.kind == "character") "portrait" else "background") }
    var imageGenerationOpen by rememberSaveable { mutableStateOf(false) }
    if (imageGenerationOpen) ImageGenerationSheet(s, entity, vm) { imageGenerationOpen = false }
    val busy by vm.busy.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.importImage(uri, entity.id, role)
    }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Eyebrow("Your visual library", Modifier.weight(1f)); TextButton(onClick = ai) { Text("AI settings") } }
            Text("Gallery", style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 6.dp))
            Text("Give your world a face and a place.", color = Muted)
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                s.entries.forEach { entry ->
                    FilterChip(selected = entity.id == entry.id, enabled = !busy,
                        onClick = { entityId = entry.id; role = if (entry.kind == "character") "portrait" else "background" },
                        label = { Text(entry.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(if (entry.kind == "character") CanonIcons.Person else CanonIcons.Place, null, Modifier.size(18.dp)) },
                        modifier = Modifier.widthIn(max = 220.dp))
                }
            }
        }
        item {
            Surface(shape = RoundedCornerShape(16.dp), color = Panel, border = BorderStroke(1.dp, Line)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Import for ${entity.name}", style = MaterialTheme.typography.labelLarge)
                    val roles = if (entity.kind == "character") listOf("portrait", "cutout", "reference") else listOf("background", "reference")
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        roles.forEach { choice ->
                            FilterChip(selected = role == choice, onClick = { role = choice }, enabled = !busy,
                                label = { Text(choice.replaceFirstChar { it.uppercase() }, fontSize = 12.sp) })
                        }
                    }
                    if (entity.kind == "location") Text("Location backgrounds also become this world's cover.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    Text(if (role == "cutout") "Use a PNG with a transparent background. The app keeps the transparency; it does not remove backgrounds." else "Images are copied onto this device. Originals stay where they are.", style = MaterialTheme.typography.bodySmall, color = Muted)
                    OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                        Icon(CanonIcons.ImageAdd, null); Spacer(Modifier.width(8.dp)); Text("Import image")
                    }
                    Button(onClick = { imageGenerationOpen = true }, modifier = Modifier.fillMaxWidth(), enabled = !busy) { Text("Generate artwork") }
                }
            }
        }
        items(s.assets.filter { it.entityId == entity.id }, key = { it.id }) { asset ->
            AssetCard(s, asset, entity, vm, busy)
        }
        item { Text("Approve visual descriptions before they enter RP. Manual descriptions work offline; OpenRouter proposals require your review.", color = Muted, style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun AssetCard(s: WorldSnapshot, asset: AssetRecord, entity: EntryRecord, vm: CanonViewModel, busy: Boolean) {
    var describe by remember { mutableStateOf(false) }
    val description = s.descriptions.find { it.assetId == asset.id }
    val activeCharacter = s.scene?.characterId == entity.id
    val activeLocation = s.scene?.locationId == entity.id
    val selectedCutout = if (activeCharacter) s.scene?.cutoutAssetId == asset.id else asset.role == "cutout"
    val selectedBackground = if (activeLocation) s.scene?.backgroundAssetId == asset.id else entity.primaryAssetId == asset.id
    if (describe) DescriptionDialog(s, asset, vm) { describe = false }
    Surface(color = Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Line)) {
        Column {
            Box(Modifier.fillMaxWidth().height(238.dp).background(Color(0xFF302E38))) {
                LocalImage(vm, asset.id, asset.label, Modifier.fillMaxSize(), if (asset.transparent) ContentScale.Fit else ContentScale.Crop)
                if (asset.transparent) Surface(color = Ink.copy(alpha = .8f), shape = RoundedCornerShape(8.dp), modifier = Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                    Text("Transparent", fontSize = 11.sp, modifier = Modifier.padding(8.dp))
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(asset.label, style = MaterialTheme.typography.titleMedium)
                Text(when { description == null -> "No visual description"; description.sourceHash != asset.contentHash -> "Stale source · review needed"; description.approved -> "Approved visual description ✓"; else -> "Draft visual description" }, color = Violet, style = MaterialTheme.typography.bodySmall)
                if (description != null) Text(description.text, maxLines = 3, color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = { describe = true }, modifier = Modifier.fillMaxWidth()) { Text("Describe / review image") }
                Text(asset.provenance, color = Muted, style = MaterialTheme.typography.bodySmall)
                if (entity.kind == "character") {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.selectAsset(asset.id, "portrait") }, enabled = !busy && entity.primaryAssetId != asset.id, modifier = Modifier.weight(1f)) {
                            Text(if (entity.primaryAssetId == asset.id) "Portrait ✓" else "Set portrait", fontSize = 12.sp)
                        }
                        OutlinedButton(onClick = { vm.selectAsset(asset.id, "cutout") }, enabled = !busy && asset.transparent && !selectedCutout, modifier = Modifier.weight(1f)) {
                            Text(if (selectedCutout) "Cutout ✓" else if (activeCharacter) "Use cutout" else "Set default cutout", fontSize = 12.sp)
                        }
                    }
                } else OutlinedButton(onClick = { vm.selectAsset(asset.id, "background") }, enabled = !busy && !selectedBackground, modifier = Modifier.fillMaxWidth()) {
                    Text(if (selectedBackground) "Location image ✓" else if (activeLocation) "Use as scene background" else "Set location image")
                }
            }
        }
    }
}

@Composable
private fun PlayScreen(s: WorldSnapshot, vm: CanonViewModel, keyboardVisible: Boolean, ai: () -> Unit, context: () -> Unit, details: () -> Unit) {
    val scene = s.scene ?: return
    val character = s.entry(scene.characterId) ?: return
    val location = s.entry(scene.locationId) ?: return
    val draft by vm.draft.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val generating by vm.generating.collectAsStateWithLifecycle()
    val generationError by vm.generationError.collectAsStateWithLifecycle()
    val hasKey by vm.hasKey.collectAsStateWithLifecycle()
    val isDemo = scene.id == "harbor-scene"
    var liveMode by rememberSaveable { mutableStateOf(!isDemo || vm.hasKey.value) }
    var historyOpen by remember { mutableStateOf(false) }
    val focus = LocalFocusManager.current
    val lastMessage = s.messages.lastOrNull()
    val dialogueScroll = rememberScrollState()
    LaunchedEffect(lastMessage?.id) { dialogueScroll.scrollTo(0) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        LocalImage(vm, scene.backgroundAssetId, "Scene at ${location.name}", Modifier.fillMaxSize())
        val spriteHeight = (maxHeight - if (keyboardVisible) 196.dp else 230.dp).coerceAtLeast(64.dp)
        val dialogueMaxHeight = (maxHeight * .34f).coerceAtLeast(72.dp)
        LocalImage(vm, scene.cutoutAssetId, "${character.name}, layered over the setting",
            Modifier.align(Alignment.TopCenter).padding(top = if (keyboardVisible) 44.dp else 100.dp).fillMaxWidth().height(spriteHeight), ContentScale.Fit)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Ink.copy(alpha = .55f), Color.Transparent, Ink.copy(alpha = .65f)))))
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp).padding(bottom = 10.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 52.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(location.name, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                    Text(if (liveMode) "OpenRouter · ${if (hasKey) "live RP" else "setup needed"}" else "Scripted demo · offline", fontSize = 10.sp, color = Violet)
                }
                IconButton(onClick = { focus.clearFocus(); historyOpen = true }) { Icon(CanonIcons.History, "Scene history") }
                IconButton(onClick = vm::save, enabled = !busy) { Icon(CanonIcons.Save, "Save scene") }
            }
            if (!keyboardVisible) FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (isDemo) FilterChip(!liveMode, { if (generating == null) liveMode = false }, label = { Text("Demo") })
                FilterChip(liveMode, { if (generating == null) liveMode = true }, label = { Text("Live RP") })
                TextButton(onClick = context) { Text("Context") }
                TextButton(onClick = details) { Text("Scene") }
                TextButton(onClick = ai) { Text("AI") }
            }
            if (liveMode && generationError != null) Text(generationError.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().background(Panel).padding(8.dp).heightIn(max = 84.dp).verticalScroll(rememberScrollState()))
            Spacer(Modifier.weight(1f))
            Surface(color = Panel.copy(alpha = .96f), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, Color(0xFF60566D)),
                modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(if (lastMessage?.speaker == "user") "You" else character.name, color = Violet, fontFamily = FontFamily.Serif, fontSize = 21.sp)
                    Text(lastMessage?.text ?: "Your scene is waiting.", fontFamily = FontFamily.Serif, fontSize = 18.sp, lineHeight = 25.sp,
                        modifier = Modifier.heightIn(max = dialogueMaxHeight).verticalScroll(dialogueScroll))
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft, onValueChange = vm::updateDraft, enabled = !busy,
                    modifier = Modifier.weight(1f), shape = RoundedCornerShape(26.dp),
                    placeholder = { Text("What do you say or do?", fontSize = 14.sp) },
                    minLines = 1, maxLines = if (keyboardVisible) 2 else 3,
                    colors = OutlinedTextFieldDefaults.colors(unfocusedContainerColor = Panel.copy(alpha = .95f), focusedContainerColor = Panel),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { if (generating == null) { if (liveMode) vm.send() else vm.sendDemo() } }),
                )
                FilledIconButton(onClick = { if (liveMode) vm.send() else vm.sendDemo() }, enabled = draft.isNotBlank() && !busy && generating == null, modifier = Modifier.size(52.dp), shape = CircleShape) {
                    Icon(CanonIcons.Send, if (liveMode) "Send roleplay message" else "Send demo message")
                }
            }
            if (!keyboardVisible) {
                Text(if (liveMode) "Replies are provisional. Review chapters before using them as memory." else "Demo replies follow a fixed script. Your words and history are saved.", fontSize = 10.sp, color = Muted, modifier = Modifier.padding(top = 8.dp, start = 6.dp))
            }
        }
    }
    if (historyOpen) ModalBottomSheet(onDismissRequest = { historyOpen = false }, containerColor = Panel) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.85f)) {
            Text("Scene history", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 24.dp))
            Text("Preserved scene transcript · demo and live messages", color = Muted, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            LazyColumn(contentPadding = PaddingValues(24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                items(s.messages, key = { it.id }) { message ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(if (message.speaker == "user") "You" else character.name, color = Violet, style = MaterialTheme.typography.labelLarge)
                        Text(message.text)
                        HorizontalDivider(Modifier.padding(top = 12.dp), color = Line)
                    }
                }
                if (scene.savedAt > 0) item {
                    Text("Last saved ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(scene.savedAt))}", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun EmptyView(title: String, description: String, icon: ImageVector) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Icon(icon, null, Modifier.size(48.dp), tint = Violet)
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(description, color = Muted)
        }
    }
}
