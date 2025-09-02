package ru.pavlig43.declaration.api.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.childContext
import ru.pavlig43.database.data.document.Document
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.api.component.IItemListComponent
import ru.pavlig43.itemlist.api.component.ItemListComponent
import ru.pavlig43.itemlist.api.data.ItemListRepository

interface MBSComponent {
    val documentList:IItemListComponent

    fun onDismissClicked()
}

class DefaultMBSComponent(
    private val componentContext: ComponentContext,
    onItemClick: (Int,String) -> Unit,
    repository: ItemListRepository<Document,DocumentType>,
    onCreate: () -> Unit,
    private val onDismissed: () -> Unit,
) : MBSComponent, ComponentContext by componentContext {

    override val documentList = ItemListComponent<Document,DocumentType>(
        componentContext = childContext("documentList"),
        fullListSelection = listOf(DocumentType.Declaration),
        tabTitle = "",
        onCreate = onCreate,
        repository = repository,
        onItemClick = onItemClick,
        withCheckbox = false
    )

    override fun onDismissClicked() {
        onDismissed()
    }

}



