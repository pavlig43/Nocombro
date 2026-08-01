package ru.pavlig43.immutable.internal.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import ru.pavlig43.tablecore.export.TableExportConfiguration
import ru.pavlig43.tablecore.export.TableExportFormat
import ru.pavlig43.theme.Res
import ru.pavlig43.theme.add_circle
import ru.pavlig43.theme.close
import ru.pavlig43.theme.search

@Composable
@Suppress("LongParameterList")
internal fun ImmutableTableToolbar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onCreateClick: () -> Unit,
    showCreateAction: Boolean,
    exportConfiguration: TableExportConfiguration<*, *>?,
    exportEnabled: Boolean,
    onExportClick: (TableExportFormat) -> Unit,
    searchFocusRequester: FocusRequester,
    onSearchFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var exportMenuExpanded by rememberSaveable { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(Res.drawable.search),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                    },
                    trailingIcon = if (searchQuery.isNotEmpty()) {
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
                        .fillMaxWidth()
                        .focusRequester(searchFocusRequester)
                        .onFocusChanged { onSearchFocusChanged(it.isFocused) },
                )
            }

            if (showCreateAction) {
                Button(
                    onClick = onCreateClick,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.height(48.dp),
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.add_circle),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Создать")
                }
            }

            if (exportConfiguration != null) {
                Box {
                    OutlinedButton(
                        onClick = { exportMenuExpanded = true },
                        enabled = exportEnabled,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text("Экспорт")
                    }
                    DropdownMenu(
                        expanded = exportMenuExpanded,
                        onDismissRequest = { exportMenuExpanded = false },
                    ) {
                        TableExportFormat.entries
                            .filter { it in exportConfiguration.supportedFormats }
                            .forEach { format ->
                                DropdownMenuItem(
                                    text = { Text(format.label()) },
                                    onClick = {
                                        exportMenuExpanded = false
                                        onExportClick(format)
                                    },
                                )
                            }
                    }
                }
            }
        }
    }
}

private fun TableExportFormat.label(): String = when (this) {
    TableExportFormat.Excel -> "Excel (.xlsx)"
    TableExportFormat.Pdf -> "PDF"
    TableExportFormat.Word -> "Word"
}
