@file:Suppress("MatchingDeclarationName")
package ru.pavlig43.declaration.internal.create.component

import kotlinx.collections.immutable.ImmutableList
import ru.pavlig43.declaration.internal.DeclarationField
import ru.pavlig43.declaration.internal.model.DeclarationEssentialsUi
import ru.pavlig43.mutable.api.column.textWithSearchIconColumn
import ru.pavlig43.mutable.api.column.writeCheckBoxColumn
import ru.pavlig43.mutable.api.column.writeDateColumn
import ru.pavlig43.mutable.api.column.writeTextColumn
import ua.wwind.table.ColumnSpec
import ua.wwind.table.editableTableColumns

internal fun createDeclarationColumns0(
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
            )

            textWithSearchIconColumn(
                headerText = "Поставщик",
                column = DeclarationField.VENDOR_NAME,
                valueOf = {it.vendorName?:""},
                onOpenDialog = {onOpenVendorDialog()},
            )

            writeDateColumn(
                headerText = "Создана",
                column = DeclarationField.BORN_DATE,
                valueOf = { it.bornDate },
                onOpenDateDialog = {onOpenBornDateDialog()},
            )

            writeDateColumn(
                headerText = "Истекает",
                column = DeclarationField.BEST_BEFORE,
                valueOf = { it.bestBefore },
                onOpenDateDialog = {onOpenBestBeforeDialog()},
            )

            writeCheckBoxColumn(
                headerText = "Отслеживать в оповещениях",
                column = DeclarationField.IS_OBSERVE,
                valueOf = { it.isObserveFromNotification },
                onChangeChecked = { item, checked -> onChangeItem(item.copy(isObserveFromNotification = checked)) },
            )


        }
    return columns
}
