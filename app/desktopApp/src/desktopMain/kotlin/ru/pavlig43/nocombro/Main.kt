package ru.pavlig43.nocombro

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.backhandler.LocalCompatNavigationEventDispatcherOwner
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
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
import kotlinx.coroutines.runBlocking
import org.koin.java.KoinJavaComponent.getKoin
import ru.pavlig43.coreui.KeyEventHandler
import ru.pavlig43.rootnocombro.api.RootDependencies
import ru.pavlig43.rootnocombro.api.runMirrorStartupMaintenance
import ru.pavlig43.rootnocombro.api.component.RootNocombroComponent
import ru.pavlig43.rootnocombro.api.ui.App
import ru.pavlig43.rootnocombro.internal.di.initKoin
import javax.imageio.ImageIO

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

private fun loadWindowIcon(): BitmapPainter {
    val stream = requireNotNull(Thread.currentThread().contextClassLoader.getResourceAsStream("icons/nocombro.png")) {
        "Window icon resource was not found"
    }
    return stream.use {
        BitmapPainter(ImageIO.read(it).toComposeImageBitmap())
    }
}

@Suppress("UnnecessarySafeCall")
fun main() {

    Logger.setLogWriters(emptyList())


    val lifecycle = LifecycleRegistry()
    initKoin {}
    FileKit.init(appId = "Nocombro")

    val backDispatcher = BackDispatcher()
    val rootDependencies = getKoin().get<RootDependencies>()
    runBlocking { runMirrorStartupMaintenance(rootDependencies) }?.let { Logger.i(it) }

    val rootNocombroComponent =
        runOnUiThread {

            RootNocombroComponent(
                componentContext = DefaultComponentContext(
                    lifecycle = lifecycle,
                    backHandler = backDispatcher
                ),
                rootDependencies = rootDependencies
            )
        }

    @OptIn(ExperimentalComposeUiApi::class)
    application {
        val windowState = rememberWindowState()
        val windowIcon = remember { loadWindowIcon() }

        Window(
            onCloseRequest = ::exitApplication,
            title = "Nocombro",
            state = windowState,
            icon = windowIcon,
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
