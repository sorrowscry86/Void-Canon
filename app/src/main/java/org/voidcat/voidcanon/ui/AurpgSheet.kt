@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package org.voidcat.voidcanon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.voidcat.voidcanon.CanonViewModel
import org.voidcat.voidcanon.data.Aurpg

@Composable
fun AurpgSheet(vm: CanonViewModel, sceneId: String, close: () -> Unit) {
    val snapshot by vm.snapshot.collectAsStateWithLifecycle()
    val generating by vm.generating.collectAsStateWithLifecycle()
    val scene = snapshot.scenes.firstOrNull { it.id == sceneId }
    val state = remember(scene?.aurpgJson) { Aurpg.decode(scene?.aurpgJson.orEmpty()) }
    var label by remember { mutableStateOf("") }
    var count by remember { mutableStateOf("1") }
    var sides by remember { mutableStateOf("20") }
    var modifier by remember { mutableStateOf("0") }
    var target by remember { mutableStateOf("") }
    var spend by remember { mutableStateOf("0") }
    var clock by remember { mutableStateOf("") }
    var segments by remember { mutableStateOf("6") }
    var working by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val enabled = !working && generating == null
    fun number(value: String, field: String) = requireNotNull(value.toIntOrNull()) { "Enter a whole number for $field." }
    fun action(block: suspend () -> String?) {
        working = true; error = null
        scope.launch {
            try { error = block() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Could not save mechanics." }
            finally { working = false }
        }
    }
    ModalBottomSheet(onDismissRequest = { if (!working) close() }, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.94f).imePadding()) {
            SheetHeader("AURPG tools", { if (!working) close() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Rules-lite · rolls and counters are stored locally for this scene.", color = Violet)
                Text("Total = dice + modifier + spent momentum. Each spent point adds +1. Meet or exceed your target to pass; leave target blank for an unopposed roll. No automatic criticals, damage or stat changes.", color = Muted)
                Text("Momentum: ${state.momentum}/10", style = MaterialTheme.typography.titleLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { action { vm.mechanicsAction("momentumDelta", sceneId, value = -1) } }, enabled = enabled && state.momentum > 0) { Text("Momentum −1") }
                    OutlinedButton(onClick = { action { vm.mechanicsAction("momentumDelta", sceneId, value = 1) } }, enabled = enabled && state.momentum < 10) { Text("Momentum +1") }
                }
                OutlinedTextField(label, { label = it.take(100) }, label = { Text("Check label (optional)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(count, { count = it.take(3) }, label = { Text("Dice count (1–10)") })
                OutlinedTextField(sides, { sides = it.take(3) }, label = { Text("Sides (2–100)") })
                OutlinedTextField(modifier, { modifier = it.take(4) }, label = { Text("Modifier (−100 to 100)") })
                OutlinedTextField(target, { target = it.take(4) }, label = { Text("Target (optional, 1–1000)") })
                OutlinedTextField(spend, { spend = it.take(2) }, label = { Text("Momentum to spend") })
                Button(onClick = { action { vm.mechanicsAction("roll", sceneId, label, number(count, "dice count"), number(sides, "sides"), number(modifier, "modifier"),
                    if (target.isBlank()) null else number(target, "target"), number(spend, "momentum")) } }, enabled = enabled, modifier = Modifier.fillMaxWidth()) { Text("Roll and record") }
                Text("The RP model receives recent recorded checks and current counters. It cannot roll dice or update these controls. Roll before sending the action you want resolved; review the model's portrayal.", color = Muted)
                HorizontalDivider(); Text("Progress clocks", style = MaterialTheme.typography.titleLarge)
                state.clocks.forEach { current ->
                    Text("${current.name}: ${current.filled}/${current.segments}", color = Violet)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { action { vm.mechanicsAction("tick", sceneId, value = -1, clockId = current.id) } }, enabled = enabled && current.filled > 0, modifier = Modifier.semantics { contentDescription = "Decrease ${current.name}" }) { Text("−1") }
                        OutlinedButton(onClick = { action { vm.mechanicsAction("tick", sceneId, value = 1, clockId = current.id) } }, enabled = enabled && current.filled < current.segments, modifier = Modifier.semantics { contentDescription = "Increase ${current.name}" }) { Text("+1") }
                    }
                }
                OutlinedTextField(clock, { clock = it.take(80) }, label = { Text("New clock name") })
                OutlinedTextField(segments, { segments = it.take(2) }, label = { Text("Clock segments (2–12)") })
                OutlinedButton(onClick = { action { vm.mechanicsAction("addClock", sceneId, label = clock, value = number(segments, "clock segments")) } }, enabled = enabled && state.clocks.size < 16) { Text("Add progress clock") }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (generating != null) Text("Wait for generation or cancel it before changing mechanics.", color = Violet)
                HorizontalDivider(); Text("Recorded rolls (${state.checks.size}/1000)", style = MaterialTheme.typography.titleLarge)
                Text("Showing the latest 20. Backups retain the full journal. Rolls are immutable; momentum and clocks are explicitly author-controlled.", color = Muted)
                state.checks.takeLast(20).asReversed().forEach { check ->
                    Text("${check.label.ifBlank { "Check" }} · ${check.dice.size}d${check.sides} ${check.dice}\nModifier ${check.modifier} · momentum ${check.momentumSpent}\nTotal ${check.total}" +
                        (check.target?.let { " vs $it · ${if (check.passed == true) "PASS" else "FAIL"}" } ?: " · no target"))
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
