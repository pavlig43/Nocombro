package ru.pavlig43.nocombro

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.backhandler.LocalCompatNavigationEventDispatcherOwner
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import androidx.navigationevent.NavigationEventHandler
import androidx.navigationevent.NavigationEventInfo
import co.touchlab.kermit.Logger
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import io.github.vinceglb.filekit.FileKit
import org.koin.java.KoinJavaComponent.getKoin
import ru.pavlig43.coreui.KeyEventHandler
import ru.pavlig43.rootnocombro.api.RootDependencies
import ru.pavlig43.rootnocombro.api.component.RootNocombroComponent
import ru.pavlig43.rootnocombro.api.ui.App
import ru.pavlig43.rootnocombro.internal.di.initKoin

@OptIn(InternalComposeUiApi::class)
@Composable
private fun WindowBackHandler(onBack: () -> Unit) {
    val owner = LocalCompatNavigationEventDispatcherOwner.current ?: error(
        "No NavigationEventDispatcher was provided via LocalCompatNavigationEventDispatcherOwner"
    )
    val dispatcher = owner.navigationEventDispatcher
    val currentOnBack = rememberUpdatedState(onBack)
    val handler = remember {
        object : NavigationEventHandler<NavigationEventInfo.None>(
            initialInfo = NavigationEventInfo.None,
            isBackEnabled = true
        ) {
            override fun onBackCompleted() {
                currentOnBack.value()
            }
        }
    }

    DisposableEffect(dispatcher, handler) {
        dispatcher.addHandler(handler)
        onDispose { handler.remove() }
    }
}

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

    @OptIn(ExperimentalComposeUiApi::class)
    application {
        val windowState = rememberWindowState()

        Window(
            onCloseRequest = ::exitApplication,
            title = "Nocombro",
            state = windowState,
            onKeyEvent = KeyEventHandler::handle
        ) {
            WindowBackHandler {
                backDispatcher.back()
            }
            LifecycleController(
                lifecycleRegistry = lifecycle,
                windowState = windowState,
                windowInfo = LocalWindowInfo.current,
            )
            App(rootNocombroComponent)


        }
    }
}
