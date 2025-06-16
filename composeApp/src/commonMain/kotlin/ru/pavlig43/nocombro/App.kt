package ru.pavlig43.nocombro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import org.jetbrains.compose.ui.tooling.preview.Preview
import ru.pavlig43.rootnocombro.api.component.IRootNocombroComponent
import ru.pavlig43.rootnocombro.api.ui.RootNocombroScreen
import ru.pavlig43.theme.NocombroTheme

@Composable
fun App(rootNocombroComponent: IRootNocombroComponent) {
    var darkMode by remember { mutableStateOf(false) }
    NocombroTheme(darkTheme = darkMode) {
        Column(Modifier.fillMaxSize()) {
            AppBar(darkMode) { darkMode = !darkMode }
            RootNocombroScreen(rootNocombroComponent)
        }


    }
}

/**
 * TODO() удалить
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppBar(isDark: Boolean, onToggleTheme: () -> Unit) {
    TopAppBar(
        title = { Text(text = "Nocombro") },
        actions = {
            IconButton(onClick = onToggleTheme) {
                Icon(
                    imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme"
                )
            }
        }
    )
}


