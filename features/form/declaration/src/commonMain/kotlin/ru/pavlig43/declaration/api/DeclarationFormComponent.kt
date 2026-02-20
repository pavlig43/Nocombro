package ru.pavlig43.declaration.api

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.replaceAll
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.instancekeeper.getOrCreate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.serialization.Serializable
import org.koin.core.scope.Scope
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.core.emptyDate
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.corekoin.ComponentKoinContext
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.declaration.internal.create.component.CreateDeclarationSingleLineComponent
import ru.pavlig43.declaration.internal.di.createDeclarationFormModule
import ru.pavlig43.declaration.internal.model.DeclarationEssentialsUi
import ru.pavlig43.declaration.internal.model.toUi
import ru.pavlig43.declaration.internal.update.DeclarationFormTabsComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory

class DeclarationFormComponent(
    declarationId: Int,
    private val tabOpener: TabOpener,
    componentContext: ComponentContext,
    dependencies: DeclarationFormDependencies,
) : ComponentContext by componentContext, MainTabComponent {

    private val koinContext = instanceKeeper.getOrCreate {
        ComponentKoinContext()
    }

    private val scope: Scope =
        koinContext.getOrCreateKoinScope(createDeclarationFormModule(dependencies))

    private val _model = MutableStateFlow(MainTabComponent.NavTabState(""))
    override val model = _model.asStateFlow()

    private val stackNavigation = StackNavigation<Config>()

    private val componentFactory = SingleLineComponentFactory<Declaration, DeclarationEssentialsUi>(
        initItem = DeclarationEssentialsUi(),
        errorFactory = { item: DeclarationEssentialsUi ->
            buildList {
                if (item.displayName.isBlank()) add("Название обязательно")
                if (item.vendorId == null) add("Поставщик обязателен")
                if (item.bornDate == emptyDate) add("Дата создания обязательна")
                if (item.bestBefore == emptyDate) add("Дата истечения обязательна")
            }
        },
        mapperToUi = { toUi() }
    )

    private fun onChangeValueForMainTab(declaration: DeclarationEssentialsUi) {
        val title = "*Декларация ${declaration.displayName}"
        _model.update { MainTabComponent.NavTabState(title) }
    }

    private fun createChild(
        config: Config,
        componentContext: ComponentContext
    ): Child {
        return when (config) {
            is Config.Create -> Child.Create(
                CreateDeclarationSingleLineComponent(
                    componentContext = componentContext,
                    onSuccessCreate = { stackNavigation.replaceAll(Config.Update(it)) },
                    observeOnItem = { declaration -> onChangeValueForMainTab(declaration) },
                    componentFactory = componentFactory,
                    createDeclarationRepository = scope.get(),
                    immutableDependencies = scope.get(),
                    tabOpener = tabOpener
                )
            )
            is Config.Update -> Child.Update(
                DeclarationFormTabsComponent(
                    componentContext = componentContext,
                    scope = scope,
                    declarationId = config.id,
                    componentFactory = componentFactory,
                    tabOpener = tabOpener,
                    observeOnDeclaration = ::onChangeValueForMainTab
                )
            )
        }
    }

    internal val stack: Value<ChildStack<Config, Child>> = childStack(
        source = stackNavigation,
        serializer = Config.serializer(),
        initialConfiguration = if (declarationId == 0) Config.Create else Config.Update(
            declarationId
        ),
        handleBackButton = false,
        childFactory = ::createChild
    )

    @Serializable
    sealed interface Config {
        @Serializable
        data object Create : Config

        @Serializable
        data class Update(val id: Int) : Config
    }

    internal sealed class Child {
        class Create(val component: CreateDeclarationSingleLineComponent) : Child()
        class Update(val component: DeclarationFormTabsComponent) : Child()
    }
}
