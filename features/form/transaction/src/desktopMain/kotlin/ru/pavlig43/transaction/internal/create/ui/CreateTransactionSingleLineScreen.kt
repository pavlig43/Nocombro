package ru.pavlig43.transaction.internal.create.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.datetime.single.datetime.DateTimePickerDialog
import ru.pavlig43.mutable.api.singleLine.ui.CreateSingleItemScreen
import ru.pavlig43.transaction.internal.TransactionEssentialsCards
import ru.pavlig43.transaction.internal.create.component.CreateTransactionSingleLineComponent
import ru.pavlig43.transaction.internal.create.component.DialogChild

/** Показывает карточную форму создания транзакции. */
@Composable
internal fun CreateTransactionSingleLineScreen(
    component: CreateTransactionSingleLineComponent,
) {
    val dialog by component.dialog.subscribeAsState()

    CreateSingleItemScreen(
        component = component,
        itemContent = { modifier ->
            TransactionEssentialsCards(
                component = component,
                onOpenCreatedAtDialog = component::onOpenCreatedAtDialog,
                modifier = modifier,
            )
        },
    )

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.DateTime -> DateTimePickerDialog(dialogChild.component)
        }
    }
}
