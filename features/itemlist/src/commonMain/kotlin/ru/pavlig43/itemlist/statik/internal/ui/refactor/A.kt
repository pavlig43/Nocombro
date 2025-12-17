package ru.pavlig43.itemlist.statik.internal.ui.refactor

import kotlinx.datetime.LocalDate
import ru.pavlig43.database.data.document.DocumentType
import ru.pavlig43.itemlist.statik.internal.component.DocumentItemUi
import ua.wwind.table.filter.data.FilterConstraint
import ua.wwind.table.filter.data.TableFilterState

data class TypeFilter(val types: Set<DocumentType>)


/**
 * Utility class for filtering Person objects based on filter constraints.
 */
object DocumentFilterMatcher {
    /**
     * Evaluate whether the given person matches the filter map.
     * Supports Text, Number(Int), Boolean, LocalDate filter types.
     */
    fun matchesDocument(
        document: DocumentItemUi,
        filters: Map<DocumentField, TableFilterState<*>>,
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

            val matches =
                when (column) {
                    DocumentField.NAME -> matchesTextField(document.displayName, stateAny)
                    DocumentField.SELECTION -> true
                    DocumentField.ID -> matchesIntField(document.id, stateAny)
                    DocumentField.TYPE -> matchesTypeField(document.type, stateAny)
                    DocumentField.CREATED_AT -> matchesDateField(document.createdAt,stateAny)
                    DocumentField.COMMENT -> matchesTextField(document.comment, stateAny)
                }

            if (!matches) return false
        }
        return true
    }

    private fun matchesTextField(
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

    private fun matchesIntField(
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
    fun matchesTypeField(type: DocumentType, state: Any?): Boolean {
        val filter = state as? TypeFilter ?: return true
        if (filter.types.isEmpty()) return true
        return type in filter.types
    }

    private fun matchesBooleanField(
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




    private fun matchesDateField(
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