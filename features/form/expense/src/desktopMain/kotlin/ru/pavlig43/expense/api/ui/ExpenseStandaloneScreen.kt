package ru.pavlig43.expense.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.expense.api.component.ExpenseFormComponent
import ru.pavlig43.expense.internal.update.ExpenseTabChild
import ru.pavlig43.expense.internal.create.ui.CreateExpenseSingleLineScreen
import ru.pavlig43.expense.internal.update.tabs.essential.UpdateExpenseSingleLineScreen
import ru.pavlig43.files.api.ui.FilesScreen
import ru.pavlig43.update.ui.FormTabsUi

@Composable
fun ExpenseStandaloneScreen(
    component: ExpenseFormComponent,
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
                is ExpenseFormComponent.Child.Create -> CreateExpenseSingleLineScreen(instance.component)
                is ExpenseFormComponent.Child.Update -> FormTabsUi(
                    component = instance.component,
                    tabChildFactory = { child: ExpenseTabChild? ->
                        ExpenseFormTabScreen(child)
                    }
                )
            }
        }
    }
}

@Composable
private fun ExpenseFormTabScreen(
    expenseChild: ExpenseTabChild?,
) {
    when (expenseChild) {
        is ExpenseTabChild.Essentials -> UpdateExpenseSingleLineScreen(expenseChild.component)
        is ExpenseTabChild.Files -> FilesScreen(expenseChild.component)
        null -> Box(Modifier.fillMaxSize()) { Text("Пусто") }
    }
}

