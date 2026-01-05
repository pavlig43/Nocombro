package ru.pavlig43.product.internal.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.ArrowCircleUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import ru.pavlig43.coreui.tooltip.IconButtonToolTip
import ru.pavlig43.immutable.api.ui.MBSImmutableTable
import ru.pavlig43.mutable.api.ui.MutableTableBox
import ru.pavlig43.product.internal.component.tabs.tabslot.CompositionTabSlot
import ru.pavlig43.product.internal.data.CompositionUi
import ru.pavlig43.product.internal.data.ProductIngredientUi


@Composable
internal fun CompositionScreen(
    component: CompositionTabSlot,
) {

    val dialog by component.dialog.subscribeAsState()

    MutableTableBox(component)

    dialog.child?.instance?.also {
        MBSImmutableTable(it)
    }



}
@Suppress("LongParameterList")
@Composable
private fun CompositionBlock(
    compositionUi: CompositionUi,
    onChangeName: (composeKey: Int, name: String) -> Unit,
    removeComposition: (composeKey: Int) -> Unit,
    openDialog: () -> Unit,
    removeIngredientUi: (Int) -> Unit,
    onChangeGram: (composeKey: Int, gram: Int) -> Unit,
    openProduct: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var isShowIngredients by remember { mutableStateOf(false) }

    Column(
        modifier.fillMaxWidth().border(2.dp, MaterialTheme.colorScheme.primary).padding(8.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text("Название состава")

            OutlinedTextField(
                value = compositionUi.name,
                onValueChange = { onChangeName(compositionUi.composeKey, it) },
                singleLine = true,
                modifier = Modifier.weight(1f)

            )
            IconButtonToolTip(
                "Удалить состав",
                onClick = { removeComposition(compositionUi.composeKey) },
                icon = Icons.Default.Delete,
                tint = MaterialTheme.colorScheme.error
            )

        }
        IconButtonToolTip(
            tooltipText = if (isShowIngredients) "Скрыть ингредиенты" else "Показать ингредиенты",
            onClick = { isShowIngredients = !isShowIngredients },
            icon = if (isShowIngredients) Icons.Default.ArrowCircleUp else Icons.Default.ArrowCircleDown,
            tint = if (isShowIngredients) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSecondaryContainer
        )
        if (isShowIngredients) {
            IngredientsScreen(
                ingredients = compositionUi.productIngredients,
                openDialog = openDialog,
                removeIngredientUi = removeIngredientUi,
                onChangeGram = onChangeGram,
                openProduct = openProduct
            )

        }

    }
}
@Suppress("LongParameterList")
@Composable
private fun IngredientsScreen(
    ingredients: List<ProductIngredientUi>,
    openDialog: () -> Unit,
    removeIngredientUi: (Int) -> Unit,
    onChangeGram: (composeKey: Int, gram: Int) -> Unit,
    openProduct: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {


    Column(modifier) {
        Text("Ингредиенты на 1 кг")
        Row(
            Modifier.fillMaxWidth().padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {


            IconButtonToolTip(
                tooltipText = "Добавить ингредиент",
                onClick = openDialog,
                icon = Icons.Default.AddTask
            )
        }
        if (ingredients.isEmpty()) {
            Text("Необходимо добавить ингредиенты", color = MaterialTheme.colorScheme.error)
        }

        ingredients.forEach { ingredient ->
            AddIngredientRow(
                ingredientUi = ingredient,
                openProduct = openProduct,
                removeIngredientUi = removeIngredientUi,
                onChangeGram = onChangeGram
            )

        }
        @Suppress("MagicNumber")
        if (ingredients.sumOf { it.countGram } != 1000) {
            Text(
                "Сумма всех весов ингредиентов должна быть равна 1000",
                color = MaterialTheme.colorScheme.error
            )
        }
    }


}


@Composable
private fun AddIngredientRow(
    ingredientUi: ProductIngredientUi,
    onChangeGram: (composeKey: Int, gram: Int) -> Unit,
    openProduct: (Int) -> Unit,
    removeIngredientUi: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = ingredientUi.name,
                maxLines = 1,
                textDecoration = TextDecoration.Underline,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )

            OutlinedTextField(
                value = if (ingredientUi.countGram == 0) "" else ingredientUi.countGram.toString(),
                onValueChange = { value ->
                    if (value.all { it.isDigit() }) {
                        onChangeGram(ingredientUi.composeKey, value.toIntOrNull() ?: 0)
                    }
                },
                placeholder = { Text("0") },
                modifier = Modifier.width(100.dp),
                singleLine = true
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButtonToolTip(
                    onClick = { openProduct(ingredientUi.ingredientId) },
                    tooltipText = "Открыть в новой вкладке",
                    icon = Icons.Default.Search,
                    modifier = Modifier.size(36.dp)
                )

                IconButtonToolTip(
                    tooltipText = "Удалить",
                    onClick = { removeIngredientUi(ingredientUi.composeKey) },
                    icon = Icons.Default.Close
                )
            }
        }
    }

}