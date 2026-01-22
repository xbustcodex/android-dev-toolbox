@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.devtoolbox.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.devtoolbox.navigation.Routes

@Composable
fun HomeScreen(onOpen: (String) -> Unit) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Android Dev Toolbox") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ToolCard("Logcat Viewer", "Foreground safe mode", onClick = { onOpen(Routes.LOGCAT) })
            ToolCard("Intent Launcher", "Launch activities / intents", onClick = { onOpen(Routes.INTENT) })
            ToolCard("App & Package Viewer", "Inspect installed apps", onClick = { onOpen(Routes.PACKAGES) })
            ToolCard("APK Info Viewer", "Inspect APK file details", onClick = { onOpen(Routes.APK_INFO) })
            ToolCard("Deep Link Tester", "Test URIs & app routing", onClick = { onOpen(Routes.DEEPLINK) })
            ToolCard("Clipboard Inspector", "View clipboard text", onClick = { onOpen(Routes.CLIPBOARD) })
        }
    }
}

@Composable
private fun ToolCard(title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

