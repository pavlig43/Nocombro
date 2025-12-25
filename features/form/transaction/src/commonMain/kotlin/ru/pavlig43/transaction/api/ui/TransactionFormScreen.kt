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
import ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.BuyBaseProductFormSlot
import ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.BuyEssentialFormSlot
import ru.pavlig43.transaction.internal.component.tabs.tabslot.transactionvariables.buy.BuyFormSlot
import ru.pavlig43.transaction.internal.ui.CreateTransactionScreen
import ru.pavlig43.transaction.internal.ui.TransactionFields
import ru.pavlig43.update.ui.ItemTabsUi

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
                is TransactionFormComponent.Child.Update -> ItemTabsUi(
                    component = instance.component,
                    slotFactory = { slotForm ->
                        SlotScreen(slotForm)
                    })
            }
        }

    }

}

@Composable
private fun SlotScreen(
    slot: BuyFormSlot?,
) {
    when (slot) {
        is BuyEssentialFormSlot -> UpdateEssentialsBlock(slot)
        is BuyBaseProductFormSlot -> Box(Modifier)

        null -> Box(Modifier)

    }
}

@Composable
private fun UpdateEssentialsBlock(
    documentSlot: BuyEssentialFormSlot,
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