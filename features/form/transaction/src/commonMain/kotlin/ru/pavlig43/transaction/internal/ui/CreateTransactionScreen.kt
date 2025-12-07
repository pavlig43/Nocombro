package ru.pavlig43.transaction.internal.ui

import androidx.compose.runtime.Composable
import ru.pavlig43.create.ui.CreateEssentialsScreen
import ru.pavlig43.transaction.internal.component.CreateTransactionComponent

@Composable
internal fun CreateTransactionScreen(
    component: CreateTransactionComponent
) {
    CreateEssentialsScreen(component) { item, onItemChange ->
        TransactionFields(
            item,
            onItemChange
        )
    }
}
