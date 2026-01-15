package ru.pavlig43.declaration.internal.component.tabs

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.operator.map
import org.koin.core.scope.Scope
import ru.pavlig43.core.component.EssentialComponentFactory
import ru.pavlig43.core.tabs.TabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.database.data.declaration.Declaration
import ru.pavlig43.declaration.internal.component.tabs.tabslot.DeclarationTabChild
import ru.pavlig43.declaration.internal.component.tabs.tabslot.DeclarationEssentialComponent
import ru.pavlig43.declaration.internal.component.tabs.tabslot.DeclarationFilesComponent
import ru.pavlig43.declaration.internal.data.DeclarationEssentialsUi
import ru.pavlig43.update.component.IItemFormInnerTabsComponent
import ru.pavlig43.update.component.UpdateComponent

internal class DeclarationFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    componentFactory: EssentialComponentFactory<Declaration, DeclarationEssentialsUi>,
    closeFormScreen: () -> Unit,
    onOpenVendorTab: (Int) -> Unit,
    scope: Scope,
    declarationId: Int
) : ComponentContext by componentContext,
    IItemFormInnerTabsComponent<DeclarationTab, DeclarationTabChild> {

    private val dbTransaction: DataBaseTransaction = scope.get()


    override val tabNavigationComponent: TabNavigationComponent<DeclarationTab, DeclarationTabChild> =
        TabNavigationComponent<DeclarationTab, DeclarationTabChild>(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                DeclarationTab.Essentials,
                DeclarationTab.Files
            ),
            serializer = DeclarationTab.serializer(),
            tabChildFactory = { context, tabConfig: DeclarationTab, _: () -> Unit ->
                when (tabConfig) {
                    DeclarationTab.Essentials -> DeclarationTabChild.Essential(

                        DeclarationEssentialComponent(
                            componentContext = context,
                            dependencies = scope.get(),
                            declarationId = declarationId,
                            updateRepository = scope.get(),
                            componentFactory = componentFactory,
                            onOpenVendorTab = onOpenVendorTab
                        )
                    )

                    DeclarationTab.Files -> DeclarationTabChild.File(
                        DeclarationFilesComponent(
                            declarationId = declarationId,
                            dependencies = scope.get(),
                            componentContext = context
                        )
                    )
//                    DeclarationTab.Essentials -> EssentialComponent(
//                        componentContext = context,
//                        dependencies = scope.get(),
//                        declarationId = declarationId,
//                        updateRepository = scope.get(),
//                        componentFactory = componentFactory,
//                        onOpenVendorTab = onOpenVendorTab
//                    )


//                    DeclarationTab.Files -> FileComponent(
//                        declarationId = declarationId,
//                        dependencies = scope.get(),
//                        componentContext = context
//                    )


                }

            },
        )

    private suspend fun update(): Result<Unit> {
        val blocks =
            tabNavigationComponent.tabChildren.map { children: TabNavigationComponent.TabChildren<DeclarationTab, DeclarationTabChild> ->
                children.items.map { child -> suspend { child.instance.component.onUpdate() } }
            }
        return dbTransaction.transaction(blocks.value)
    }

    override val updateComponent: UpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = { update() },
        errorMessages = getErrors(lifecycle),
        closeFormScreen = closeFormScreen
    )


}