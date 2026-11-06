@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package org.voidcat.voidcanon.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.pm.PackageInfoCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.voidcat.voidcanon.CanonViewModel

data class AppIdentity(val name: String, val version: String, val build: Long, val packageName: String)

@Composable
fun rememberAppIdentity(): AppIdentity {
    val context = LocalContext.current
    return remember(context) {
        @Suppress("DEPRECATION")
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        AppIdentity(context.getString(org.voidcat.voidcanon.R.string.app_name), info.versionName.orEmpty(),
            PackageInfoCompat.getLongVersionCode(info), context.packageName)
    }
}

/** Kept outside scrolling content so every sheet has an immediately reachable exit. */
@Composable
fun SheetHeader(title: String, close: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        actions()
        TextButton(onClick = close) { Text("Close") }
    }
    HorizontalDivider()
}

@Composable
fun AppSettingsSheet(vm: CanonViewModel, close: () -> Unit, ai: () -> Unit, images: () -> Unit, backup: () -> Unit) {
    val app = rememberAppIdentity()
    val connected by vm.imageConnected.collectAsStateWithLifecycle()
    val config by vm.configuration.collectAsStateWithLifecycle()
    ModalBottomSheet(onDismissRequest = close, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.9f)) {
            SheetHeader("Settings", close)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(app.name, style = MaterialTheme.typography.headlineMedium)
                Text("Version ${app.version} · build ${app.build}", color = Violet)
                Text(if (app.packageName.endsWith(".preview")) "Separate test app" else "Regular Void Canon app", color = Muted)
                Text("The version shown here identifies the app installed on this device.", style = MaterialTheme.typography.bodySmall, color = Muted)
                HorizontalDivider()
                SettingsLink("AI & models", "OpenRouter · roleplay and image descriptions" +
                    if (config.rpModel.isNotBlank()) "\nRP: ${config.rpModel}" else "\nChoose models and manage your key", ai)
                SettingsLink("Artwork connection", if (connected) "Cloudflare image service connected" else "Connect Cloudflare image generation", images)
                SettingsLink("Backups & data", "Export or restore your local world library", backup)
                HorizontalDivider()
                Text("Local world editing and saved artwork work offline. Provider connections are optional. Keys and access codes are excluded from portable backups.", style = MaterialTheme.typography.bodySmall, color = Muted)
                Text("App identifier: ${app.packageName}", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}

@Composable
private fun SettingsLink(title: String, subtitle: String, open: () -> Unit) {
    OutlinedButton(onClick = open, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Muted)
        }
        Icon(CanonIcons.Right, null)
    }
}

@Composable
fun ImageConnectionControls(vm: CanonViewModel) {
    val connected by vm.imageConnected.collectAsStateWithLifecycle()
    val endpoint by vm.imageEndpoint.collectAsStateWithLifecycle()
    val generating by vm.generating.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(vm::importImageConnection) }
    if (connected) {
        Text("Connected to $endpoint", style = MaterialTheme.typography.bodySmall, color = Muted)
        TextButton(onClick = vm::disconnectImages, enabled = generating == null && !busy) { Text("Disconnect image service") }
    } else {
        Text("Import your private image connection file once to connect this device to the development image service.", color = Muted)
    }
    OutlinedButton(onClick = { picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }, enabled = generating == null && !busy) {
        Text(if (connected) "Replace image connection" else "Import image connection")
    }
}

@Composable
fun ImageConnectionSettingsSheet(vm: CanonViewModel, close: () -> Unit) {
    ModalBottomSheet(onDismissRequest = close, containerColor = Panel, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxHeight(.8f)) {
            SheetHeader("Artwork connection", close)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Cloudflare · FLUX.2 Klein 4B", style = MaterialTheme.typography.titleLarge)
                ImageConnectionControls(vm)
                Text("Generate from Gallery after selecting a character or location. Review the prompt and optional reference before sending them.", color = Muted)
                Text("The connection code is encrypted on this device and excluded from backups. Disconnecting leaves saved artwork intact.", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
