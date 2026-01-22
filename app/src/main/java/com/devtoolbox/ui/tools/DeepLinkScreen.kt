package com.devtoolbox.ui.tools

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun DeepLinkScreen() {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var uriText by remember { mutableStateOf(TextFieldValue("")) }
    var lastResult by remember { mutableStateOf<String?>(null) }

    fun showToast(msg: String) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    fun normalizeUri(input: String): String {
        val s = input.trim()
        if (s.isEmpty()) return s
        // If user types "example.com" assume https
        return if (!s.contains("://")) "https://$s" else s
    }

    fun launchDeepLink(raw: String) {
        val normalized = normalizeUri(raw)
        if (normalized.isBlank()) {
            lastResult = "❌ Enter a URI first."
            return
        }

        val uri = runCatching { Uri.parse(normalized) }.getOrNull()
        if (uri == null) {
            lastResult = "❌ Invalid URI."
            return
        }

        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            // Check if something can handle it
            val resolved = context.packageManager.resolveActivity(intent, 0)
            if (resolved == null) {
                lastResult = "❌ No app found to handle: $normalized"
                return
            }

            context.startActivity(intent)
            lastResult = "✅ Launched: $normalized"
        } catch (e: ActivityNotFoundException) {
            lastResult = "❌ No activity found to handle: $normalized"
        } catch (e: Exception) {
            lastResult = "❌ Error launching URI: ${e.message}"
        }
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
                    Spacer(Modifier.width(8.dp))
                    Text("Paste")
                }
            }

            if (lastResult != null) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = lastResult!!,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Divider()

            Text(
                "Tips:",
                style = MaterialTheme.typography.titleSmall
            )
            Text("• If you enter a domain without a scheme, https:// is added automatically.")
            Text("• Use custom schemes like myapp:// to test app deep link routing.")
        }
    }
}
