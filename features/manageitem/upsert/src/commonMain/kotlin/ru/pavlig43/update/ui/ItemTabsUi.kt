package ru.pavlig43.update.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import ru.pavlig43.core.FormTabChild
import ru.pavlig43.core.FormTabComponent
import ru.pavlig43.coreui.tab.TabNavigationContent
import ru.pavlig43.update.component.IItemFormInnerTabsComponent


@Composable
fun <Tab : Any, Child : FormTabChild> ItemTabsUi1(
    component: IItemFormInnerTabsComponent<Tab, Child>,
    slotFactory: @Composable (Child?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        bottomBar = {
            UpdateButton(component.updateComponent)
        },

        ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            TabNavigationContent(
                navigationComponent = component.tabNavigationComponent,
                tabContent = { index, tabChild, modifier, isSelected, _, _ ->
                    TabContent(
                        formTabComponent = tabChild.component,
                        modifier = modifier,
                        isSelected = isSelected,
                        onSelect = { component.tabNavigationComponent.onSelectTab(index) },
                    )
                },
                tabsRowModifier = Modifier.fillMaxSize(),
                tabChildFactory = slotFactory,
            )
        }
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
    formTabComponent: FormTabComponent,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier

) {
    val interactionSource = remember(formTabComponent) { MutableInteractionSource() }
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
                text = formTabComponent.title,
                textDecoration = if (isSelected) TextDecoration.Underline else TextDecoration.None
            )
        }
    }
}