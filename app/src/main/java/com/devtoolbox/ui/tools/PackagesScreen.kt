package com.devtoolbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PackagesScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("App & Package Viewer") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("v1 placeholder")
            Text("Planned: app list, details, open App Info, open Play Store.")
        }
    }
}

