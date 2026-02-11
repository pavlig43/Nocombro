package ru.pavlig43.declaration.internal.update

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import org.koin.core.scope.Scope
import ru.pavlig43.core.TransactionExecutor
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.core.tabs.TabOpener
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.declaration.internal.model.DeclarationEssentialsUi
import ru.pavlig43.declaration.internal.update.DeclarationTabChild.*
import ru.pavlig43.declaration.internal.update.tabs.component.DeclarationFilesComponent
import ru.pavlig43.declaration.internal.update.tabs.essential.DeclarationUpdateSingleLineComponent
import ru.pavlig43.mutable.api.singleLine.component.SingleLineComponentFactory
import ru.pavlig43.update.component.IItemFormTabsComponent
import ru.pavlig43.update.component.getDefaultUpdateComponent

internal class DeclarationFormTabsComponent(
    componentContext: ComponentContext,
    componentFactory: SingleLineComponentFactory<Declaration, DeclarationEssentialsUi>,
    closeFormScreen: () -> Unit,
    scope: Scope,
    declarationId: Int,
    observeOnDeclaration: (DeclarationEssentialsUi) -> Unit,
    private val tabOpener: TabOpener,
) : ComponentContext by componentContext,
    IItemFormTabsComponent<DeclarationTab, DeclarationTabChild> {

    override val transactionExecutor: TransactionExecutor = scope.get()



    override val tabNavigationComponent: TabNavigationComponent<DeclarationTab, DeclarationTabChild> =
        TabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                DeclarationTab.Essential,
                DeclarationTab.Files
            ),
            serializer = DeclarationTab.serializer(),
            tabChildFactory = { context, tabConfig: DeclarationTab, _: () -> Unit ->
                when (tabConfig) {

                    DeclarationTab.Files -> File(
                        DeclarationFilesComponent(
                            declarationId = declarationId,
                            dependencies = scope.get(),
                            componentContext = context
                        )
                    )

                    DeclarationTab.Essential -> Essential(
                        DeclarationUpdateSingleLineComponent(
                            componentContext = context,
                            declarationId = declarationId,
                            updateRepository = scope.get(),
                            observeOnItem = observeOnDeclaration,
                            componentFactory = componentFactory,
                            immutableDependencies = scope.get(),
                            tabOpener = tabOpener
                        )
                    )
                }

            },
        )


    override val updateComponent = getDefaultUpdateComponent(componentContext, closeFormScreen)

}
