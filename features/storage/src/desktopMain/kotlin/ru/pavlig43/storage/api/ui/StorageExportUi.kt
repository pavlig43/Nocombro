package ru.pavlig43.storage.api.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.tablecore.export.ExcelColumn
import ru.pavlig43.tablecore.export.TableExportConfiguration
import ru.pavlig43.tablecore.export.TableExportFormat
import ru.pavlig43.tablecore.export.exportExcelFile
import ru.pavlig43.tablecore.export.formatValue
import ua.wwind.table.ColumnSpec
import ua.wwind.table.state.TableState

/**
 * Собирает плоское описание колонок для Excel из текущего состояния таблицы.
 *
 * Берет только видимые колонки и соблюдает фактический порядок `columnOrder`,
 * чтобы экспорт совпадал с тем, что пользователь видит на экране сейчас.
 */
@Composable
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
 * Рисует верхнюю панель действий таблицы с выпадающим меню форматов экспорта.
 *
 * Панель вынесена отдельно, чтобы `StorageScreen` и `BatchMovementTableScreen`
 * использовали одинаковый UI и не дублировали одну и ту же compose-разметку.
 */
@Composable
internal fun BoxScope.StorageExportActionBar(
    exportConfiguration: TableExportConfiguration<*, *>,
    isExportMenuExpanded: Boolean,
    onExpandExportMenu: () -> Unit,
    onDismissExportMenu: () -> Unit,
    onExportClick: (TableExportFormat) -> Unit,
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
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
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
