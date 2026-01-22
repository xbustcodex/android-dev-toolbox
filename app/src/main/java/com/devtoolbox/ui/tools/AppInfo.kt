package com.devtoolbox.ui.tools

import android.graphics.drawable.Drawable

data class AppInfo(
    val name: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val icon: Drawable,
    val isSystemApp: Boolean,
    val apkSizeBytes: Long,
    val lastUpdateTime: Long,
    val firstInstallTime: Long
)
