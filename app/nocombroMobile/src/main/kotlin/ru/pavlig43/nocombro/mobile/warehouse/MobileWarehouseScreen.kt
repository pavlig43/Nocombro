package ru.pavlig43.nocombro.mobile.warehouse

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.search

@Composable
fun MobileWarehouseRoute(
    component: MobileWarehouseComponent,
    onOpenMenu: () -> Unit,
) {
    val state by component.uiState.collectAsState()
    MobileWarehouseScreen(
        state = state,
        onOpenMenu = onOpenMenu,
        onLocationSelected = component::selectLocation,
        onSearchQueryChanged = component::updateSearchQuery,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MobileWarehouseScreen(
    state: MobileWarehouseUiState,
    onOpenMenu: () -> Unit,
    onLocationSelected: (MobileWarehouseLocation) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .testTag("warehouse_scaffold"),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Склад") },
                navigationIcon = {
                    IconButton(onClick = onOpenMenu) {
                        Text(
                            text = "←",
                            style = MaterialTheme.typography.headlineMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            state.updatedAt?.let { updatedAt ->
                Text(
                    text = formatWarehouseUpdatedAt(updatedAt),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            WarehouseLocationSwitcher(
                selected = state.selectedLocation,
                onSelected = onLocationSelected,
            )
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск товара") },
                leadingIcon = {
                    Icon(
                        painter = painterResource(Res.drawable.search),
                        contentDescription = null,
                    )
                },
                singleLine = true,
            )
            when {
                !state.hasSnapshot -> WarehouseEmptyMessage(
                    "Данных склада пока нет. Выполните получение или синхронизацию в главном меню"
                )
                state.products.isEmpty() -> WarehouseEmptyMessage("Ничего не найдено")
                else -> WarehouseProductList(state)
            }
        }
    }
}

@Composable
private fun WarehouseLocationSwitcher(
    selected: MobileWarehouseLocation,
    onSelected: (MobileWarehouseLocation) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        WarehouseLocationButton(
            label = "Основной",
            selected = selected == MobileWarehouseLocation.MAIN,
            onClick = { onSelected(MobileWarehouseLocation.MAIN) },
            modifier = Modifier.weight(1f),
        )
        WarehouseLocationButton(
            label = "Экспериментальный",
            selected = selected == MobileWarehouseLocation.EXPERIMENTAL,
            onClick = { onSelected(MobileWarehouseLocation.EXPERIMENTAL) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun WarehouseLocationButton(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    if (selected) {
        Button(onClick = onClick, modifier = modifier) { Text(label) }
    } else {
        OutlinedButton(onClick = onClick, modifier = modifier) { Text(label) }
    }
}

@Composable
private fun WarehouseProductList(state: MobileWarehouseUiState) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("warehouse_product_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(
            items = state.products,
            key = MobileWarehouseUiProduct::productSyncId,
        ) { product ->
            WarehouseProductCard(product, state.searchQuery)
        }
    }
}

@Composable
private fun WarehouseProductCard(
    product: MobileWarehouseUiProduct,
    searchQuery: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            HighlightedWarehouseText(
                text = product.displayName,
                query = searchQuery,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                baseColor = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(20.dp))
            HighlightedWarehouseText(
                text = product.formattedBalance,
                query = searchQuery,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                baseColor = if (product.balance < 0L) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun HighlightedWarehouseText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    maxLines: Int,
    overflow: TextOverflow,
    baseColor: Color,
) {
    val highlightedText = buildWarehouseHighlightedText(
        text = text,
        query = query,
        background = MaterialTheme.colorScheme.primaryContainer,
        foreground = MaterialTheme.colorScheme.onPrimaryContainer,
    )
    Text(
        text = highlightedText,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow,
        color = baseColor,
        style = MaterialTheme.typography.bodyLarge,
    )
}

internal fun buildWarehouseHighlightedText(
    text: String,
    query: String,
    background: Color,
    foreground: Color,
): AnnotatedString = buildAnnotatedString {
    append(text)
    findMobileWarehouseSearchMatches(text, query).forEach { range ->
        addStyle(
            style = SpanStyle(background = background, color = foreground),
            start = range.first,
            end = range.last + 1,
        )
    }
}

@Composable
private fun WarehouseEmptyMessage(text: String) {
    Text(
        text = text,
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

internal fun formatWarehouseUpdatedAt(value: LocalDateTime): String {
    val month = RUSSIAN_MONTHS[value.month.ordinal]
    val hour = value.hour.toString().padStart(2, '0')
    val minute = value.minute.toString().padStart(2, '0')
    return "Обновлено ${value.day} $month, $hour:$minute"
}

private val RUSSIAN_MONTHS = listOf(
    "января",
    "февраля",
    "марта",
    "апреля",
    "мая",
    "июня",
    "июля",
    "августа",
    "сентября",
    "октября",
    "ноября",
    "декабря",
)
