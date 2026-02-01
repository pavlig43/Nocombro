package ru.pavlig43.sampletable.app.components

import androidx.compose.runtime.Composable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.format.FormatDialog
import ua.wwind.table.format.FormatDialogSettings
import ua.wwind.table.format.data.TableFormatRule
import ua.wwind.table.strings.DefaultStrings

@Suppress("CyclomaticComplexMethod")
@Composable
fun ConditionalFormattingDialog(
    showDialog: Boolean,
    rules: ImmutableList<TableFormatRule<ru.pavlig43.sampletable.column.PersonColumn, Map<ru.pavlig43.sampletable.column.PersonColumn, TableFilterState<*>>>>,
    onRulesChanged: (
        ImmutableList<
            TableFormatRule<
                    ru.pavlig43.sampletable.column.PersonColumn,
                Map<ru.pavlig43.sampletable.column.PersonColumn, TableFilterState<*>>,
            >,
        >,
    ) -> Unit,
    buildFormatFilterData: (
        TableFormatRule<ru.pavlig43.sampletable.column.PersonColumn, Map<ru.pavlig43.sampletable.column.PersonColumn, TableFilterState<*>>>,
        (
            TableFormatRule<
                    ru.pavlig43.sampletable.column.PersonColumn,
                Map<ru.pavlig43.sampletable.column.PersonColumn, TableFilterState<*>>,
            >,
        ) -> Unit,
    ) -> List<
        ua.wwind.table.format.FormatFilterData<ru.pavlig43.sampletable.column.PersonColumn>,
    >,
    onDismissRequest: () -> Unit,
) {
    FormatDialog(
        showDialog = showDialog,
        rules = rules,
        onRulesChanged = onRulesChanged,
        getNewRule = { id ->
            TableFormatRule.new<ru.pavlig43.sampletable.column.PersonColumn, Map<ru.pavlig43.sampletable.column.PersonColumn, TableFilterState<*>>>(
                id,
                emptyMap(),
            )
        },
        getTitle = { field ->
            when (field) {
                ru.pavlig43.sampletable.column.PersonColumn.NAME -> "Name"
                ru.pavlig43.sampletable.column.PersonColumn.AGE -> "Age"
                ru.pavlig43.sampletable.column.PersonColumn.ACTIVE -> "Active"
                ru.pavlig43.sampletable.column.PersonColumn.ID -> "ID"
                ru.pavlig43.sampletable.column.PersonColumn.EMAIL -> "Email"
                ru.pavlig43.sampletable.column.PersonColumn.CITY -> "City"
                ru.pavlig43.sampletable.column.PersonColumn.COUNTRY -> "Country"
                ru.pavlig43.sampletable.column.PersonColumn.DEPARTMENT -> "Department"
                ru.pavlig43.sampletable.column.PersonColumn.POSITION -> "Position"
                ru.pavlig43.sampletable.column.PersonColumn.SALARY -> "Salary"
                ru.pavlig43.sampletable.column.PersonColumn.RATING -> "Rating"
                ru.pavlig43.sampletable.column.PersonColumn.HIRE_DATE -> "Hire Date"
                ru.pavlig43.sampletable.column.PersonColumn.NOTES -> "Notes"
                ru.pavlig43.sampletable.column.PersonColumn.AGE_GROUP -> "Age group"
                ru.pavlig43.sampletable.column.PersonColumn.EXPAND -> "Movements"
                ru.pavlig43.sampletable.column.PersonColumn.SELECTION -> "Selection"
            }
        },
        filters = buildFormatFilterData,
        entries = ru.pavlig43.sampletable.column.PersonColumn.entries.toImmutableList(),
        key = Unit,
        strings = DefaultStrings,
        onDismissRequest = onDismissRequest,
        settings = FormatDialogSettings(),
    )
}
