package ru.pavlig43.storage.api.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.tablecore.export.ExcelColumn
import ru.pavlig43.tablecore.export.TableExportConfiguration
import ru.pavlig43.tablecore.export.TableExportFormat
import ru.pavlig43.tablecore.export.exportExcelFile
import ru.pavlig43.tablecore.export.formatValue
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.close
import ru.pavlig43.theme.search
import ua.wwind.table.ColumnSpec
import ua.wwind.table.state.TableState

/**
 * Собирает плоское описание колонок для Excel из текущего состояния таблицы.
 *
 * Берет только видимые колонки и соблюдает фактический порядок `columnOrder`,
 * чтобы экспорт совпадал с тем, что пользователь видит на экране сейчас.
 */
@Composable
@Suppress("LoopWithTooManyJumpStatements", "UnreachableCode")
internal fun <T : Any, C, E> buildStorageExportColumns(
    columns: ImmutableList<ColumnSpec<T, C, E>>,
    tableState: TableState<C>,
    items: List<T>,
    exportConfiguration: TableExportConfiguration<T, C>,
): List<ExcelColumn> {
    val visibleByKey = columns.filter { it.visible }.associateBy { it.key }
    return buildList {
        for (key in tableState.columnOrder) {
            val spec = visibleByKey[key] ?: continue
            val title = spec.title?.invoke()?.takeIf { it.isNotBlank() } ?: continue
            add(
                ExcelColumn(
                    header = title,
                    values = items.map { item ->
                        exportConfiguration.formatValue(
                            column = key,
                            rawValue = spec.valueOf(item),
                            item = item,
                        )
                    },
                ),
            )
        }
    }
}

/**
 * Рисует верхнюю панель действий таблицы с поиском и меню форматов экспорта.
 *
 * Панель вынесена отдельно, чтобы `StorageScreen` и `BatchMovementTableScreen`
 * использовали одинаковый UI и не дублировали одну и ту же compose-разметку.
 * Поле поиска отображается только тогда, когда передан [searchQuery], поэтому
 * экран движения партий сохраняет прежний компактный вид панели.
 *
 * @param searchQuery текущий запрос или `null`, если поиск на экране не нужен.
 * @param onSearchQueryChange обработчик изменения и очистки поискового запроса.
 * @param searchFocusRequester запросчик фокуса для ввода сразу после печати символа.
 * @param onSearchFocusChanged обработчик состояния фокуса поискового поля.
 */
@Composable
@Suppress("LongParameterList")
internal fun BoxScope.StorageExportActionBar(
    exportConfiguration: TableExportConfiguration<*, *>,
    isExportMenuExpanded: Boolean,
    onExpandExportMenu: () -> Unit,
    onDismissExportMenu: () -> Unit,
    onExportClick: (TableExportFormat) -> Unit,
    searchQuery: String? = null,
    onSearchQueryChange: (String) -> Unit = {},
    searchFocusRequester: FocusRequester? = null,
    onSearchFocusChanged: (Boolean) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(18.dp),
        tonalElevation = 3.dp,
        shadowElevation = 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            searchQuery?.let { query ->
                OutlinedTextField(
                    value = query,
                    onValueChange = onSearchQueryChange,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = if (query.isNotEmpty()) {
                        {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    painter = painterResource(Res.drawable.close),
                                    contentDescription = "Очистить поиск",
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }
                    } else {
                        null
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .then(
                            searchFocusRequester?.let { focusRequester ->
                                Modifier
                                    .focusRequester(focusRequester)
                                    .onFocusChanged { onSearchFocusChanged(it.isFocused) }
                            } ?: Modifier
                        ),
                )
            }
            Box {
                FilledTonalButton(
                    onClick = onExpandExportMenu,
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Экспорт")
                }
                DropdownMenu(
                    expanded = isExportMenuExpanded,
                    onDismissRequest = onDismissExportMenu,
                ) {
                    TableExportFormat.entries.forEach { exportFormat ->
                        val exportEnabled = exportFormat in exportConfiguration.supportedFormats
                        DropdownMenuItem(
                            text = { Text(exportFormat.label()) },
                            onClick = {
                                onDismissExportMenu()
                                if (exportEnabled) onExportClick(exportFormat)
                            },
                            enabled = exportEnabled,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Запускает экспорт таблицы в выбранный формат.
 *
 * Сейчас реально поддержан только Excel, но диспетчер оставлен общим,
 * чтобы позже без перестройки UI подключить PDF и Word.
 */
@Suppress("RedundantSuspendModifier")
internal suspend fun runStorageExport(
    exportFormat: TableExportFormat,
    exportConfiguration: TableExportConfiguration<*, *>,
    exportColumns: List<ExcelColumn>,
): String? =
    when (exportFormat) {
        TableExportFormat.Excel -> {
            val result = exportExcelFile(
                suggestedFileName = exportConfiguration.suggestedFileName,
                columns = exportColumns,
            )
            result.exceptionOrNull()?.message ?: "Не удалось экспортировать таблицу в Excel."
                .takeIf { result.isFailure }
        }

        TableExportFormat.Pdf,
        TableExportFormat.Word -> null
    }

/**
 * Человекочитаемая подпись формата для `DropdownMenu`.
 */
private fun TableExportFormat.label(): String =
    when (this) {
        TableExportFormat.Excel -> "Excel (.xlsx)"
        TableExportFormat.Pdf -> "PDF"
        TableExportFormat.Word -> "Word"
    }
