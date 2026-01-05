package ru.pavlig43.nocombro

import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.vinceglb.filekit.FileKit
import org.koin.java.KoinJavaComponent.getKoin
import ru.pavlig43.rootnocombro.api.RootDependencies
import ru.pavlig43.rootnocombro.api.component.RootNocombroComponent


fun main() {
    val lifecycle = LifecycleRegistry()
    initKoin {}
    FileKit.init(appId = "Nocombro")

    // Always create the root component outside Compose on the UI thread

    val rootNocombroComponent =
        runOnUiThread {

            RootNocombroComponent(
                componentContext = DefaultComponentContext(lifecycle = lifecycle),
                rootDependencies = getKoin().get<RootDependencies>()
            )
        }

    application {
        val windowState = rememberWindowState()

        Window(
            onCloseRequest = ::exitApplication,
//            alwaysOnTop = true,
            title = "Nocombro",
            state = windowState
        ) {
            LifecycleController(
                lifecycleRegistry = lifecycle,
                windowState = windowState,
                windowInfo = LocalWindowInfo.current,
            )
//            SampleApp(rootNocombroComponent)
            App(rootNocombroComponent)


        }
    }
}



