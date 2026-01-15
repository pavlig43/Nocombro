package ru.pavlig43.rootnocombro.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pushToFront
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.serialization.Serializable
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.rootnocombro.api.RootDependencies
import ru.pavlig43.rootnocombro.internal.di.createRootNocombroModule
import ru.pavlig43.rootnocombro.internal.navigation.MainTabNavigationComponent
import ru.pavlig43.signroot.api.component.IRootSignComponent
import ru.pavlig43.signroot.api.component.RootSignComponent

class RootNocombroComponent(
    componentContext: ComponentContext,
    rootDependencies: RootDependencies
) : ComponentContext by componentContext {

    private val koinContext = instanceKeeper.getOrCreate { ComponentKoinContext() }
    private val scope = koinContext.getOrCreateKoinScope(
        createRootNocombroModule(
            rootDependencies
        )
    )

    private val stackNavigation = StackNavigation<RootConfig>()

    internal val stack: Value<ChildStack<RootConfig, RootChild>> =
        childStack<ComponentContext, RootConfig, RootChild>(
            source = stackNavigation,
            serializer = RootConfig.serializer(),
            initialConfiguration = RootConfig.Tabs,
            handleBackButton = false,
            childFactory = ::createChild
        )
    val settingsComponent = SettingsComponent(
        componentContext = childContext("settings"),
        settingsRepository = scope.get()
    )


    private fun createChild(
        rootConfig: RootConfig,
        componentContext: ComponentContext
    ): RootChild {
        return when (rootConfig) {
            RootConfig.Sign -> RootChild.RootSign(
                RootSignComponent(
                    componentContext = componentContext,
                    rootSignDependencies = scope.get(),
                    signIn = { stackNavigation.pushToFront(RootConfig.Tabs) },
                    signUp = { stackNavigation.pushToFront(RootConfig.Tabs) }
                )

            )


            RootConfig.Tabs -> RootChild.Tabs(
                MainTabNavigationComponent(
                componentContext = childContext("tabs"),
                scope = scope
            )
            )
        }
    }

    @Serializable
    sealed interface RootConfig {

        @Serializable
        data object Sign : RootConfig

        @Serializable
        data object Tabs:RootConfig

    }
}
internal sealed interface RootChild {
    class RootSign(val component: IRootSignComponent) : RootChild
    class Tabs(
        val component: MainTabNavigationComponent
    ):RootChild
}





