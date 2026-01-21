package com.devtoolbox

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import com.devtoolbox.navigation.NavGraph
import com.devtoolbox.ui.theme.DevToolboxTheme

@Composable
fun DevToolboxApp() {
    DevToolboxTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            NavGraph()
        }
    }
}

