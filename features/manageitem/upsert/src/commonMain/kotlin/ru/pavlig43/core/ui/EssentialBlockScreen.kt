package ru.pavlig43.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen
import ru.pavlig43.core.component.EssentialsComponent
import ru.pavlig43.core.data.ItemEssentialsUi

@Composable
fun <I: ItemEssentialsUi> EssentialBlockScreen(
    component: EssentialsComponent<out GenericItem, I>,
    fieldsBody: @Composable (item: I, updateItem: (I) -> Unit) -> Unit
) {
    val item by component.itemFields.collectAsState()
    LoadInitDataScreen(component.initDataComponent) {
        fieldsBody(item, component::onChangeItem)
    }
}




