@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.declaration.internal.update.tabs.essential

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.declaration.internal.DeclarationField
import ru.pavlig43.declaration.internal.model.DeclarationEssentialsUi
import ru.pavlig43.mutable.api.column.textWithSearchIconColumn
import ru.pavlig43.mutable.api.column.writeCheckBoxColumn
import ru.pavlig43.mutable.api.column.writeDateColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

internal fun createDeclarationColumns1(
    onOpenVendorDialog: () -> Unit,
    onOpenBornDateDialog: () -> Unit,
    onOpenBestBeforeDialog: () -> Unit,
    onChangeItem: ((DeclarationEssentialsUi) -> DeclarationEssentialsUi) -> Unit,
): ImmutableList<ColumnSpec<DeclarationEssentialsUi, DeclarationField, Unit>> {
    val columns =
        editableTableColumns<DeclarationEssentialsUi, DeclarationField, Unit> {

            writeTextColumn(
                headerText = "Название",
                column = DeclarationField.DISPLAY_NAME,
                valueOf = { it.displayName },
                isSortable = false,
                onChangeItem = { item, name -> onChangeItem { it.copy(displayName = name) } },
                filterType = TableFilterType.TextTableFilter()
            )

            textWithSearchIconColumn(
                headerText = "Поставщик",
                column = DeclarationField.VENDOR_NAME,
                valueOf = { it.vendorName ?: "" },
                isSortable = false,
                onOpenDialog = { onOpenVendorDialog() },
            )

            writeDateColumn(
                headerText = "Создана",
                column = DeclarationField.BORN_DATE,
                valueOf = { it.bornDate },
                isSortable = false,
                onOpenDateDialog = {onOpenBornDateDialog()},
            )

            writeDateColumn(
                headerText = "Истекает",
                column = DeclarationField.BEST_BEFORE,
                valueOf = { it.bestBefore },
                isSortable = false,
                onOpenDateDialog = {onOpenBestBeforeDialog()},
            )

            writeCheckBoxColumn(
                headerText = "Отслеживать в оповещениях",
                column = DeclarationField.IS_OBSERVE,
                valueOf = { it.isObserveFromNotification },
                isSortable = false,
                onChangeChecked = { item, checked -> onChangeItem { it.copy(isObserveFromNotification = checked) } },
            )

        }
    return columns
}
