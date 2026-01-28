package ru.pavlig43.rootnocombro.internal.topbar.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.rootnocombro.api.component.SettingsComponent
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.menu

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NocombroAppBar(settingsComponent: SettingsComponent, onOpenDrawer: () -> Unit) {
    val darkMode by settingsComponent.darkMode.collectAsState()
    CenterAlignedTopAppBar(
        title = { Text(text = "Nocombro") },
        navigationIcon = {
            IconButton(onOpenDrawer){
                Icon(painter = painterResource( Res.drawable.menu), contentDescription = "Menu")
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