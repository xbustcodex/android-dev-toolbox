package com.devtoolbox.ui.tools

import android.content.Context

data class DeepLinkHistoryItem(
    val uri: String,
    val timestamp: Long
)

private const val PREFS = "devtoolbox_deeplink"
private const val KEY_HISTORY = "history_lines"
private const val MAX_ITEMS = 20

// Stored as lines: "timestamp|uri"
fun loadDeepLinkHistory(context: Context): List<DeepLinkHistoryItem> {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val raw = prefs.getString(KEY_HISTORY, "").orEmpty()
    if (raw.isBlank()) return emptyList()

    return raw
        .split("\n")
        .mapNotNull { line ->
            val idx = line.indexOf('|')
            if (idx <= 0) return@mapNotNull null
            val ts = line.substring(0, idx).toLongOrNull() ?: return@mapNotNull null
            val uri = line.substring(idx + 1)
            if (uri.isBlank()) return@mapNotNull null
            DeepLinkHistoryItem(uri = uri, timestamp = ts)
        }
        .sortedByDescending { it.timestamp }
}

fun saveDeepLinkHistory(context: Context, items: List<DeepLinkHistoryItem>) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    val trimmed = items
        .distinctBy { it.uri.trim() }
        .sortedByDescending { it.timestamp }
        .take(MAX_ITEMS)

    val raw = trimmed.joinToString("\n") { "${it.timestamp}|${it.uri}" }
    prefs.edit().putString(KEY_HISTORY, raw).apply()
}

fun addDeepLinkHistory(context: Context, uri: String) {
    val clean = uri.trim()
    if (clean.isBlank()) return
    val existing = loadDeepLinkHistory(context).filterNot { it.uri.equals(clean, ignoreCase = true) }
    val updated = listOf(DeepLinkHistoryItem(clean, System.currentTimeMillis())) + existing
    saveDeepLinkHistory(context, updated)
}

fun clearDeepLinkHistory(context: Context) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    prefs.edit().remove(KEY_HISTORY).apply()
}
