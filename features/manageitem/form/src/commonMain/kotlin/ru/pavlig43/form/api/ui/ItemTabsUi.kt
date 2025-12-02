package ru.pavlig43.form.api.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.FormTabSlot
import ru.pavlig43.coreui.tab.TabNavigationContent
import ru.pavlig43.form.api.component.IItemFormInnerTabsComponent
import ru.pavlig43.upsertitem.api.ui.UpdateButton


@Composable
fun <Tab : Any, Slot : FormTabSlot> ItemTabsUi(
    component: IItemFormInnerTabsComponent<Tab, Slot>,
    slotFactory: @Composable (Slot?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
        TabNavigationContent(
            navigationComponent = component.tabNavigationComponent,
            tabContent = { index, tabComponent, modifier, isSelected, _, _ ->
                TabContent(
                    formTabSlot = tabComponent,
                    modifier = modifier,
                    isSelected = isSelected,
                    onSelect = { component.tabNavigationComponent.onSelectTab(index) },
                )
            },
            containerContent = { innerTabs: @Composable (modifier: Modifier) -> Unit,
                                 formSlot: Slot? ->
                innerTabs(Modifier.fillMaxWidth())
                slotFactory(formSlot)
            }
        )
        UpdateButton(component.updateComponent)
    }
}


val tabSize = DpSize(228.dp, 36.dp)

/**
 * Computations
 */
val tabModifier = Modifier.size(tabSize).padding(8.dp)
val tabBorder = @Composable { isSelected: Boolean ->
    if (isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.onBackground)
    } else {
        CardDefaults.outlinedCardBorder()
    }
}
val tabOnClickModifier =
    @Composable { onSelect: () -> Unit, interactionSource: MutableInteractionSource ->
        Modifier.clickable(
            onClick = onSelect,
            interactionSource = interactionSource,
            indication = null
        )
    }


@Composable
private fun TabContent(
    formTabSlot: FormTabSlot,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier

) {
    val interactionSource = remember(formTabSlot) { MutableInteractionSource() }
    val pressedAsState = interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressedAsState.value) {
        if (pressedAsState.value) {
            onSelect()
        }
    }

    OutlinedCard(
        modifier = modifier.then(tabOnClickModifier(onSelect, interactionSource)),
        colors = CardDefaults.outlinedCardColors().copy(),
        border = tabBorder(isSelected)
    ) {
        Row(
            modifier = Modifier.then(tabModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = formTabSlot.title,
                textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None
            )
        }
    }
}