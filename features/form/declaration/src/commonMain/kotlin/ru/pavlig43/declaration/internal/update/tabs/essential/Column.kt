@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.declaration.internal.update.tabs.essential

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.declaration.internal.DeclarationField
import ru.pavlig43.declaration.internal.model.DeclarationEssentialsUi
import ru.pavlig43.mutable.api.column.*
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns
import ua.wwind.table.filter.data.TableFilterType

internal fun createDeclarationColumns1(
    onOpenVendorDialog: () -> Unit,
    onOpenBornDateDialog: () -> Unit,
    onOpenBestBeforeDialog: () -> Unit,
    onChangeItem: (DeclarationEssentialsUi) -> Unit,
): ImmutableList<ColumnSpec<DeclarationEssentialsUi, DeclarationField, Unit>> {
    val columns =
        editableTableColumns<DeclarationEssentialsUi, DeclarationField, Unit> {

            writeTextColumn(
                headerText = "Название",
                column = DeclarationField.DISPLAY_NAME,
                valueOf = { it.displayName },
                onChangeItem = { item, name -> onChangeItem(item.copy(displayName = name)) },
                filterType = TableFilterType.TextTableFilter()
            )

            textWithSearchIconColumn(
                headerText = "Поставщик",
                column = DeclarationField.VENDOR_NAME,
                valueOf = { it.vendorName ?: "" },
                onOpenDialog = { onOpenVendorDialog() },
                filterType = null
            )

            writeDateColumn(
                headerText = "Создана",
                column = DeclarationField.BORN_DATE,
                valueOf = { it.bornDate },
                onOpenDateDialog = onOpenBornDateDialog,
                filterType = TableFilterType.DateTableFilter()
            )

            writeDateColumn(
                headerText = "Истекает",
                column = DeclarationField.BEST_BEFORE,
                valueOf = { it.bestBefore },
                onOpenDateDialog = onOpenBestBeforeDialog,
                filterType = TableFilterType.DateTableFilter()
            )

            writeCheckBoxColumn(
                headerText = "Отслеживать в оповещениях",
                column = DeclarationField.IS_OBSERVE,
                valueOf = { it.isObserveFromNotification },
                onChangeChecked = { item, checked -> onChangeItem(item.copy(isObserveFromNotification = checked)) },
                filterType = TableFilterType.BooleanTableFilter()
            )

            readDateColumn(
                headerText = "Дата создания",
                column = DeclarationField.CREATED_AT,
                valueOf = { it.createdAt },
                filterType = TableFilterType.DateTableFilter()
            )
        }
    return columns
}
