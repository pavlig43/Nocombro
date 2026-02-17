package ru.pavlig43.product.internal.update.tabs.composition

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.database.data.product.ProductType
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.multiLine.ui.MutableTableBox
import ua.wwind.table.config.TableRowStyle
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.format.rememberCustomization


@Composable
internal fun CompositionScreen(
    component: CompositionComponent,
) {

    val dialog by component.dialog.subscribeAsState()

    val rules = remember {
        listOf(
            TableFormatRule.new<CompositionField, Unit>(
                id = 1, filter = Unit
            )
        ).toImmutableList()
    }

    val customization = rememberCustomization<CompositionUi, CompositionField, Unit>(
        rules = rules,
        matches = { item, filter ->  item.productType in listOf(ProductType.FOOD_BASE, ProductType.FOOD_PF)},
        baseRowStyle = { row ->
            if (row.item.productType in listOf(ProductType.FOOD_BASE, ProductType.FOOD_PF)) {
                TableRowStyle(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            }
            else TableRowStyle()


        }
    )


    MutableTableBox(
        component = component,
        tableSettingsModify = { it.copy(stripedRows = false) },
        tableCustomization = customization
    )

    dialog.child?.instance?.also {
        MBSImmutableTable(it)
    }


}
