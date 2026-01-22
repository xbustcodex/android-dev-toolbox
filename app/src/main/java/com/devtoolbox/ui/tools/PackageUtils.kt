package com.devtoolbox.ui.tools

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.io.File

fun loadInstalledApps(context: Context,includeSystemApps: Boolean = false,launchableOnly: Boolean = true): List<AppInfo> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

    return apps
        .asSequence()
        // Keep launchable apps to avoid listing every internal package
        .filter {
            if (launchableOnly) {
                pm.getLaunchIntentForPackage(it.packageName) != null
            } else {
                true
            }
        }
        .mapNotNull { app ->
            try {
                val pkgInfo = pm.getPackageInfo(app.packageName, 0)

                val isSystem = (app.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                if (!includeSystemApps && isSystem) return@mapNotNull null

                val apkPath = app.sourceDir ?: ""
                val apkSize = runCatching { File(apkPath).length() }.getOrDefault(0L)

                AppInfo(
                    name = pm.getApplicationLabel(app).toString(),
                    packageName = app.packageName,
                    versionName = pkgInfo.versionName ?: "N/A",
                    versionCode = if (Build.VERSION.SDK_INT >= 28) pkgInfo.longVersionCode else pkgInfo.versionCode.toLong(),
                    icon = pm.getApplicationIcon(app),
                    isSystemApp = isSystem,
                    apkSizeBytes = apkSize,
                    lastUpdateTime = pkgInfo.lastUpdateTime,
                    firstInstallTime = pkgInfo.firstInstallTime
                )
            } catch (_: Exception) {
                null
            }
        }
        .sortedBy { it.name.lowercase() }
        .toList()
}
