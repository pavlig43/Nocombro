package ru.pavlig43.rootnocombro.internal.topbar.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ru.pavlig43.rootnocombro.internal.settings.component.ISettingsComponent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NocombroAppBar(settingsComponent: ISettingsComponent, onOpenDrawer: () -> Unit) {
    val darkMode by settingsComponent.darkMode.collectAsState()
    CenterAlignedTopAppBar(
        title = { Text(text = "Nocombro") },
        navigationIcon = {
            IconButton(onOpenDrawer){
                Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = settingsComponent::toggleDarkMode) {
                Icon(
                    imageVector = if (darkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle theme"
                )
            }
        }
    )
}