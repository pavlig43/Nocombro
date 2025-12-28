package ua.wwind.table.sample.app.components

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
    rules: ImmutableList<TableFormatRule<ua.wwind.table.sample.column.PersonColumn, Map<ua.wwind.table.sample.column.PersonColumn, TableFilterState<*>>>>,
    onRulesChanged: (
        ImmutableList<
            TableFormatRule<
                    ua.wwind.table.sample.column.PersonColumn,
                Map<ua.wwind.table.sample.column.PersonColumn, TableFilterState<*>>,
            >,
        >,
    ) -> Unit,
    buildFormatFilterData: (
        TableFormatRule<ua.wwind.table.sample.column.PersonColumn, Map<ua.wwind.table.sample.column.PersonColumn, TableFilterState<*>>>,
        (
            TableFormatRule<
                    ua.wwind.table.sample.column.PersonColumn,
                Map<ua.wwind.table.sample.column.PersonColumn, TableFilterState<*>>,
            >,
        ) -> Unit,
    ) -> List<
        ua.wwind.table.format.FormatFilterData<ua.wwind.table.sample.column.PersonColumn>,
    >,
    onDismissRequest: () -> Unit,
) {
    FormatDialog(
        showDialog = showDialog,
        rules = rules,
        onRulesChanged = onRulesChanged,
        getNewRule = { id ->
            TableFormatRule.new<ua.wwind.table.sample.column.PersonColumn, Map<ua.wwind.table.sample.column.PersonColumn, TableFilterState<*>>>(
                id,
                emptyMap(),
            )
        },
        getTitle = { field ->
            when (field) {
                ua.wwind.table.sample.column.PersonColumn.NAME -> "Name"
                ua.wwind.table.sample.column.PersonColumn.AGE -> "Age"
                ua.wwind.table.sample.column.PersonColumn.ACTIVE -> "Active"
                ua.wwind.table.sample.column.PersonColumn.ID -> "ID"
                ua.wwind.table.sample.column.PersonColumn.EMAIL -> "Email"
                ua.wwind.table.sample.column.PersonColumn.CITY -> "City"
                ua.wwind.table.sample.column.PersonColumn.COUNTRY -> "Country"
                ua.wwind.table.sample.column.PersonColumn.DEPARTMENT -> "Department"
                ua.wwind.table.sample.column.PersonColumn.POSITION -> "Position"
                ua.wwind.table.sample.column.PersonColumn.SALARY -> "Salary"
                ua.wwind.table.sample.column.PersonColumn.RATING -> "Rating"
                ua.wwind.table.sample.column.PersonColumn.HIRE_DATE -> "Hire Date"
                ua.wwind.table.sample.column.PersonColumn.NOTES -> "Notes"
                ua.wwind.table.sample.column.PersonColumn.AGE_GROUP -> "Age group"
                ua.wwind.table.sample.column.PersonColumn.EXPAND -> "Movements"
                ua.wwind.table.sample.column.PersonColumn.SELECTION -> "Selection"
            }
        },
        filters = buildFormatFilterData,
        entries = ua.wwind.table.sample.column.PersonColumn.entries.toImmutableList(),
        key = Unit,
        strings = DefaultStrings,
        onDismissRequest = onDismissRequest,
        settings = FormatDialogSettings(),
    )
}
