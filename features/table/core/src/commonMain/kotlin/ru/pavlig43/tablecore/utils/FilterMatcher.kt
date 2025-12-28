package ru.pavlig43.tablecore.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import ru.pavlig43.database.data.product.ProductType
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState
import ua.wwind.table.filter.data.TableFilterType
import kotlin.collections.iterator

@Suppress("UNCHECKED_CAST")

abstract class FilterMatcher<I, C> {


    protected abstract fun matchesRules(item:I,column:C,stateAny:  TableFilterState<*>): Boolean

    @Suppress("ComplexCondition")
    fun matchesItem(
        item: I,
        filters: Map<C, TableFilterState<*>>,
    ): Boolean {
        for ((column, stateAny) in filters) {
            // If state has no constraint or values, skip this field (not restrictive)
            if (stateAny.constraint == null ||
                (
                        stateAny.values == null &&
                                stateAny.constraint != FilterConstraint.IS_NULL &&
                                stateAny.constraint != FilterConstraint.IS_NOT_NULL
                        )
            ) {
                continue
            }

            val matches = matchesRules(item,column,stateAny)

            if (!matches) return false
        }
        return true
    }
    protected fun matchesTextField(
        value: String,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<String>
        val query = st.values?.firstOrNull().orEmpty()
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.CONTAINS -> value.contains(query, ignoreCase = true)
            FilterConstraint.STARTS_WITH -> value.startsWith(query, ignoreCase = true)
            FilterConstraint.ENDS_WITH -> value.endsWith(query, ignoreCase = true)
            FilterConstraint.EQUALS -> value.equals(query, ignoreCase = true)
            FilterConstraint.NOT_EQUALS -> !value.equals(query, ignoreCase = true)
            else -> true
        }
    }
@Suppress("CyclomaticComplexMethod")
    protected fun matchesIntField(
        value: Int,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<Int>
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.GT -> value > (st.values?.getOrNull(0) ?: value)
            FilterConstraint.GTE -> value >= (st.values?.getOrNull(0) ?: value)
            FilterConstraint.LT -> value < (st.values?.getOrNull(0) ?: value)
            FilterConstraint.LTE -> value <= (st.values?.getOrNull(0) ?: value)
            FilterConstraint.EQUALS -> value == (st.values?.getOrNull(0) ?: value)
            FilterConstraint.NOT_EQUALS -> value != (st.values?.getOrNull(0) ?: value)
            FilterConstraint.BETWEEN -> {
                val from = st.values?.getOrNull(0) ?: value
                val to = st.values?.getOrNull(1) ?: value
                value in from..to
            }

            else -> true
        }
    }
    fun <I>matchesTypeField(value: I, state: TableFilterState<*>,): Boolean {
        val constraint = state.constraint ?: return true

        @Suppress("UNCHECKED_CAST")
        val selectedValues = (state.values as? List<I>) ?: emptyList()

        return when (constraint) {
            FilterConstraint.IN -> selectedValues.isEmpty() || selectedValues.contains(value)
            FilterConstraint.NOT_IN -> selectedValues.isEmpty() || !selectedValues.contains(value)
            FilterConstraint.EQUALS -> selectedValues.firstOrNull() == value
            FilterConstraint.NOT_EQUALS -> selectedValues.firstOrNull() != value
            else -> true
        }
    }
    protected fun matchesBooleanField(
        value: Boolean,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<Boolean>
        val constraint = st.constraint ?: return true

        return when (constraint) {
            FilterConstraint.EQUALS -> st.values?.firstOrNull()?.let { v -> value == v } ?: true
            FilterConstraint.NOT_EQUALS -> st.values?.firstOrNull()?.let { v -> value != v } ?: true
            else -> true
        }
    }


    private fun <T> matchesComparableField(
        value: T,
        state: TableFilterState<*>,
    ): Boolean where T : Comparable<T> {
        @Suppress("UNCHECKED_CAST")
        val st = state as TableFilterState<T>
        val constraint = st.constraint ?: return true

        val v0 = st.values?.getOrNull(0) ?: value
        val v1 = st.values?.getOrNull(1) ?: value

        return when (constraint) {
            FilterConstraint.GT         -> value >  v0
            FilterConstraint.GTE        -> value >= v0
            FilterConstraint.LT         -> value <  v0
            FilterConstraint.LTE        -> value <= v0
            FilterConstraint.EQUALS     -> value == v0
            FilterConstraint.NOT_EQUALS -> value != v0
            FilterConstraint.BETWEEN    -> value in v0..v1
            else                        -> true
        }
    }
    protected fun matchesDateField(
        value: LocalDate,
        state: TableFilterState<*>,
    ): Boolean = matchesComparableField(value, state)

    protected fun matchesDateTimeField(
        value: LocalDateTime,
        state: TableFilterState<*>,
    ): Boolean = matchesComparableField(value, state)




}



