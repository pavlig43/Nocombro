package ru.pavlig43.itemlist.core.refac.core.utils

import kotlinx.datetime.LocalDate
import ru.pavlig43.itemlist.core.refac.api.model.IItemUi
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState

interface FilterMatcher<I : IItemUi, C> {
    fun matchesItem(item: I, filters: Map<C, TableFilterState<*>>): Boolean
}
abstract class DefaultFilterMatcher<I : IItemUi, C> {


    protected abstract fun matchesRules(item:I,column:C,stateAny:  TableFilterState<*>): Boolean

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




    protected fun matchesDateField(
        value: LocalDate,
        state: TableFilterState<*>,
    ): Boolean {
        val st = state as TableFilterState<LocalDate>
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

}


