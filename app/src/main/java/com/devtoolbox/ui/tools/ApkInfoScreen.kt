package com.devtoolbox.ui.tools

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ApkInfoScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("APK Info Viewer") }) }) { padding ->
        Column(Modifier.padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("v1 placeholder")
            Text("Planned: SAF picker, parse APK, show permissions/sdk/abi/size.")
        }
    }
}

