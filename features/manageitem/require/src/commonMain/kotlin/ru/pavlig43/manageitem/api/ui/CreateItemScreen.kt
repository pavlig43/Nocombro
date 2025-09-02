package ru.pavlig43.manageitem.api.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import ru.pavlig43.core.data.Item
import ru.pavlig43.core.data.ItemType
import ru.pavlig43.manageitem.api.component.CreateItemComponent
import ru.pavlig43.upsertitem.api.ui.CreateStateScreen

@Composable
fun <I: Item,S: ItemType> CreateScreen(
    createItemComponent:CreateItemComponent<I,S>,
    modifier: Modifier = Modifier
){
    Column(modifier.fillMaxSize()) {
        RequireValuesScreen(createItemComponent.requires)
        CreateStateScreen(createItemComponent.createComponent)
    }

}