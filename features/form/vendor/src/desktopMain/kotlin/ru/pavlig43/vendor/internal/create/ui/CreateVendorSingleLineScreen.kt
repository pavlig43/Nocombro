package ru.pavlig43.vendor.internal.create.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.pavlig43.mutable.api.singleLine.ui.CreateSingleItemScreen
import ru.pavlig43.vendor.internal.VendorEssentialsCards
import ru.pavlig43.vendor.internal.create.component.CreateVendorSingleLineComponent

/**
 * Показывает карточную форму создания поставщика.
 *
 * @param component компонент создания поставщика
 */
@Composable
internal fun CreateVendorSingleLineScreen(
    component: CreateVendorSingleLineComponent,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
    ) {
        CreateSingleItemScreen(
            component = component,
            itemContent = { modifier ->
                VendorEssentialsCards(
                    component = component,
                    modifier = modifier,
                )
            },
        )
    }
}
