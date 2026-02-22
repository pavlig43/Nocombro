package ru.pavlig43.transaction.internal.update.tabs.component.opzs.ingredients

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.coreui.ErrorScreen
import ru.pavlig43.coreui.LoadingUi
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.multiLine.ui.MutableTableBox
import ua.wwind.table.config.TableRowStyle
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.rememberCustomization

@Composable
internal fun IngredientScreen(
    component: IngredientComponent
) {

    val rules = remember {
        listOf(
            TableFormatRule.new<IngredientField, Unit>(
                id = 1, filter = Unit
            )
        ).toImmutableList()
    }

    val customization = rememberCustomization<IngredientUi, IngredientField, Unit>(
        rules = rules,
        matches = { item, filter ->  item.productType in listOf(ProductType.FOOD_BASE, ProductType.FOOD_PF)},
        baseRowStyle = { row ->
            if (row.item.productType in listOf(ProductType.FOOD_BASE, ProductType.FOOD_PF)) {
                TableRowStyle(
                    modifier = androidx.compose.ui.Modifier,
                    contentColor = MaterialTheme.colorScheme.primaryContainer,
                    elevation = 2.dp,
                    shape = RoundedCornerShape(4.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                )
            } else TableRowStyle()
        }
    )
    val dialog by component.dialog.subscribeAsState()
    val enabledFillButton by component.enabledFillButton.collectAsState()
    val loadCompositionState by component.loadCompositionState.collectAsState()

    Column {
        FillButton(
            enabled = enabledFillButton,
            onClick = component::fillFromPf
        )

        when (loadCompositionState) {
            is LoadCompositionState.Loading -> LoadingUi()
            is LoadCompositionState.Error -> ErrorScreen(
                message = (loadCompositionState as LoadCompositionState.Error).message
            )
            is LoadCompositionState.Success -> Unit
        }

        MutableTableBox(
            component = component,
            tableSettingsModify = { ts -> ts.copy(
                stripedRows = false,
                showFooter = true) },
            tableCustomization = customization
        )
    }

    dialog.child?.instance?.also { dialogChild ->
        when (dialogChild) {
            is DialogChild.ImmutableMBS -> MBSImmutableTable(dialogChild.component)
        }
    }
}

@Composable
private fun FillButton(
    enabled: Boolean,
    onClick: () -> Unit) {
    Row(
        modifier = Modifier.padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text("Заполнить по ПФ")
        }
    }
}
