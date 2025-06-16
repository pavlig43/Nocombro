package ru.pavlig43.nocombro

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.getKoin
import ru.pavlig43.database.NocombroDatabase
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.rootnocombro.api.IRootDependencies
import ru.pavlig43.rootnocombro.api.component.RootNocombroComponent
import javax.swing.SwingUtilities


fun main() {
    val lifecycle = LifecycleRegistry()
    initKoin {}

    // Always create the root component outside Compose on the UI thread

    val rootNocombroComponent =
        runOnUiThread {
            RootNocombroComponent(
                componentContext = DefaultComponentContext(lifecycle = lifecycle),
                rootDependencies = getKoin().get<IRootDependencies>()
            )
        }
    application {
        val windowState = rememberWindowState()
        Window(
            onCloseRequest = ::exitApplication,
            alwaysOnTop = true,
            title = "Nocombro",
            state = windowState
        ) {
            App(rootNocombroComponent)

        }
    }
}


