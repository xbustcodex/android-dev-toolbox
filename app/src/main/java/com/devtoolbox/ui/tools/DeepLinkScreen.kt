package com.devtoolbox.ui.tools

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

data class ResolveDetails(
    val handlerLabel: String,
    val handlerPackage: String
)

@Composable
fun DeepLinkScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val pm = context.packageManager

    var uriText by remember { mutableStateOf(TextFieldValue("")) }
    var lastResult by remember { mutableStateOf<String?>(null) }
    var resolveDetails by remember { mutableStateOf<ResolveDetails?>(null) }

    var autoLaunchFromHistory by remember { mutableStateOf(false) }

    var history by remember { mutableStateOf<List<DeepLinkHistoryItem>>(emptyList()) }

    // Load persisted history once
    LaunchedEffect(Unit) {
        history = loadDeepLinkHistory(context)
    }

    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun normalizeUri(input: String): String {
        val s = input.trim()
        if (s.isEmpty()) return s
        // If user types "example.com" assume https
        return if (!s.contains("://")) "https://$s" else s
    }

    fun resolveHandler(raw: String): ResolveDetails? {
        val normalized = normalizeUri(raw)
        if (normalized.isBlank()) return null

        val uri = runCatching { Uri.parse(normalized) }.getOrNull() ?: return null
        val intent = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        val resolved = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
        val pkg = resolved.activityInfo?.packageName ?: return null
        val label = runCatching {
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        }.getOrElse { pkg }

        return ResolveDetails(handlerLabel = label, handlerPackage = pkg)
    }

    fun doResolve(raw: String) {
        val normalized = normalizeUri(raw)
        if (normalized.isBlank()) {
            lastResult = "❌ Enter a URI first."
            resolveDetails = null
            return
        }

        val details = resolveHandler(normalized)
        if (details == null) {
            lastResult = "❌ No app found to handle: $normalized"
            resolveDetails = null
        } else {
            lastResult = "✅ Resolved: $normalized"
            resolveDetails = details
        }
    }

    fun launchDeepLink(raw: String) {
        val normalized = normalizeUri(raw)
        if (normalized.isBlank()) {
            lastResult = "❌ Enter a URI first."
            resolveDetails = null
            return
        }

        val uri = runCatching { Uri.parse(normalized) }.getOrNull()
        if (uri == null) {
            lastResult = "❌ Invalid URI."
            resolveDetails = null
            return
        }

        // Resolve first (also gives handler details)
        val details = resolveHandler(normalized)
        if (details == null) {
            lastResult = "❌ No app found to handle: $normalized"
            resolveDetails = null
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

        runCatching {
            context.startActivity(intent)
            lastResult = "✅ Launched: $normalized"
            resolveDetails = details

            // Save to history on successful launch
            addDeepLinkHistory(context, normalized)
            history = loadDeepLinkHistory(context)
        }.onFailure { e ->
            lastResult = "❌ Error launching URI: ${e.message}"
        }
    }

    fun shareUri(raw: String) {
        val normalized = normalizeUri(raw)
        if (normalized.isBlank()) {
            showToast("Enter a URI first")
            return
        }

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, normalized)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share URI"))
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Deep Link Tester") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Test deep links and app routing by launching a URI.",
                style = MaterialTheme.typography.bodyMedium
            )

            OutlinedTextField(
                value = uriText,
                onValueChange = { uriText = it },
                label = { Text("URI (e.g. myapp://path or https://example.com)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            // Actions row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { launchDeepLink(uriText.text) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Launch")
                }

                OutlinedButton(onClick = { doResolve(uriText.text) }) {
                    Icon(Icons.Filled.Info, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Resolve")
                }

                OutlinedButton(onClick = { shareUri(uriText.text) }) {
                    Icon(Icons.Filled.Share, contentDescription = null)
                }

                OutlinedButton(
                    onClick = {
                        val clipText = clipboard.getText()?.text?.toString().orEmpty()
                        if (clipText.isBlank()) {
                            showToast("Clipboard is empty")
                        } else {
                            uriText = TextFieldValue(clipText)
                            showToast("Pasted from clipboard")
                        }
                    }
                ) {
                    Icon(Icons.Filled.ContentPaste, contentDescription = null)
                }
            }

            // Result card
            if (lastResult != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(lastResult!!, style = MaterialTheme.typography.bodyMedium)

                        resolveDetails?.let { details ->
                            Divider()

                            Text("Handler: ${details.handlerLabel}", style = MaterialTheme.typography.bodyMedium)

                            SelectionContainer {
                                Text(
                                    "Package: ${details.handlerPackage}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = {
                                    clipboard.setText(AnnotatedString(details.handlerPackage))
                                    showToast("Copied: ${details.handlerPackage}")
                                }) { Text("Copy package") }

                                OutlinedButton(onClick = {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${details.handlerPackage}")
                                    }
                                    context.startActivity(intent)
                                }) { Text("App info") }
                            }
                        }
                    }
                }
            }

            // Auto-launch toggle (GLOBAL)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Auto-launch when selecting history",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = autoLaunchFromHistory,
                    onCheckedChange = { autoLaunchFromHistory = it }
                )
            }

            // History header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Recent", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))

                if (history.isNotEmpty()) {
                    TextButton(onClick = {
                        clearDeepLinkHistory(context)
                        history = emptyList()
                        showToast("History cleared")
                    }) {
                        Icon(Icons.Filled.Delete, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Clear")
                    }
                }
            }

            // History list
            if (history.isEmpty()) {
                Text(
                    "No recent links yet. Launch a URI to add it here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history, key = { it.uri }) { item ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    uriText = TextFieldValue(item.uri)
                                    if (autoLaunchFromHistory) {
                                        launchDeepLink(item.uri)
                                    } else {
                                        doResolve(item.uri)
                                    }
                                }
                        ) {
                            Column(Modifier.padding(12.dp)) {
                                Text(item.uri, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    "${relativeTime(item.timestamp)} • Tap to load",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Divider()

            Text("Tips:", style = MaterialTheme.typography.titleSmall)
            Text("• If you enter a domain without a scheme, https:// is added automatically.")
            Text("• Use custom schemes like myapp:// to test app deep link routing.")
        }
    }
}

private fun relativeTime(ms: Long): String {
    val diff = System.currentTimeMillis() - ms
    val sec = diff / 1000
    val min = sec / 60
    val hr = min / 60
    val day = hr / 24

    return when {
        sec < 10 -> "just now"
        sec < 60 -> "${sec}s ago"
        min < 60 -> "${min}m ago"
        hr < 24 -> "${hr}h ago"
        day < 7 -> "${day}d ago"
        else -> "over a week ago"
    }
}
