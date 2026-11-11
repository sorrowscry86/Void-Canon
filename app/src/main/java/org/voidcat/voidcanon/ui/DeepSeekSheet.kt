@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package org.voidcat.voidcanon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.voidcat.voidcanon.CanonViewModel

@Composable
fun DeepSeekSheet(vm: CanonViewModel, close: () -> Unit) {
    val stored = remember { vm.deepSeek.settings.configuration() }
    var model by remember { mutableStateOf(stored.rpModel) }
    var key by remember { mutableStateOf("") }
    var consent by remember { mutableStateOf(stored.consent) }
    var active by remember { mutableStateOf(vm.deepSeekActive.value) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(emptyList<String>()) }
    val scope = rememberCoroutineScope()
    ModalBottomSheet(onDismissRequest = { if (!saving) close() }, containerColor = Panel,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.94f).imePadding()) {
            SheetHeader("DeepSeek direct backup", { if (!saving) close() })
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Uses your separate DeepSeek API account for roleplay and summaries. Image descriptions continue through OpenRouter; artwork keeps its existing connection.", color = Muted)
                OutlinedTextField(model, { model = it.take(200) }, label = { Text("DeepSeek text model ID") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Enter the exact model ID offered by your DeepSeek account. Story context is bounded to 24,000 characters with up to 4,096 output tokens, subject to provider limits.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(key, { key = it }, label = { Text(if (vm.deepSeek.settings.hasKey()) "Replace DeepSeek key (optional)" else "DeepSeek API key") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (vm.deepSeek.settings.hasKey()) TextButton(onClick = {
                    saving = true
                    scope.launch {
                        error = vm.saveDeepSeek(model, "", false, false)
                        if (error == null) {
                            try {
                                withContext(Dispatchers.IO) { vm.deepSeek.settings.clearKey() }
                                key = ""; active = false; consent = false
                            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                              catch (e: Exception) { error = e.message ?: "Could not remove the key." }
                        }
                        saving = false
                    }
                }, enabled = !saving) { Text("Remove DeepSeek key") }
                Text("The key is encrypted separately using Android Keystore and excluded from backups. Enter it here, never in chat or lore.", color = Muted, style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = {
                    loading = true; error = null
                    scope.launch {
                        try {
                            models = vm.deepSeekModelIds(key)
                            if (models.isEmpty()) error = "No model IDs were returned. You can enter one manually."
                        } catch (e: kotlinx.coroutines.CancellationException) { throw e }
                          catch (_: Exception) { error = "Could not load DeepSeek models. Check your key/connection, or enter a model ID manually." }
                        finally { loading = false }
                    }
                }, enabled = !saving && !loading) { Text(if (loading) "Loading…" else "Load DeepSeek model IDs") }
                Text("Loading sends only your API key to DeepSeek, not your story. You can also enter a model ID manually.", color = Muted, style = MaterialTheme.typography.bodySmall)
                models.forEach { id -> TextButton(onClick = { model = id }) { Text(id) } }
                Row { Checkbox(consent, { consent = it }); Text("Allow selected story text to be sent directly to DeepSeek and accept its API charges.", Modifier.weight(1f)) }
                Row { Checkbox(active, { active = it }); Text("Use DeepSeek for future text requests", Modifier.weight(1f)) }
                Text("OpenRouter's Free only setting does not apply to this separately authorized API connection. Switching makes no request and does not resend a failed turn. Turn this off to return to OpenRouter. There is no automatic fallback or retry.", color = Muted)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Button(onClick = {
                    saving = true
                    scope.launch {
                        error = vm.saveDeepSeek(model, key, consent, active)
                        saving = false
                        if (error == null) { key = ""; close() }
                    }
                }, enabled = !saving && !loading, modifier = Modifier.fillMaxWidth()) { Text("Save DeepSeek settings") }
            }
        }
    }
}
