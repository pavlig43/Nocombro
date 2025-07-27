package ru.pavlig43.productform.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.manageitem.api.ui.ManageBaseValuesOfItem
import ru.pavlig43.productform.api.component.IProductFormComponent
import ru.pavlig43.upsertitem.api.ui.SaveScreenState

@Composable
fun ProductFormScreen(
    component: IProductFormComponent,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = modifier
            .padding(horizontal = 8.dp)
            .verticalScroll(scrollState)
    ) {

        ManageBaseValuesOfItem(component = component.manageBaseValuesOfComponent)
        SaveScreenState(component = component.saveProductComponent)

    }


}