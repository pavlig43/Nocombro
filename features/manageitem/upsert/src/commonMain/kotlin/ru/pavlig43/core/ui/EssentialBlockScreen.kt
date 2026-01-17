package ru.pavlig43.core.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import ru.pavlig43.core.component.EssentialsComponent
import ru.pavlig43.core.model.GenericItem
import ru.pavlig43.core.model.ItemEssentialsUi
import ru.pavlig43.loadinitdata.api.ui.LoadInitDataScreen

/**
 * @param [fieldsBody] Набор полей, который на вход принимает
 * 1)сам объект(например документ), чтобы из него получить свойство(например имя).
 * 2)колбэк, который возвращает
 * измененный предмет(чаще всего объект это дата класс, а колбэк его метод copy)
 */
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




