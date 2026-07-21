package ru.pavlig43.tablecore.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState

class FilterMatcherTest : FunSpec({
    test("date filter compares a date-time field by calendar date") {
        val selectedDate = LocalDate(2025, 9, 10)

        DateTimeMatcher.matches(
            value = LocalDateTime(2025, 9, 10, 23, 59),
            constraint = FilterConstraint.GT,
            dates = listOf(selectedDate),
        ) shouldBe false

        DateTimeMatcher.matches(
            value = LocalDateTime(2025, 9, 11, 0, 0),
            constraint = FilterConstraint.GT,
            dates = listOf(selectedDate),
        ) shouldBe true

        DateTimeMatcher.matches(
            value = LocalDateTime(2025, 9, 10, 23, 59),
            constraint = FilterConstraint.EQUALS,
            dates = listOf(selectedDate),
        ) shouldBe true
    }

    test("between includes both whole boundary dates") {
        val from = LocalDate(2025, 9, 10)
        val to = LocalDate(2025, 9, 12)

        DateTimeMatcher.matches(
            value = LocalDateTime(2025, 9, 12, 23, 59),
            constraint = FilterConstraint.BETWEEN,
            dates = listOf(from, to),
        ) shouldBe true

        DateTimeMatcher.matches(
            value = LocalDateTime(2025, 9, 13, 0, 0),
            constraint = FilterConstraint.BETWEEN,
            dates = listOf(from, to),
        ) shouldBe false
    }
})

private object DateTimeMatcher : FilterMatcher<LocalDateTime, Unit>() {
    fun matches(
        value: LocalDateTime,
        constraint: FilterConstraint,
        dates: List<LocalDate>,
    ): Boolean = matchesItem(
        item = value,
        filters = mapOf(
            Unit to TableFilterState(
                constraint = constraint,
                values = dates,
            ),
        ),
    )

    override fun matchesRules(
        item: LocalDateTime,
        column: Unit,
        stateAny: TableFilterState<*>,
    ): Boolean = matchesDateTimeField(item, stateAny)
}
