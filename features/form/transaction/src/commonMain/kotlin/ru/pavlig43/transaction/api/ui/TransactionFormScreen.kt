package ru.pavlig43.transaction.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.core.ui.EssentialBlockScreen
import ru.pavlig43.transaction.api.component.TransactionFormComponent
import ru.pavlig43.transaction.internal.component.tabs.TransactionTabChild
import ru.pavlig43.transaction.internal.component.tabs.component.TransactionEssentialComponent
import ru.pavlig43.transaction.internal.component.tabs.component.buy.BuyScreen
import ru.pavlig43.transaction.internal.component.tabs.component.reminders.RemindersScreen
import ru.pavlig43.transaction.internal.ui.CreateTransactionScreen
import ru.pavlig43.transaction.internal.ui.TransactionFields
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
                is TransactionFormComponent.Child.Create -> CreateTransactionScreen(instance.component)
                is TransactionFormComponent.Child.Update -> FormTabsUi(
                    component = instance.component,
                    tabChildFactory = { tabChild ->
                        TabsScreen(tabChild)
                    })
            }
        }

    }

}

@Composable
private fun TabsScreen(
    child: TransactionTabChild?,
) {
    when (child) {
        is TransactionTabChild.Essentials -> UpdateEssentialsBlock(child.component)
        is TransactionTabChild.Buy -> BuyScreen(child.component)
        is TransactionTabChild.Reminders -> RemindersScreen(child.component)

        null -> Box(Modifier)

    }
}

@Composable
private fun UpdateEssentialsBlock(
    documentSlot: TransactionEssentialComponent,
    modifier: Modifier = Modifier
) {
    Column(modifier.verticalScroll(rememberScrollState())) {
        EssentialBlockScreen(documentSlot) { item, onItemChange ->
            TransactionFields(
                item,
                onItemChange
            )
        }
    }
}