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
import ru.pavlig43.documentform.api.component.DocumentFormComponent
import ru.pavlig43.documentlist.api.component.DocumentListComponent
import ru.pavlig43.rootnocombro.api.IRootDependencies
import ru.pavlig43.rootnocombro.internal.di.createRootNocombroModule
import ru.pavlig43.rootnocombro.internal.navigation.drawer.component.DrawerDestination
import ru.pavlig43.rootnocombro.internal.navigation.tab.component.DefaultTabNavigationComponent
import ru.pavlig43.rootnocombro.internal.navigation.tab.component.TabConfig
import ru.pavlig43.rootnocombro.internal.settings.component.ISettingsComponent
import ru.pavlig43.rootnocombro.internal.settings.component.SettingsComponent
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

    override val stack: Value<ChildStack<Config, IRootNocombroComponent.Child>> =
        childStack<ComponentContext, Config, IRootNocombroComponent.Child>(
            source = stackNavigation,
            serializer = Config.serializer(),
            initialConfiguration = Config.Tabs,
            handleBackButton = false,
            childFactory = ::createChild
        )
    override val settingsComponent: ISettingsComponent = SettingsComponent(
        componentContext = childContext("settings"),
        settingsRepository = scope.get()
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
                    signIn = { stackNavigation.pushToFront(Config.Tabs) },
                    signUp = { stackNavigation.pushToFront(Config.Tabs) }
                )

            )


            Config.Tabs -> IRootNocombroComponent.Child.Tabs(
                DefaultTabNavigationComponent(
                    componentContext = componentContext,
                    startConfigurations = listOf(
                        TabConfig.DocumentList(),
                    ),
                    addConfigurationFromDrawer = { toTabConfig() },
                    serializer = TabConfig.serializer(),
                    slotFactory = { context, tabConfig: TabConfig, openNewTab: (TabConfig) -> Unit, onCloseTab: () -> Unit ->

                        when (tabConfig) {
                            is TabConfig.DocumentList -> DocumentListComponent(
                                componentContext = context,
                                onCreateScreen = { openNewTab(TabConfig.CreateDocument()) },
                                dependencies = scope.get(),
                                onItemClick = { openNewTab(TabConfig.ChangeDocument(it)) }
                            )

                            is TabConfig.CreateDocument -> DocumentFormComponent(
                                documentId = 0,
                                closeTab = onCloseTab,
                                componentContext = context,
                                dependencies = scope.get()
                            )

                            is TabConfig.ChangeDocument -> DocumentFormComponent(
                                componentContext = context,
                                dependencies = scope.get(),
                                closeTab = onCloseTab,
                                documentId = tabConfig.id
                            )
                        }
                    }

                )
            )
        }
    }

    private fun DrawerDestination.toTabConfig(): TabConfig =
        when (this) {
            is DrawerDestination.CreateDocument -> TabConfig.CreateDocument()
            is DrawerDestination.Documents -> TabConfig.DocumentList()
        }

    @Serializable
    sealed interface Config {

        @Serializable
        data object Sign : Config

        @Serializable
        data object Tabs : Config

    }
}






