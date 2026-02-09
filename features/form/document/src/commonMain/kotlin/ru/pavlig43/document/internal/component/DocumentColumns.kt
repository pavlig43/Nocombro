@file:Suppress("MatchingDeclarationName")

package ru.pavlig43.document.internal.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ru.pavlig43.coreui.DateRow
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.document.internal.data.DocumentEssentialsUi
import ua.wwind.table.ColumnSpec
import ua.wwind.table.component.TableCellTextField
import ua.wwind.table.editableTableColumns

/**
 * Создаёт колонки для таблицы создания/редактирования документа
 *
 * @param onOpenDateDialog Callback для открытия диалога выбора даты. Принимает composeId строки
 * @param onChangeItem Callback для обновления данных документа
 */
@Suppress("LongMethod")
internal fun createDocumentColumns(
    onOpenDateDialog: (Int) -> Unit,
    onChangeItem: (DocumentEssentialsUi) -> Unit
): ImmutableList<ColumnSpec<DocumentEssentialsUi, DocumentField, Unit>> {
    val columns =
        editableTableColumns<DocumentEssentialsUi, DocumentField, Unit> {

            // Название документа
            column(DocumentField.DISPLAY_NAME, valueOf = { it.displayName }) {
                header("Название")
                align(Alignment.Center)
                cell { item, _ ->
                    TableCellTextField(
                        value = item.displayName,
                        onValueChange = { newValue ->
                            onChangeItem(item.copy(displayName = newValue))
                        }
                    )
                }
            }

            // Тип документа - dropdown
            column(DocumentField.TYPE, valueOf = { it.type }) {
                header("Тип")
                align(Alignment.Center)
                cell { item, _ ->
                    DocumentTypeDropdown(
                        currentType = item.type,
                        onTypeSelected = { type ->
                            onChangeItem(item.copy(type = type))
                        }
                    )
                }
            }

            // Дата создания
            column(DocumentField.CREATED_AT, valueOf = { it.createdAt }) {
                header("Дата создания")
                align(Alignment.Center)
                cell { item, _ ->
                    DateRow(
                        date = item.createdAt,
                        isChangeDialogVisible = { onOpenDateDialog(item.composeId) }
                    )
                }
            }

            // Комментарий
            column(DocumentField.COMMENT, valueOf = { it.comment }) {
                header("Комментарий")
                align(Alignment.Center)
                cell { item, _ ->
                    TableCellTextField(
                        value = item.comment,
                        onValueChange = { newValue ->
                            onChangeItem(item.copy(comment = newValue))
                        }
                    )
                }
            }
        }

    return columns
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DocumentTypeDropdown(
    currentType: DocumentType?,
    onTypeSelected: (DocumentType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedText = currentType?.displayName ?: ""

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        TextField(
            value = selectedText,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .menuAnchor()
                .fillMaxSize()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            DocumentType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(type.displayName) },
                    onClick = {
                        onTypeSelected(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
