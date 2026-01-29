package ru.pavlig43.rootnocombro.internal.navigation.tab.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.core.MainTabComponent
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.close

/**
 * Properties
 */
val tabShape = RoundedCornerShape(12.dp)
val tabSize = DpSize(248.dp, 36.dp)
val closeButtonSize = 16.dp
val closeButtonIconSize = 12.dp

/**
 * Computations
 */
val tabModifier = Modifier.size(tabSize).padding(4.dp)
val closeButtonModifier = Modifier.size(closeButtonSize)
val closeButtonIconModifier = Modifier.size(closeButtonIconSize)
val tabBorder = @Composable { isSelected: Boolean, isDragging: Boolean ->
    if (isDragging || isSelected) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        CardDefaults.outlinedCardBorder()
    }
}
val tabOnClickModifier = @Composable { onSelect: () -> Unit, interactionSource: MutableInteractionSource ->
    Modifier.clickable(
        onClick = onSelect,
        interactionSource = interactionSource,
        indication = null
    )
}


@Suppress("LongParameterList")
@Composable
internal fun TabContent(
    mainTabComponent: MainTabComponent,
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    isDragging: Boolean,
    onClose: () -> Unit,
    onSelect: () -> Unit
) {

    val state by mainTabComponent.model.collectAsState()
    val interactionSource = remember(mainTabComponent) { MutableInteractionSource() }
    val pressedAsState = interactionSource.collectIsPressedAsState()
    LaunchedEffect(pressedAsState.value) {
        if (pressedAsState.value) {
            onSelect()
        }
    }

    OutlinedCard(
        modifier = modifier.then(tabOnClickModifier(onSelect, interactionSource)),
        shape = tabShape,
        border = tabBorder(isSelected, isDragging)
    ) {
        Row(
            modifier = Modifier.then(tabModifier),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = state.title,Modifier.weight(1f))
            FilledTonalIconButton(modifier = closeButtonModifier, onClick = onClose) {
                Icon(
                    modifier = closeButtonIconModifier,
                    painter = painterResource( Res.drawable.close),
                    contentDescription = null
                )
            }
        }
    }
}