package com.devtoolbox.ui.tools

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun loadInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)

    return apps
        .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
        .map {
            val pkgInfo = pm.getPackageInfo(it.packageName, 0)
            AppInfo(
                name = pm.getApplicationLabel(it).toString(),
                packageName = it.packageName,
                versionName = pkgInfo.versionName ?: "N/A",
                versionCode = if (Build.VERSION.SDK_INT >= 28)
                    pkgInfo.longVersionCode else pkgInfo.versionCode.toLong(),
                icon = pm.getApplicationIcon(it)
            )
        }
        .sortedBy { it.name.lowercase() }
}
