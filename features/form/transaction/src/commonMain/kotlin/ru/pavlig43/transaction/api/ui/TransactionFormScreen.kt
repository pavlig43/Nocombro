package ru.pavlig43.transaction.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.transaction.api.component.TransactionFormComponent
import ru.pavlig43.transaction.internal.create.ui.CreateTransactionSingleLineScreen
import ru.pavlig43.transaction.internal.update.TransactionTabChild
import ru.pavlig43.transaction.internal.update.tabs.component.buy.BuyScreen
import ru.pavlig43.transaction.internal.update.tabs.component.expenses.ExpensesScreen
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.pf.PfScreen
import ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingridients.IngredientScreen
import ru.pavlig43.transaction.internal.update.tabs.component.reminders.RemindersScreen
import ru.pavlig43.transaction.internal.update.tabs.essential.UpdateTransactionSingleLineScreen
import ru.pavlig43.update.ui.FormTabsUi

@Composable
fun TransactionFormScreen(
    component: TransactionFormComponent,
    modifier: Modifier = Modifier,
) {

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,

        modifier = modifier
            .padding(horizontal = 8.dp)
    ) {
        val stack by component.stack.subscribeAsState()
        Children(
            stack = stack,
        ) { child ->
            when (val instance = child.instance) {
                is TransactionFormComponent.Child.Create -> CreateTransactionSingleLineScreen(instance.component)
                is TransactionFormComponent.Child.Update -> FormTabsUi(
                    component = instance.component,
                    tabChildFactory = { tabChild ->
                        TransactionSlotScreen(tabChild)
                    })
            }
        }

    }

}

@Composable
private fun TransactionSlotScreen(
    child: TransactionTabChild?,
) {
    when (child) {
        is TransactionTabChild.Essentials -> UpdateTransactionSingleLineScreen(child.component)
        is TransactionTabChild.Buy -> BuyScreen(child.component)
        is TransactionTabChild.Reminders -> RemindersScreen(child.component)
        is TransactionTabChild.Expenses -> ExpensesScreen(child.component)
        is TransactionTabChild.Pf -> PfScreen(child.component)
        is TransactionTabChild.Ingredients -> IngredientScreen(child.component)

        null -> Box(Modifier)

    }
}
