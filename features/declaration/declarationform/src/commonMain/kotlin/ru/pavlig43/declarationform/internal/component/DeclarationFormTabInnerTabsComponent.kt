package ru.pavlig43.declarationform.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import com.arkivanov.decompose.value.Value
import com.arkivanov.decompose.value.operator.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import ru.pavlig43.core.RequestResult
import ru.pavlig43.core.data.dbSafeCall
import ru.pavlig43.core.tabs.DefaultTabNavigationComponent
import ru.pavlig43.core.tabs.ITabNavigationComponent
import ru.pavlig43.database.DataBaseTransaction
import ru.pavlig43.declarationform.internal.di.UpdateRepositoryType
import ru.pavlig43.form.api.component.IItemFormInnerTabsComponent
import ru.pavlig43.itemlist.api.component.refactoring.ItemListDependencies
import ru.pavlig43.upsertitem.api.component.IUpdateComponent
import ru.pavlig43.upsertitem.api.component.UpdateComponent

internal class DeclarationFormTabInnerTabsComponent(
    componentContext: ComponentContext,
    closeFormScreen:()->Unit,
    onOpenVendorTab: (Int) -> Unit,
    scope: Scope,
    declarationId: Int,
    onChangeValueForMainTab: (String) -> Unit
) : ComponentContext by componentContext, IItemFormInnerTabsComponent<DeclarationTab, DeclarationTabSlot> {

    private val _mainTabTitle = MutableStateFlow("")
    override val mainTabTitle: StateFlow<String> = _mainTabTitle.asStateFlow()
    private val dbTransaction:DataBaseTransaction = scope.get()


    override val tabNavigationComponent: ITabNavigationComponent<DeclarationTab, DeclarationTabSlot> =
        DefaultTabNavigationComponent(
            componentContext = childContext("tab"),
            startConfigurations = listOf(
                DeclarationTab.RequireValues,
                DeclarationTab.Files
            ),
            serializer = DeclarationTab.serializer(),
            slotFactory = { context, tabConfig: DeclarationTab, _: (DeclarationTab) -> Unit, _: () -> Unit ->
                when (tabConfig) {

                    DeclarationTab.RequireValues -> UpdateDeclarationTabSlot(
                        componentContext = context,
                        itemListDependencies = scope.get(),
                        declarationId = declarationId,
                        updateRepository = scope.get(named(UpdateRepositoryType.Declaration.name)),
                        onChangeValueForMainTab = onChangeValueForMainTab,
                        onOpenVendorTab = onOpenVendorTab,
                    )


                    DeclarationTab.Files -> DeclarationFileTabSlot(
                        declarationId = declarationId,
                        updateRepository = scope.get(),
                        componentContext = context
                    )


                }

            },
        )
    private suspend fun update():RequestResult<Unit> {
        val blocks: Value<List<suspend () -> Unit>> = tabNavigationComponent.children.map { children->
            children.items.map { child-> suspend {child.instance.onUpdate()} } }
        return dbSafeCall("document form"){
            dbTransaction.transaction(blocks.value)
        }
    }
    override val updateComponent: IUpdateComponent = UpdateComponent(
        componentContext = childContext("update"),
        onUpdateComponent = {update()},
        closeFormScreen = closeFormScreen
    )



}


