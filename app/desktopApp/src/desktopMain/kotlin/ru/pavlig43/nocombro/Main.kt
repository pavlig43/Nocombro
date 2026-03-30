package ru.pavlig43.nocombro

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.vinceglb.filekit.FileKit
import org.koin.java.KoinJavaComponent.getKoin
import ru.pavlig43.rootnocombro.api.RootDependencies
import ru.pavlig43.rootnocombro.api.component.RootNocombroComponent
import ru.pavlig43.rootnocombro.api.ui.App
import ru.pavlig43.rootnocombro.internal.di.initKoin

fun main() {

    Logger.setLogWriters(emptyList())


    val lifecycle = LifecycleRegistry()
    initKoin {}
    FileKit.init(appId = "Nocombro")

    val backDispatcher = BackDispatcher()

    val rootNocombroComponent =
        runOnUiThread {

            RootNocombroComponent(
                componentContext = DefaultComponentContext(
                    lifecycle = lifecycle,
                    backHandler = backDispatcher
                ),
                rootDependencies = getKoin().get<RootDependencies>()
            )
        }

    application {
        val windowState = rememberWindowState()

        Window(
            onCloseRequest = ::exitApplication,
            title = "Nocombro",
            state = windowState,
            onPreviewKeyEvent = { event ->
                if (event.key == Key.Escape && event.type == KeyEventType.KeyUp) {
                    backDispatcher.back()
                    true
                } else {
                    false
                }
            }
        ) {
            LifecycleController(
                lifecycleRegistry = lifecycle,
                windowState = windowState,
                windowInfo = LocalWindowInfo.current,
            )
            App(rootNocombroComponent)


        }
    }
}
