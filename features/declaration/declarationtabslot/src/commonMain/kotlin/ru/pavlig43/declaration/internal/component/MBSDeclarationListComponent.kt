package ru.pavlig43.declaration.internal.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.declarationlist.api.component.DeclarationListComponent
import ru.pavlig43.declarationlist.internal.data.DeclarationItemUi
import ru.pavlig43.declarationlist.internal.data.DeclarationListRepository

class MBSDeclarationListComponent(
    private val componentContext: ComponentContext,
    onItemClick: (DeclarationItemUi) -> Unit,
    onCreate: () -> Unit,
    repository: DeclarationListRepository,
    private val onDismissed: () -> Unit,
) : ComponentContext by componentContext {

    val declarationList = DeclarationListComponent(
        componentContext = childContext("itemList"),
        tabTitle = "",
        onCreate = onCreate,
        onItemClick = onItemClick,
        withCheckbox = false,
        repository = repository,
    )

    fun onDismissClicked() {
        onDismissed()
    }

}