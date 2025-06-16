package ru.pavlig43.rootnocombro.api.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.serialization.Serializable
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.document.api.component.DocumentComponent
import ru.pavlig43.rootnocombro.api.IRootDependencies
import ru.pavlig43.rootnocombro.intetnal.di.createRootNocombroModule
import ru.pavlig43.signroot.api.component.RootSignComponent

class RootNocombroComponent(
    componentContext: ComponentContext,
    rootDependencies: IRootDependencies
) : IRootNocombroComponent, ComponentContext by componentContext {

    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(
        createRootNocombroModule(
        rootDependencies
        )
    )


    private val stackNavigation = StackNavigation<Config>()

    override val stack: Value<ChildStack<Config, IRootNocombroComponent.Child>> = childStack<ComponentContext,Config, IRootNocombroComponent.Child>(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = Config.Document,
        handleBackButton = false,
        childFactory = ::createChild
    )

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): IRootNocombroComponent.Child {
        return when (config) {
            Config.Sign -> IRootNocombroComponent.Child.RootSign(
                RootSignComponent(
                    componentContext = componentContext,
                    rootSignDependencies = scope.get(),
                    signIn = { stackNavigation.pushToFront(Config.Document) },
                    signUp = { stackNavigation.pushToFront(Config.Document) }
                )

            )

            Config.Document -> IRootNocombroComponent.Child.Document(
                DocumentComponent(
                    componentContext = componentContext,
                    dependencies = scope.get()
                )
            )

            Config.Tab -> IRootNocombroComponent.Child.Tab()
        }
    }

    @Serializable
    sealed interface Config {

        @Serializable
        data object Sign : Config

        @Serializable
        data object Document : Config

        @Serializable
        data object Tab : Config

    }
}

@Composable
fun TabScreen() {
    var tabIndex by remember { mutableStateOf(0) }

    val tabs = listOf("Home", "About", "Settings")

    Column(modifier = Modifier.fillMaxWidth()) {
        TabRow(selectedTabIndex = tabIndex) {
            tabs.forEachIndexed { index, title ->
                Tab(text = { Text(title) },
                    selected = tabIndex == index,
                    onClick = { tabIndex = index }
                )
            }
        }
        when (tabIndex) {
            0 -> Box{Text("0")}
            1 -> Box{Text("1")}
            2 -> Box{Text("2")}
        }
    }
}




