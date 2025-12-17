package ru.pavlig43.nocombro

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.rootnocombro.api.component.IRootNocombroComponent
import ru.pavlig43.rootnocombro.api.ui.RootNocombroScreen
import ru.pavlig43.theme.NocombroTheme

@Composable
fun App(rootNocombroComponent: IRootNocombroComponent,) {
    val darkMode by rootNocombroComponent.settingsComponent.darkMode.collectAsState()

    NocombroTheme(darkTheme = darkMode) {
        Column(Modifier.fillMaxSize()) {

            RootNocombroScreen(rootNocombroComponent)
        }


    }
}




