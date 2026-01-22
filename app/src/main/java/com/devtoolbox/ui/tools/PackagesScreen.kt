package com.devtoolbox.ui.tools

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppFilter { USER, SYSTEM, ALL }
private enum class SortMode { AZ, SIZE, RECENT, INSTALLED }

@Composable
fun PackagesScreen() {
    val context = LocalContext.current

    var allApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var query by remember { mutableStateOf(TextFieldValue("")) }
    var launchableOnly by remember { mutableStateOf(true) }

    var filter by remember { mutableStateOf(AppFilter.USER) }
    var sortMode by remember { mutableStateOf(SortMode.AZ) }
    var sortMenuOpen by remember { mutableStateOf(false) }

    // Load apps whenever filter changes (system apps toggle affects data source)
    LaunchedEffect(filter, launchableOnly) {
        val includeSystem = (filter == AppFilter.SYSTEM || filter == AppFilter.ALL)
        allApps = loadInstalledApps(
            context,
            includeSystemApps = includeSystem,
            launchableOnly = launchableOnly
        )
    }
    Row(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Launchable only",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = launchableOnly,
            onCheckedChange = { launchableOnly = it }
        )
    }

    val visibleApps = remember(allApps, query, filter, sortMode) {
        // filter (user/system/all)
        val filteredByType = when (filter) {
            AppFilter.USER -> allApps.filter { !it.isSystemApp }
            AppFilter.SYSTEM -> allApps.filter { it.isSystemApp }
            AppFilter.ALL -> allApps
        }

        // search
        val q = query.text.trim().lowercase()
        val searched = if (q.isEmpty()) filteredByType else {
            filteredByType.filter { app ->
                app.name.lowercase().contains(q) || app.packageName.lowercase().contains(q)
            }
        }

        // sorting
        when (sortMode) {
            SortMode.AZ -> searched.sortedBy { it.name.lowercase() }
            SortMode.SIZE -> searched.sortedByDescending { it.apkSizeBytes }
            SortMode.RECENT -> searched.sortedByDescending { it.lastUpdateTime }
            SortMode.INSTALLED -> searched.sortedByDescending { it.lastUpdateTime } // placeholder
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Installed Apps") },
                actions = {
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = "Sort")
                    }
                    DropdownMenu(
                        expanded = sortMenuOpen,
                        onDismissRequest = { sortMenuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort: A–Z") },
                            onClick = { sortMode = SortMode.AZ; sortMenuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort: Size") },
                            onClick = { sortMode = SortMode.SIZE; sortMenuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort: Install Date") },
                            onClick = { sortMode = SortMode.INSTALLED; sortMenuOpen = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort: Recent Update") },
                            onClick = { sortMode = SortMode.RECENT; sortMenuOpen = false }
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // Search + Clear button
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search apps (name or package)") },
                singleLine = true,
                trailingIcon = {
                    if (query.text.isNotEmpty()) {
                        IconButton(onClick = { query = TextFieldValue("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            )

            // Filter toggles
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filter == AppFilter.USER,
                    onClick = { filter = AppFilter.USER },
                    label = { Text("User") }
                )
                FilterChip(
                    selected = filter == AppFilter.SYSTEM,
                    onClick = { filter = AppFilter.SYSTEM },
                    label = { Text("System") }
                )
                FilterChip(
                    selected = filter == AppFilter.ALL,
                    onClick = { filter = AppFilter.ALL },
                    label = { Text("All") }
                )
            }

            // Results summary
            Text(
                text = "Showing ${visibleApps.size} apps • Sort: ${sortLabel(sortMode)}",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(visibleApps, key = { it.packageName }) { app ->
                    AppRow(app = app)
                }
            }
        }
    }
}

private fun sortLabel(mode: SortMode): String = when (mode) {
    SortMode.AZ -> "A–Z"
    SortMode.SIZE -> "Size"
    SortMode.RECENT -> "Recent"
    SortMode.INSTALLED -> "Installed"
}

@Composable
private fun AppRow(app: AppInfo) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .clickable {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                    }
                    context.startActivity(intent)
                }
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    bitmap = app.icon.toBitmap(96, 96).asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(app.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        if (app.isSystemApp) "System app" else "User app",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                "Version ${app.versionName} (${app.versionCode})",
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "APK ${formatBytes(app.apkSizeBytes)} • Updated ${formatDate(app.lastUpdateTime)} • Installed ${formatDate(app.firstInstallTime)}"
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(onClick = {
                    val marketIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("market://details?id=${app.packageName}")
                    )
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://play.google.com/store/apps/details?id=${app.packageName}")
                    )
                    try {
                        context.startActivity(marketIntent)
                    } catch (_: Exception) {
                        context.startActivity(webIntent)
                    }
                }) { Text("Play Store") }

                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${app.packageName}")
                    }
                    context.startActivity(intent)
                }) { Text("App Info") }
            }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format(Locale.US, "%.2f GB", bytes / gb)
        bytes >= mb -> String.format(Locale.US, "%.2f MB", bytes / mb)
        bytes >= kb -> String.format(Locale.US, "%.1f KB", bytes / kb)
        else -> "$bytes B"
    }
}

private fun formatDate(ms: Long): String {
    val df = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return df.format(Date(ms))
}
