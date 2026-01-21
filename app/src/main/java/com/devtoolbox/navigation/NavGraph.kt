package com.devtoolbox.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.devtoolbox.ui.home.HomeScreen
import com.devtoolbox.ui.tools.*

@Composable
fun NavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) { HomeScreen(onOpen = { nav.navigate(it) }) }
        composable(Routes.LOGCAT) { LogcatScreen() }
        composable(Routes.INTENT) { IntentLauncherScreen() }
        composable(Routes.PACKAGES) { PackagesScreen() }
        composable(Routes.APK_INFO) { ApkInfoScreen() }
        composable(Routes.DEEPLINK) { DeepLinkScreen() }
        composable(Routes.CLIPBOARD) { ClipboardScreen() }
    }
}

