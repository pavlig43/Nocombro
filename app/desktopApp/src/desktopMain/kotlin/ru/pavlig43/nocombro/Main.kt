package ru.pavlig43.nocombro

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import ru.pavlig43.coreui.KeyEventHandler
import ru.pavlig43.coreui.isEscKeyDown
import ru.pavlig43.coreui.isEscKeyUp
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
        var escKeyDownSeen by remember { mutableStateOf(false) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Nocombro",
            state = windowState,
            onKeyEvent = { event ->
                when {
                    // ESC KeyDown дошёл до Window — значит ни один ребёнок его не потребил
                    event.isEscKeyDown -> {
                        escKeyDownSeen = true
                        false
                    }
                    // ESC KeyUp: закрываем вкладку только если KeyDown тоже дошёл до Window
                    event.isEscKeyUp -> {
                        val seen = escKeyDownSeen
                        escKeyDownSeen = false
                        if (seen) backDispatcher.back()
                        seen
                    }
                    else -> KeyEventHandler.handle(event)
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
