@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.devtoolbox.ui.tools

import android.os.Process
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

private enum class LogLevel(val label: String, val letter: String) {
    VERBOSE("Verbose", "V"),
    DEBUG("Debug", "D"),
    INFO("Info", "I"),
    WARN("Warn", "W"),
    ERROR("Error", "E"),
}

private data class LogLine(
    val raw: String,
    val level: String? = null
)

@Composable
fun LogcatScreen() {
    val appPid = remember { Process.myPid() }
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    var onlyThisApp by remember { mutableStateOf(true) }
    var selectedLevel by remember { mutableStateOf<LogLevel?>(null) } // null = all
    var query by remember { mutableStateOf("") }

    var logs by remember { mutableStateOf(listOf<LogLine>()) }

    suspend fun loadLogs() {
        isLoading = true
        errorText = null
        try {
            logs = readLogcatDump(
                pid = if (onlyThisApp) appPid else null,
                minLevel = selectedLevel?.letter
            )
        } catch (t: Throwable) {
            errorText = t.message ?: "Failed to read logcat"
        } finally {
            isLoading = false
        }
    }

    val filtered = remember(logs, query) {
        if (query.isBlank()) logs
        else logs.filter { it.raw.contains(query, ignoreCase = true) }
    }

    // Initial load
    LaunchedEffect(Unit) { loadLogs() }

    // Auto-refresh when toggles change (optional but nice)
    LaunchedEffect(onlyThisApp, selectedLevel) { loadLogs() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Logcat Viewer") },
                actions = {
                    TextButton(
                        onClick = { scope.launch { loadLogs() } },
                        enabled = !isLoading
                    ) {
                        Text(if (isLoading) "Loading..." else "Refresh")
                    }
                    TextButton(onClick = { logs = emptyList() }) { Text("Clear") }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = onlyThisApp,
                    onClick = { onlyThisApp = !onlyThisApp },
                    label = {
                        Text(if (onlyThisApp) "PID: $appPid" else "All apps (may be blocked)")
                    }
                )

                LevelDropdown(
                    selected = selectedLevel,
                    onSelected = { selectedLevel = it },
                    modifier = Modifier.weight(1f)
                )
            }

            if (errorText != null) {
                AssistChip(
                    onClick = {},
                    label = { Text("Logcat error: ${errorText!!}") }
                )
                Text(
                    "Tip: Android blocks reading other apps’ logs. Keep PID enabled to read your app logs.",
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxSize()
            ) {
                if (filtered.isEmpty() && !isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No logs to display.")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtered) { line ->
                            Text(
                                text = line.raw,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Divider()
                        }
                    }
                }
            }
        }
    }
}

private suspend fun readLogcatDump(
    pid: Int? = null,
    minLevel: String? = null,
    maxLines: Int = 1500
): List<LogLine> = withContext(Dispatchers.IO) {
    val args = mutableListOf("logcat", "-d", "-v", "time")

    if (pid != null) {
        args += "--pid=$pid"
    }

    if (minLevel != null) {
        args += "*:$minLevel"
    }

    val process = ProcessBuilder(args)
        .redirectErrorStream(true)
        .start()

    val lines = mutableListOf<LogLine>()
    BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
        while (true) {
            val raw = reader.readLine() ?: break
            val level = raw.split(" ").firstOrNull { it in listOf("V", "D", "I", "W", "E", "A") }
            lines.add(LogLine(raw = raw, level = level))
            if (lines.size >= maxLines) break
        }
    }
    process.destroy()
    lines
}

@Composable
private fun LevelDropdown(
    selected: LogLevel?,
    onSelected: (LogLevel?) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.label ?: "All levels",
            onValueChange = {},
            readOnly = true,
            label = { Text("Level") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DropdownMenuItem(
                text = { Text("All levels") },
                onClick = {
                    onSelected(null)
                    expanded = false
                }
            )
            LogLevel.entries.forEach { lvl ->
                DropdownMenuItem(
                    text = { Text(lvl.label) },
                    onClick = {
                        onSelected(lvl)
                        expanded = false
                    }
                )
            }
        }
    }
}
