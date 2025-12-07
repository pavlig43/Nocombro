package ru.pavlig43.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.core.component.EssentialsComponent
import ru.pavlig43.core.data.GenericItem
import ru.pavlig43.core.data.ItemEssentialsUi
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen

@Composable
fun <I : ItemEssentialsUi> EssentialBlockScreen(
    component: EssentialsComponent<out GenericItem, I>,
    modifier: Modifier = Modifier,
    fieldsBody: @Composable (item: I, updateItem: (I) -> Unit) -> Unit
) {

    Column(modifier) {
        val item by component.itemFields.collectAsState()
        LoadInitDataScreen(component.initDataComponent) {

            fieldsBody(item, component::onChangeItem)
        }
    }

}




